package com.hellotractor.chatwoot.domain.model

data class ChatwootMessage(
    val id: Int,
    val content: String?,
    val messageType: ChatwootMessageType,
    val contentType: String? = null,
    val createdAt: Long,
    val conversationId: Int,
    val attachments: List<ChatwootAttachment> = emptyList(),
    val sender: ChatwootMessageSender? = null,
    val echoId: String? = null
)

enum class ChatwootMessageType(val value: Int) {
    INCOMING(0),
    OUTGOING(1),
    ACTIVITY(2);

    companion object {
        fun fromValue(value: Int): ChatwootMessageType =
            entries.find { it.value == value } ?: ACTIVITY
    }
}
