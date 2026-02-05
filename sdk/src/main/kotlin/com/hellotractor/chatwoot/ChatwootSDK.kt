package com.hellotractor.chatwoot

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.google.gson.Gson
import com.hellotractor.chatwoot.data.local.ChatwootDatabase
import com.hellotractor.chatwoot.data.local.dao.ChatwootContactDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootConversationDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootMessageDao
import com.hellotractor.chatwoot.data.remote.api.ChatwootApiService
import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager
import com.hellotractor.chatwoot.data.repository.ChatwootRepositoryImpl
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.domain.usecase.InitializeChatwootUseCase
import com.hellotractor.chatwoot.domain.usecase.LoadMessagesUseCase
import com.hellotractor.chatwoot.domain.usecase.SendActionUseCase
import com.hellotractor.chatwoot.domain.usecase.SendMessageUseCase
import com.hellotractor.chatwoot.util.ChatwootConstants
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

    val isInitialized: Boolean get() = _instance != null

    internal val dependencies: ChatwootDependencies
        get() = _instance ?: throw IllegalStateException(
            "ChatwootSDK is not initialized. Call ChatwootSDK.init(context, config) first."
        )

    fun init(context: Context, config: ChatwootConfig) {
        if (_instance != null) return
        synchronized(this) {
            if (_instance != null) return
            _instance = ChatwootDependencies(context.applicationContext, config)
        }
    }

    fun destroy() {
        _instance?.webSocketManager?.destroy()
        _instance = null
    }
}

internal class ChatwootDependencies(context: Context, val config: ChatwootConfig) {

    val gson: Gson = Gson()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
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

    val prefs: SharedPreferences = context.getSharedPreferences(
        ChatwootConstants.PREFS_NAME, Context.MODE_PRIVATE
    )

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
        prefs = prefs
    )

    val initializeUseCase: InitializeChatwootUseCase = InitializeChatwootUseCase(repository)
    val loadMessagesUseCase: LoadMessagesUseCase = LoadMessagesUseCase(repository)
    val sendMessageUseCase: SendMessageUseCase = SendMessageUseCase(repository)
    val sendActionUseCase: SendActionUseCase = SendActionUseCase(webSocketManager)
}
