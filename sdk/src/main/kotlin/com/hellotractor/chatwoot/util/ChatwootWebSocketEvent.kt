package com.hellotractor.chatwoot.util

import com.hellotractor.chatwoot.domain.model.ChatwootMessage

sealed class ChatwootWebSocketEvent {
    data object Welcome : ChatwootWebSocketEvent()
    data object Ping : ChatwootWebSocketEvent()
    data object ConfirmedSubscription : ChatwootWebSocketEvent()
    data class MessageCreated(val message: ChatwootMessage) : ChatwootWebSocketEvent()
    data class MessageUpdated(val message: ChatwootMessage) : ChatwootWebSocketEvent()
    data object TypingStarted : ChatwootWebSocketEvent()
    data object TypingStopped : ChatwootWebSocketEvent()
    data class PresenceUpdate(val onlineUsers: Map<String, Any>) : ChatwootWebSocketEvent()
    data class ConversationStatusChanged(val conversationId: Int, val status: String) : ChatwootWebSocketEvent()
    data class Error(val error: ChatwootError) : ChatwootWebSocketEvent()
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
