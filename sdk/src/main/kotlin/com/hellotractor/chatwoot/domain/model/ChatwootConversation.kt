package com.hellotractor.chatwoot.domain.model

data class ChatwootConversation(
    val id: Int,
    val inboxId: Int? = null,
    val messages: List<ChatwootMessage> = emptyList(),
    val contact: ChatwootContact? = null,
    val status: String? = null
)
