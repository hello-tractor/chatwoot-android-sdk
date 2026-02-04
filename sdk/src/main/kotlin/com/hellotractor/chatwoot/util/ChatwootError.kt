package com.hellotractor.chatwoot.util

sealed class ChatwootError(override val message: String) : Exception(message) {
    data class NetworkError(override val message: String = "Network error") : ChatwootError(message)
    data class ApiError(val code: Int, override val message: String) : ChatwootError(message)
    data class AuthError(override val message: String = "Authentication failed") : ChatwootError(message)
    data class ContactNotFound(override val message: String = "Contact not found") : ChatwootError(message)
    data class ConversationNotFound(override val message: String = "Conversation not found") : ChatwootError(message)
    data class WebSocketError(override val message: String = "WebSocket error") : ChatwootError(message)
    data class SendMessageError(override val message: String = "Failed to send message") : ChatwootError(message)
    data class UnknownError(override val message: String = "Unknown error") : ChatwootError(message)
}
