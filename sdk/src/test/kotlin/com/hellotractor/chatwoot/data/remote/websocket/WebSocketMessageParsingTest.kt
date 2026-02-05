package com.hellotractor.chatwoot.data.remote.websocket

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hellotractor.chatwoot.data.remote.dto.ChatwootMessageDto
import org.junit.Test

class WebSocketMessageParsingTest {

    private val gson = Gson()

    @Test
    fun `parse welcome message`() {
        val json = """{"type":"welcome"}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val type = parsed.get("type")?.asString

        assertThat(type).isEqualTo("welcome")
    }

    @Test
    fun `parse ping message`() {
        val json = """{"type":"ping","message":1700000000}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val type = parsed.get("type")?.asString

        assertThat(type).isEqualTo("ping")
    }

    @Test
    fun `parse confirm_subscription message`() {
        val json = """{"identifier":"{\"channel\":\"RoomChannel\",\"pubsub_token\":\"abc\"}","type":"confirm_subscription"}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val type = parsed.get("type")?.asString

        assertThat(type).isEqualTo("confirm_subscription")
    }

    @Test
    fun `parse message_created event`() {
        val messageJson = """{
            "event": "message.created",
            "data": {
                "id": 123,
                "content": "Hello from agent",
                "message_type": 0,
                "content_type": "text",
                "created_at": 1700000000,
                "conversation_id": 50,
                "attachments": [],
                "sender": {
                    "id": 1,
                    "name": "Agent Smith",
                    "avatar_url": "https://example.com/avatar.png"
                }
            }
        }"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(messageJson).asJsonObject
        val event = parsed.get("event")?.asString
        val dataJson = parsed.get("data")?.toString()

        assertThat(event).isEqualTo("message.created")
        assertThat(dataJson).isNotNull()

        val messageDto: ChatwootMessageDto = gson.fromJson(dataJson, ChatwootMessageDto::class.java)
        assertThat(messageDto.id).isEqualTo(123)
        assertThat(messageDto.content).isEqualTo("Hello from agent")
        assertThat(messageDto.messageType).isEqualTo(0)
        assertThat(messageDto.sender?.name).isEqualTo("Agent Smith")
    }

    @Test
    fun `parse message_updated event`() {
        val messageJson = """{
            "event": "message.updated",
            "data": {
                "id": 456,
                "content": "Updated content",
                "message_type": 1,
                "created_at": 1700000001,
                "conversation_id": 50,
                "attachments": []
            }
        }"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(messageJson).asJsonObject
        val event = parsed.get("event")?.asString
        val dataJson = parsed.get("data")?.toString()

        assertThat(event).isEqualTo("message.updated")

        val messageDto: ChatwootMessageDto = gson.fromJson(dataJson, ChatwootMessageDto::class.java)
        assertThat(messageDto.id).isEqualTo(456)
        assertThat(messageDto.content).isEqualTo("Updated content")
    }

    @Test
    fun `parse typing_on event`() {
        val json = """{"event": "conversation.typing_on", "data": {"conversation_id": 100}}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val event = parsed.get("event")?.asString

        assertThat(event).isEqualTo("conversation.typing_on")
    }

    @Test
    fun `parse typing_off event`() {
        val json = """{"event": "conversation.typing_off", "data": {"conversation_id": 100}}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val event = parsed.get("event")?.asString

        assertThat(event).isEqualTo("conversation.typing_off")
    }

    @Test
    fun `parse presence_update event`() {
        val json = """{"event": "presence.update", "data": {"user_1": "online", "user_2": "offline"}}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val event = parsed.get("event")?.asString
        val data = parsed.get("data")?.asJsonObject

        assertThat(event).isEqualTo("presence.update")
        assertThat(data?.get("user_1")?.asString).isEqualTo("online")
    }

    @Test
    fun `parse conversation_status_changed event`() {
        val json = """{"event": "conversation.status_changed", "data": {"id": 200, "status": "resolved"}}"""

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val event = parsed.get("event")?.asString
        val data = parsed.get("data")?.asJsonObject

        assertThat(event).isEqualTo("conversation.status_changed")
        assertThat(data?.get("id")?.asInt).isEqualTo(200)
        assertThat(data?.get("status")?.asString).isEqualTo("resolved")
    }

    @Test
    fun `ActionCable subscribe command serializes correctly`() {
        val identifier = ActionCableChannelIdentifier(pubsubToken = "test-token")
        val identifierJson = gson.toJson(identifier)
        val envelope = ActionCableEnvelope(
            command = "subscribe",
            identifier = identifierJson
        )
        val json = gson.toJson(envelope)

        assertThat(json).contains("\"command\":\"subscribe\"")
        assertThat(json).contains("\"identifier\":")

        // The identifier should be a JSON string containing the channel info
        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val innerIdentifier = parsed.get("identifier")?.asString
        assertThat(innerIdentifier).contains("RoomChannel")
        assertThat(innerIdentifier).contains("test-token")
    }

    @Test
    fun `ActionCable message command serializes correctly`() {
        val identifier = ActionCableChannelIdentifier(pubsubToken = "test-token")
        val identifierJson = gson.toJson(identifier)
        val actionData = ActionCableActionData(action = "typing_on", conversationId = 123)
        val dataJson = gson.toJson(actionData)

        val envelope = ActionCableEnvelope(
            command = "message",
            identifier = identifierJson,
            data = dataJson
        )
        val json = gson.toJson(envelope)

        assertThat(json).contains("\"command\":\"message\"")
        assertThat(json).contains("\"data\":")

        @Suppress("DEPRECATION")
        val parsed = JsonParser().parse(json).asJsonObject
        val innerData = parsed.get("data")?.asString
        assertThat(innerData).contains("typing_on")
        assertThat(innerData).contains("123")
    }
}
