package com.hellotractor.chatwoot.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.google.gson.Gson
import com.hellotractor.chatwoot.ChatwootConfig
import com.hellotractor.chatwoot.data.local.ChatwootDatabase
import com.hellotractor.chatwoot.data.remote.api.ChatwootApiService
import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager
import com.hellotractor.chatwoot.data.repository.ChatwootRepositoryImpl
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.domain.usecase.InitializeChatwootUseCase
import com.hellotractor.chatwoot.domain.usecase.LoadMessagesUseCase
import com.hellotractor.chatwoot.domain.usecase.SendActionUseCase
import com.hellotractor.chatwoot.domain.usecase.SendMessageUseCase
import com.hellotractor.chatwoot.presentation.ChatwootViewModel
import com.hellotractor.chatwoot.util.ChatwootConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Usage in consuming app:
 * ```
 * startKoin {
 *     modules(
 *         chatwootModule(ChatwootConfig(baseUrl = "...", inboxIdentifier = "..."))
 *     )
 * }
 * ```
 */
fun chatwootModule(config: ChatwootConfig) = module {

    single { config }

    single(named("ChatwootGson")) { Gson() }

    single(named("ChatwootHttpClient")) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single(named("ChatwootRetrofit")) {
        Retrofit.Builder()
            .baseUrl(config.apiUrl + "/")
            .client(get<OkHttpClient>(named("ChatwootHttpClient")))
            .addConverterFactory(GsonConverterFactory.create(get(named("ChatwootGson"))))
            .build()
    }

    single<ChatwootApiService> {
        get<Retrofit>(named("ChatwootRetrofit")).create(ChatwootApiService::class.java)
    }

    single {
        Room.databaseBuilder(
            get<Context>(),
            ChatwootDatabase::class.java,
            ChatwootConstants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    single { get<ChatwootDatabase>().messageDao() }
    single { get<ChatwootDatabase>().contactDao() }
    single { get<ChatwootDatabase>().conversationDao() }

    single<SharedPreferences>(named("ChatwootPrefs")) {
        get<Context>().getSharedPreferences(ChatwootConstants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    single {
        ChatwootWebSocketManager(
            config = get(),
            okHttpClient = get(named("ChatwootHttpClient")),
            gson = get(named("ChatwootGson"))
        )
    }

    single<ChatwootRepository> {
        ChatwootRepositoryImpl(
            apiService = get(),
            messageDao = get(),
            contactDao = get(),
            conversationDao = get(),
            prefs = get(named("ChatwootPrefs"))
        )
    }

    single { InitializeChatwootUseCase(get()) }
    single { LoadMessagesUseCase(get()) }
    single { SendMessageUseCase(get()) }
    single { SendActionUseCase(get()) }

    viewModel { ChatwootViewModel(get(), get(), get(), get(), get()) }
}
