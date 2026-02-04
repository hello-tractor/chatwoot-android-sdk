package com.hellotractor.chatwoot.domain.model

data class ChatwootContact(
    val id: Int,
    val contactIdentifier: String?,
    val pubsubToken: String?,
    val name: String? = null,
    val email: String? = null
)
