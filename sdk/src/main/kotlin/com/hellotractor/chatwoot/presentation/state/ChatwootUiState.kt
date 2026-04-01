package com.hellotractor.chatwoot.presentation.state

import android.net.Uri
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.util.ConnectionState

data class ChatwootUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isUploading: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val messages: List<ChatwootMessage> = emptyList(),
    val isAgentTyping: Boolean = false,
    val isAgentOnline: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val errorMessage: String? = null,
    val pendingAttachmentUri: Uri? = null
)

sealed class ChatwootUiEvent {
    data class SendMessage(val content: String, val attachmentUri: Uri? = null) : ChatwootUiEvent()
    data class AttachmentSelected(val uri: Uri) : ChatwootUiEvent()
    data object AttachmentRemoved : ChatwootUiEvent()
    data object StartTyping : ChatwootUiEvent()
    data object StopTyping : ChatwootUiEvent()
    data object RetryConnection : ChatwootUiEvent()
    data object LoadMessages : ChatwootUiEvent()
    data object LoadMoreMessages : ChatwootUiEvent()
}

sealed class ChatwootUiEffect {
    data class ShowError(val message: String) : ChatwootUiEffect()
    data object MessageSent : ChatwootUiEffect()
    data object ScrollToBottom : ChatwootUiEffect()
    data class ConversationResolved(val conversationId: Int) : ChatwootUiEffect()
    data class OpenImageViewer(val imageUrl: String) : ChatwootUiEffect()
    data class OpenFileExternal(val fileUrl: String, val mimeType: String?) : ChatwootUiEffect()
}
