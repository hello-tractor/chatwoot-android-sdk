package com.hellotractor.chatwoot

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatwootConfigTest {

    @Test
    fun `apiUrl is constructed correctly`() {
        val config = ChatwootConfig(
            baseUrl = "https://app.chatwoot.com",
            inboxIdentifier = "inbox-123"
        )

        assertThat(config.apiUrl).isEqualTo("https://app.chatwoot.com/public/api/v1/inboxes/inbox-123")
    }

    @Test
    fun `apiUrl trims trailing slash from baseUrl`() {
        val config = ChatwootConfig(
            baseUrl = "https://app.chatwoot.com/",
            inboxIdentifier = "inbox-123"
        )

        assertThat(config.apiUrl).isEqualTo("https://app.chatwoot.com/public/api/v1/inboxes/inbox-123")
    }

    @Test
    fun `webSocketUrl is constructed correctly for https`() {
        val config = ChatwootConfig(
            baseUrl = "https://app.chatwoot.com",
            inboxIdentifier = "inbox-123"
        )

        assertThat(config.webSocketUrl).isEqualTo("wss://app.chatwoot.com/cable")
    }

    @Test
    fun `webSocketUrl handles http protocol`() {
        val config = ChatwootConfig(
            baseUrl = "http://localhost:3000",
            inboxIdentifier = "inbox-123"
        )

        assertThat(config.webSocketUrl).isEqualTo("wss://localhost:3000/cable")
    }

    @Test
    fun `webSocketUrl trims trailing slash`() {
        val config = ChatwootConfig(
            baseUrl = "https://app.chatwoot.com/",
            inboxIdentifier = "inbox-123"
        )

        assertThat(config.webSocketUrl).isEqualTo("wss://app.chatwoot.com/cable")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws when baseUrl is blank`() {
        ChatwootConfig(baseUrl = "", inboxIdentifier = "inbox-123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws when inboxIdentifier is blank`() {
        ChatwootConfig(baseUrl = "https://app.chatwoot.com", inboxIdentifier = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws when baseUrl is whitespace only`() {
        ChatwootConfig(baseUrl = "   ", inboxIdentifier = "inbox-123")
    }
}
