package com.hellotractor.chatwoot

/**
 * @param baseUrl The base URL of the Chatwoot instance (e.g., "https://app.chatwoot.com").
 * @param inboxIdentifier The inbox API channel identifier from Chatwoot dashboard.
 */
data class ChatwootConfig(
    val baseUrl: String,
    val inboxIdentifier: String
) {
    val apiUrl: String get() = "${baseUrl.trimEnd('/')}/public/api/v1/inboxes/$inboxIdentifier"
    val webSocketUrl: String get() = "wss://${baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')}/cable"

    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(inboxIdentifier.isNotBlank()) { "inboxIdentifier must not be blank" }
    }
}
