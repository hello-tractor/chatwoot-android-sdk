package com.hellotractor.chatwoot

/**
 * @param baseUrl The base URL of the Chatwoot instance (e.g., "https://app.chatwoot.com").
 *                Must use HTTPS for security.
 * @param inboxIdentifier The inbox API channel identifier from Chatwoot dashboard.
 */
data class ChatwootConfig(
    val baseUrl: String,
    val inboxIdentifier: String
) {
    // Normalize URL to always use HTTPS
    private val secureBaseUrl: String = baseUrl
        .trim()
        .replace(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        .trimEnd('/')

    val apiUrl: String get() = "$secureBaseUrl/public/api/v1/inboxes/$inboxIdentifier"
    val webSocketUrl: String get() = "wss://${secureBaseUrl.removePrefix("https://")}/cable"

    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(inboxIdentifier.isNotBlank()) { "inboxIdentifier must not be blank" }
        require(secureBaseUrl.startsWith("https://")) {
            "baseUrl must use HTTPS for secure communication"
        }
    }
}
