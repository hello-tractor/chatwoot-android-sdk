package com.hellotractor.chatwoot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hellotractor.chatwoot.ChatwootSDK

internal class ChatwootViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatwootViewModel::class.java)) {
            val deps = ChatwootSDK.dependencies
            return ChatwootViewModel(
                initializeUseCase = deps.initializeUseCase,
                loadMessagesUseCase = deps.loadMessagesUseCase,
                sendMessageUseCase = deps.sendMessageUseCase,
                sendActionUseCase = deps.sendActionUseCase,
                webSocketManager = deps.webSocketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
