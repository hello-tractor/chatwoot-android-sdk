package com.hellotractor.chatwoot.domain.model

data class ChatwootUnreadState(
    val count: Int = 0,
    val latestMessages: List<ChatwootMessage> = emptyList()
) {
    val hasUnread: Boolean get() = count > 0
}
