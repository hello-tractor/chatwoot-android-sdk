package com.hellotractor.chatwoot

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.hellotractor.chatwoot.data.local.ChatwootDatabase
import com.hellotractor.chatwoot.data.local.dao.ChatwootContactDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootConversationDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootMessageDao
import com.hellotractor.chatwoot.data.remote.api.ChatwootApiService
import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager
import com.hellotractor.chatwoot.data.repository.ChatwootRepositoryImpl
import com.hellotractor.chatwoot.domain.ChatwootUnreadManager
import com.hellotractor.chatwoot.domain.model.ChatwootUnreadState
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.domain.usecase.InitializeChatwootUseCase
import com.hellotractor.chatwoot.domain.usecase.LoadMessagesUseCase
import com.hellotractor.chatwoot.domain.usecase.SendActionUseCase
import com.hellotractor.chatwoot.domain.usecase.SendMessageUseCase
import com.hellotractor.chatwoot.util.ChatwootConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Main entry point for the Chatwoot SDK. Must be initialized before use.
 *
 * Usage:
 * ```
 * // In Application.onCreate() or before launching chat:
 * ChatwootSDK.init(
 *     context = applicationContext,
 *     config = ChatwootConfig(
 *         baseUrl = "https://your-chatwoot.com",
 *         inboxIdentifier = "your-inbox-id"
 *     )
 * )
 *
 * // Then launch chat:
 * ChatwootLauncher.launch(context, user, theme)
 * ```
 */
object ChatwootSDK {

    @Volatile
    private var _instance: ChatwootDependencies? = null

    private var sdkScope: CoroutineScope? = null
    private var unreadManager: ChatwootUnreadManager? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null

    val isInitialized: Boolean get() = _instance != null

    internal val dependencies: ChatwootDependencies
        get() = _instance ?: throw IllegalStateException(
            "ChatwootSDK is not initialized. Call ChatwootSDK.init(context, config) first."
        )

    val unreadState: StateFlow<ChatwootUnreadState>
        get() = unreadManager?.unreadState
            ?: throw IllegalStateException(
                "ChatwootSDK is not initialized. Call ChatwootSDK.init(context, config) first."
            )

    fun init(context: Context, config: ChatwootConfig) {
        if (_instance != null) return
        synchronized(this) {
            if (_instance != null) return
            val deps = ChatwootDependencies(context.applicationContext, config)
            _instance = deps

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            sdkScope = scope

            val manager = ChatwootUnreadManager(
                webSocketManager = deps.webSocketManager,
                repository = deps.repository,
                scope = scope
            )
            unreadManager = manager
            manager.startListening()

            connectIfSessionExists(deps)
            observeAppLifecycle(deps)
        }
    }

    private fun connectIfSessionExists(deps: ChatwootDependencies) {
        val pubsubToken = deps.repository.getPubsubToken()
        val conversationId = deps.repository.getConversationId()
        if (pubsubToken != null && conversationId != null) {
            deps.webSocketManager.connect(pubsubToken, conversationId)
        }
    }

    private fun observeAppLifecycle(deps: ChatwootDependencies) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // App came to foreground — reconnect WebSocket if we have a session
                connectIfSessionExists(deps)
            }

            override fun onStop(owner: LifecycleOwner) {
                // App went to background — disconnect to save battery
                deps.webSocketManager.disconnect()
            }
        }
        lifecycleObserver = observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    internal fun onChatOpened() {
        unreadManager?.isChatOpen = true
        unreadManager?.markSeen()
    }

    internal fun onChatClosed() {
        unreadManager?.isChatOpen = false
    }

    internal fun onSessionEstablished(pubsubToken: String, conversationId: Int) {
        val deps = _instance ?: return
        if (deps.webSocketManager.connectionState.value == com.hellotractor.chatwoot.util.ConnectionState.DISCONNECTED) {
            deps.webSocketManager.connect(pubsubToken, conversationId)
        }
    }

    fun destroy() {
        lifecycleObserver?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
        lifecycleObserver = null
        _instance?.webSocketManager?.destroy()
        sdkScope?.cancel()
        sdkScope = null
        unreadManager = null
        _instance = null
    }
}

internal class ChatwootDependencies(context: Context, val config: ChatwootConfig) {

    companion object {
        private const val TAG = "ChatwootSDK"
    }

    val gson: Gson = Gson()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .apply {
            // Only enable HTTP logging in debug builds - NEVER log request/response bodies in production
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(config.apiUrl + "/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ChatwootApiService = retrofit.create(ChatwootApiService::class.java)

    val database: ChatwootDatabase = Room.databaseBuilder(
        context,
        ChatwootDatabase::class.java,
        ChatwootConstants.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    val messageDao: ChatwootMessageDao = database.messageDao()
    val contactDao: ChatwootContactDao = database.contactDao()
    val conversationDao: ChatwootConversationDao = database.conversationDao()

    // Use EncryptedSharedPreferences for secure token storage (API 23+)
    val prefs: SharedPreferences = createSecurePrefs(context)

    val webSocketManager: ChatwootWebSocketManager = ChatwootWebSocketManager(
        config = config,
        okHttpClient = okHttpClient,
        gson = gson
    )

    val repository: ChatwootRepository = ChatwootRepositoryImpl(
        apiService = apiService,
        messageDao = messageDao,
        contactDao = contactDao,
        conversationDao = conversationDao,
        prefs = prefs,
        contentResolver = context.contentResolver
    )

    val initializeUseCase: InitializeChatwootUseCase = InitializeChatwootUseCase(repository)
    val loadMessagesUseCase: LoadMessagesUseCase = LoadMessagesUseCase(repository)
    val sendMessageUseCase: SendMessageUseCase = SendMessageUseCase(repository)
    val sendActionUseCase: SendActionUseCase = SendActionUseCase(webSocketManager)

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ChatwootConstants.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails (shouldn't happen on API 23+)
            Log.w(TAG, "Failed to create encrypted prefs, falling back to standard prefs", e)
            context.getSharedPreferences(ChatwootConstants.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}
