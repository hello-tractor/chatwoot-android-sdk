package com.hellotractor.chatwoot.di

import com.hellotractor.chatwoot.ChatwootConfig
import com.hellotractor.chatwoot.ChatwootSDK
import com.hellotractor.chatwoot.presentation.ChatwootViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Optional Koin module for apps that use Koin for dependency injection.
 * Initializes [ChatwootSDK] and exposes its dependencies through Koin.
 *
 * Usage:
 * ```
 * startKoin {
 *     androidContext(this@App)
 *     modules(
 *         chatwootModule(ChatwootConfig(baseUrl = "...", inboxIdentifier = "..."))
 *     )
 * }
 * ```
 *
 * If your app does NOT use Koin, call [ChatwootSDK.init] directly instead.
 */
fun chatwootModule(config: ChatwootConfig) = module {

    single {
        ChatwootSDK.init(androidContext(), config)
        config
    }

    single { ChatwootSDK.dependencies.repository }
    single { ChatwootSDK.dependencies.webSocketManager }
    single { ChatwootSDK.dependencies.initializeUseCase }
    single { ChatwootSDK.dependencies.loadMessagesUseCase }
    single { ChatwootSDK.dependencies.sendMessageUseCase }
    single { ChatwootSDK.dependencies.sendActionUseCase }

    viewModel {
        ChatwootViewModel(get(), get(), get(), get(), get())
    }
}
