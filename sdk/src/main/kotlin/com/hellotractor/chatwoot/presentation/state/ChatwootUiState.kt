package com.hellotractor.chatwoot.presentation.state

import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.util.ConnectionState

data class ChatwootUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val messages: List<ChatwootMessage> = emptyList(),
    val isAgentTyping: Boolean = false,
    val isAgentOnline: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val errorMessage: String? = null
)

sealed class ChatwootUiEvent {
    data class SendMessage(val content: String) : ChatwootUiEvent()
    data object StartTyping : ChatwootUiEvent()
    data object StopTyping : ChatwootUiEvent()
    data object RetryConnection : ChatwootUiEvent()
    data object LoadMessages : ChatwootUiEvent()
}

sealed class ChatwootUiEffect {
    data class ShowError(val message: String) : ChatwootUiEffect()
    data object MessageSent : ChatwootUiEffect()
    data object ScrollToBottom : ChatwootUiEffect()
    data class ConversationResolved(val conversationId: Int) : ChatwootUiEffect()
}
