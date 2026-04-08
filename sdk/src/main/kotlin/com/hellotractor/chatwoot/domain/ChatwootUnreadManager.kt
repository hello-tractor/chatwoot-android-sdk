package com.hellotractor.chatwoot.domain

import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.model.ChatwootUnreadState
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootWebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// All public methods must be called from the main thread.
// startListening() collects on the provided scope which must use Dispatchers.Main.
internal class ChatwootUnreadManager(
    private val webSocketManager: ChatwootWebSocketManager,
    private val repository: ChatwootRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val MAX_PREVIEW_MESSAGES = 3
        private const val MAX_TRACKED_MESSAGES = 100
    }

    private val _unreadState = MutableStateFlow(ChatwootUnreadState())
    val unreadState: StateFlow<ChatwootUnreadState> = _unreadState.asStateFlow()

    private val unreadMessages = mutableListOf<ChatwootMessage>()

    var isChatOpen: Boolean = false

    fun startListening() {
        scope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is ChatwootWebSocketEvent.MessageCreated -> {
                        onMessageCreated(event.message)
                    }
                    else -> { /* ignore other events */ }
                }
            }
        }
    }

    private fun onMessageCreated(message: ChatwootMessage) {
        // Only count agent messages (OUTGOING = from agent to contact)
        if (message.messageType != ChatwootMessageType.OUTGOING) return

        // If chat is currently open, don't count — the user sees it in real time
        if (isChatOpen) return

        val lastSeenAt = repository.getLastSeenAt()
        if (message.createdAt <= lastSeenAt) return

        // Avoid duplicates
        if (unreadMessages.any { it.id == message.id }) return

        unreadMessages.add(message)

        // Cap memory usage — keep only the most recent messages, but track full count
        val totalCount = unreadMessages.size
        if (unreadMessages.size > MAX_TRACKED_MESSAGES) {
            unreadMessages.removeAt(0)
        }

        _unreadState.value = ChatwootUnreadState(
            count = totalCount,
            latestMessages = unreadMessages.takeLast(MAX_PREVIEW_MESSAGES)
        )
    }

    fun markSeen() {
        val now = System.currentTimeMillis() / 1000
        repository.saveLastSeenAt(now)
        unreadMessages.clear()
        _unreadState.value = ChatwootUnreadState()
    }
}
