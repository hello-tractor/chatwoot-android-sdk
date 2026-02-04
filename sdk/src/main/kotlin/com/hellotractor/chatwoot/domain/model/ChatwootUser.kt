package com.hellotractor.chatwoot.domain.model

data class ChatwootUser(
    val identifier: String,
    val identifierHash: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val customAttributes: Map<String, Any>? = null
)
