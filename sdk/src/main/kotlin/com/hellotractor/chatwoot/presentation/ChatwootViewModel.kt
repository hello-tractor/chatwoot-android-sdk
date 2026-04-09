package com.hellotractor.chatwoot.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellotractor.chatwoot.ChatwootSDK
import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.domain.usecase.InitializeChatwootUseCase
import com.hellotractor.chatwoot.domain.usecase.LoadMessagesUseCase
import com.hellotractor.chatwoot.domain.usecase.SendActionUseCase
import com.hellotractor.chatwoot.domain.usecase.SendMessageUseCase
import com.hellotractor.chatwoot.presentation.state.ChatwootUiEffect
import com.hellotractor.chatwoot.presentation.state.ChatwootUiEvent
import com.hellotractor.chatwoot.presentation.state.ChatwootUiState
import com.hellotractor.chatwoot.util.ChatwootWebSocketEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatwootViewModel(
    private val initializeUseCase: InitializeChatwootUseCase,
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendActionUseCase: SendActionUseCase,
    private val webSocketManager: ChatwootWebSocketManager,
    private val repository: ChatwootRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatwootUiState())
    val state: StateFlow<ChatwootUiState> = _state.asStateFlow()

    private val _effects = Channel<ChatwootUiEffect>(Channel.BUFFERED)
    val effects: Flow<ChatwootUiEffect> = _effects.receiveAsFlow()

    private var contactId: String? = null
    private var conversationId: Int? = null
    private var pubsubToken: String? = null

    fun initialize(user: ChatwootUser) {
        if (_state.value.isInitialized) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = initializeUseCase(user)
            result.fold(
                onSuccess = { initResult ->
                    contactId = initResult.contact.contactIdentifier ?: initResult.contact.id.toString()
                    pubsubToken = initResult.contact.pubsubToken

                    val conversation = initResult.conversation
                    if (conversation != null) {
                        conversationId = conversation.id
                        _state.update { it.copy(isInitialized = true, isLoading = false) }

                        if (pubsubToken != null) {
                            ChatwootSDK.onSessionEstablished(pubsubToken!!, conversation.id)
                            collectWebSocketEvents()
                            collectConnectionState()
                        }

                        loadMessages()
                        observeMessages(conversation.id)
                    } else {
                        // No conversation yet — show empty chat, create on first send
                        _state.update { it.copy(isInitialized = true, isLoading = false) }
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                    _effects.send(ChatwootUiEffect.ShowError(error.message ?: "Initialization failed"))
                }
            )
        }
    }

    fun onEvent(event: ChatwootUiEvent) {
        when (event) {
            is ChatwootUiEvent.SendMessage -> sendMessage(event.content, event.attachmentUri)
            is ChatwootUiEvent.AttachmentSelected -> _state.update { it.copy(pendingAttachmentUri = event.uri) }
            is ChatwootUiEvent.AttachmentRemoved -> _state.update { it.copy(pendingAttachmentUri = null) }
            is ChatwootUiEvent.StartTyping -> conversationId?.let { sendActionUseCase.sendTyping(it) }
            is ChatwootUiEvent.StopTyping -> conversationId?.let { sendActionUseCase.sendStopTyping(it) }
            is ChatwootUiEvent.RetryConnection -> retryConnection()
            is ChatwootUiEvent.LoadMessages -> loadMessages()
            is ChatwootUiEvent.LoadMoreMessages -> loadMoreMessages()
        }
    }

    private fun sendMessage(content: String, attachmentUri: Uri? = null) {
        val cId = contactId ?: return
        val uri = attachmentUri ?: _state.value.pendingAttachmentUri

        viewModelScope.launch {
            if (uri != null) {
                _state.update { it.copy(isUploading = true) }
            }

            // Create conversation on first message if needed
            if (conversationId == null) {
                val convResult = repository.createConversation(cId)
                convResult.fold(
                    onSuccess = { conversation ->
                        conversationId = conversation.id
                        repository.saveConversationId(conversation.id)
                        repository.persistConversation(conversation)

                        // Now connect WebSocket for this conversation
                        if (pubsubToken != null) {
                            ChatwootSDK.onSessionEstablished(pubsubToken!!, conversation.id)
                            collectWebSocketEvents()
                            collectConnectionState()
                        }
                    },
                    onFailure = { error ->
                        _state.update { it.copy(isUploading = false, pendingAttachmentUri = null) }
                        _effects.send(ChatwootUiEffect.ShowError(error.message ?: "Failed to start conversation"))
                        return@launch
                    }
                )
            }

            val convId = conversationId ?: return@launch
            val result = sendMessageUseCase(cId, convId, content, uri)
            _state.update { it.copy(isUploading = false, pendingAttachmentUri = null) }
            result.fold(
                onSuccess = { message ->
                    // Add the sent message to the UI immediately
                    _state.update { state ->
                        val updated = state.messages.toMutableList()
                        val existingIndex = updated.indexOfFirst { it.echoId != null && it.echoId == message.echoId }
                        if (existingIndex >= 0) {
                            updated[existingIndex] = message
                        } else if (updated.none { it.id == message.id }) {
                            updated.add(message)
                        }
                        state.copy(messages = updated)
                    }
                    _effects.send(ChatwootUiEffect.MessageSent)
                    _effects.send(ChatwootUiEffect.ScrollToBottom)
                },
                onFailure = { error ->
                    _effects.send(ChatwootUiEffect.ShowError(error.message ?: "Failed to send"))
                }
            )
        }
    }

    private fun loadMessages() {
        val cId = contactId ?: return
        val convId = conversationId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = loadMessagesUseCase(cId, convId)
            result.fold(
                onSuccess = { loadResult ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            messages = loadResult.messages,
                            hasMoreMessages = loadResult.hasMore
                        )
                    }
                    _effects.send(ChatwootUiEffect.ScrollToBottom)
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(ChatwootUiEffect.ShowError(error.message ?: "Failed to load messages"))
                }
            )
        }
    }

    private fun loadMoreMessages() {
        val convId = conversationId ?: return
        if (_state.value.isLoadingMore || !_state.value.hasMoreMessages) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val currentCount = _state.value.messages.size
            val loadResult = loadMessagesUseCase.loadMore(convId, currentCount)
            _state.update {
                it.copy(
                    isLoadingMore = false,
                    messages = loadResult.messages + it.messages,
                    hasMoreMessages = loadResult.hasMore
                )
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun observeMessages(conversationId: Int) {
    }

    private fun collectWebSocketEvents() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is ChatwootWebSocketEvent.MessageCreated -> {
                        _state.update { state ->
                            val updated = state.messages.toMutableList()
                            val existingIndex = updated.indexOfFirst {
                                it.echoId != null && it.echoId == event.message.echoId
                            }
                            if (existingIndex >= 0) {
                                updated[existingIndex] = event.message
                            } else if (updated.none { it.id == event.message.id }) {
                                updated.add(event.message)
                            }
                            state.copy(messages = updated, isAgentTyping = false)
                        }
                        _effects.send(ChatwootUiEffect.ScrollToBottom)
                    }
                    is ChatwootWebSocketEvent.MessageUpdated -> {
                        _state.update { state ->
                            val updated = state.messages.map {
                                if (it.id == event.message.id) event.message else it
                            }
                            state.copy(messages = updated)
                        }
                    }
                    is ChatwootWebSocketEvent.TypingStarted -> {
                        _state.update { it.copy(isAgentTyping = true) }
                    }
                    is ChatwootWebSocketEvent.TypingStopped -> {
                        _state.update { it.copy(isAgentTyping = false) }
                    }
                    is ChatwootWebSocketEvent.PresenceUpdate -> {
                        _state.update { it.copy(isAgentOnline = event.onlineUsers.isNotEmpty()) }
                    }
                    is ChatwootWebSocketEvent.ConversationStatusChanged -> {
                        if (event.status == "resolved") {
                            _effects.send(ChatwootUiEffect.ConversationResolved(event.conversationId))
                        }
                    }
                    is ChatwootWebSocketEvent.Error -> {
                        _effects.send(ChatwootUiEffect.ShowError(event.error.message))
                    }
                    else -> { /* Welcome, Ping, ConfirmedSubscription — no UI action */ }
                }
            }
        }
    }

    private fun collectConnectionState() {
        viewModelScope.launch {
            webSocketManager.connectionState.collect { connectionState ->
                _state.update { it.copy(connectionState = connectionState) }
            }
        }
    }

    private fun retryConnection() {
        _state.update { it.copy(errorMessage = null) }
        loadMessages()
    }

    override fun onCleared() {
        super.onCleared()
        // WebSocket lifecycle is now managed by ChatwootSDK — don't disconnect here
    }
}
