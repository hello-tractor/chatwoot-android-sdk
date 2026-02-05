package com.hellotractor.chatwoot.data.mapper

import com.google.common.truth.Truth.assertThat
import com.hellotractor.chatwoot.data.remote.dto.ChatwootAttachmentDto
import com.hellotractor.chatwoot.data.remote.dto.ChatwootContactDto
import com.hellotractor.chatwoot.data.remote.dto.ChatwootConversationDto
import com.hellotractor.chatwoot.data.remote.dto.ChatwootMessageDto
import com.hellotractor.chatwoot.data.remote.dto.ChatwootSenderDto
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import org.junit.Test

class MappersTest {

    @Test
    fun `ContactDto toDomain maps all fields correctly`() {
        val dto = ChatwootContactDto(
            id = 123,
            sourceId = "contact-abc",
            pubsubToken = "token-xyz",
            name = "John Doe",
            email = "john@example.com",
            identifier = "id-123",
            identifierHash = "hash-123"
        )

        val domain = dto.toDomain()

        assertThat(domain.id).isEqualTo(123)
        assertThat(domain.contactIdentifier).isEqualTo("contact-abc")
        assertThat(domain.pubsubToken).isEqualTo("token-xyz")
        assertThat(domain.name).isEqualTo("John Doe")
        assertThat(domain.email).isEqualTo("john@example.com")
    }

    @Test
    fun `ContactDto toDomain handles null fields`() {
        val dto = ChatwootContactDto(
            id = 456,
            sourceId = null,
            pubsubToken = null,
            name = null,
            email = null,
            identifier = null,
            identifierHash = null
        )

        val domain = dto.toDomain()

        assertThat(domain.id).isEqualTo(456)
        assertThat(domain.contactIdentifier).isNull()
        assertThat(domain.pubsubToken).isNull()
        assertThat(domain.name).isNull()
        assertThat(domain.email).isNull()
    }

    @Test
    fun `MessageDto toDomain maps outgoing message correctly`() {
        val dto = ChatwootMessageDto(
            id = 100,
            content = "Hello world",
            messageType = 1,
            contentType = "text",
            createdAt = 1700000000L,
            conversationId = 50,
            attachments = emptyList(),
            sender = null,
            echoId = "echo-123"
        )

        val domain = dto.toDomain()

        assertThat(domain.id).isEqualTo(100)
        assertThat(domain.content).isEqualTo("Hello world")
        assertThat(domain.messageType).isEqualTo(ChatwootMessageType.OUTGOING)
        assertThat(domain.contentType).isEqualTo("text")
        assertThat(domain.createdAt).isEqualTo(1700000000L)
        assertThat(domain.conversationId).isEqualTo(50)
        assertThat(domain.echoId).isEqualTo("echo-123")
    }

    @Test
    fun `MessageDto toDomain maps incoming message correctly`() {
        val dto = ChatwootMessageDto(
            id = 101,
            content = "Hi there",
            messageType = 0,
            contentType = "text",
            createdAt = 1700000001L,
            conversationId = 50,
            attachments = emptyList(),
            sender = ChatwootSenderDto(id = 1, name = "Agent", avatarUrl = "https://example.com/avatar.png", type = "user"),
            echoId = null
        )

        val domain = dto.toDomain()

        assertThat(domain.messageType).isEqualTo(ChatwootMessageType.INCOMING)
        assertThat(domain.sender).isNotNull()
        assertThat(domain.sender?.name).isEqualTo("Agent")
        assertThat(domain.sender?.avatarUrl).isEqualTo("https://example.com/avatar.png")
    }

    @Test
    fun `MessageDto toDomain maps activity message correctly`() {
        val dto = ChatwootMessageDto(
            id = 102,
            content = "Conversation resolved",
            messageType = 2,
            contentType = null,
            createdAt = 1700000002L,
            conversationId = 50,
            attachments = emptyList(),
            sender = null,
            echoId = null
        )

        val domain = dto.toDomain()

        assertThat(domain.messageType).isEqualTo(ChatwootMessageType.ACTIVITY)
    }

    @Test
    fun `MessageDto toDomain maps attachments correctly`() {
        val dto = ChatwootMessageDto(
            id = 103,
            content = null,
            messageType = 1,
            contentType = "file",
            createdAt = 1700000003L,
            conversationId = 50,
            attachments = listOf(
                ChatwootAttachmentDto(
                    id = 1,
                    fileType = "image",
                    dataUrl = "https://example.com/image.png",
                    thumbUrl = "https://example.com/thumb.png",
                    fileSize = 1024
                )
            ),
            sender = null,
            echoId = null
        )

        val domain = dto.toDomain()

        assertThat(domain.attachments).hasSize(1)
        assertThat(domain.attachments[0].fileType).isEqualTo("image")
        assertThat(domain.attachments[0].dataUrl).isEqualTo("https://example.com/image.png")
        assertThat(domain.attachments[0].thumbUrl).isEqualTo("https://example.com/thumb.png")
        assertThat(domain.attachments[0].fileSize).isEqualTo(1024)
    }

    @Test
    fun `ConversationDto toDomain maps correctly`() {
        val dto = ChatwootConversationDto(
            id = 200,
            inboxId = 10,
            status = "open",
            messages = listOf(
                ChatwootMessageDto(
                    id = 1,
                    content = "Test",
                    messageType = 1,
                    contentType = "text",
                    createdAt = 1700000000L,
                    conversationId = 200,
                    attachments = emptyList(),
                    sender = null,
                    echoId = null
                )
            ),
            contact = ChatwootContactDto(
                id = 5,
                sourceId = "c-5",
                pubsubToken = "t-5",
                name = null,
                email = null,
                identifier = null,
                identifierHash = null
            )
        )

        val domain = dto.toDomain()

        assertThat(domain.id).isEqualTo(200)
        assertThat(domain.inboxId).isEqualTo(10)
        assertThat(domain.status).isEqualTo("open")
        assertThat(domain.messages).hasSize(1)
        assertThat(domain.contact).isNotNull()
        assertThat(domain.contact?.id).isEqualTo(5)
    }

    @Test
    fun `ChatwootUser toCreateRequest maps correctly`() {
        val user = ChatwootUser(
            identifier = "user-123",
            identifierHash = "hash-abc",
            name = "Jane Doe",
            email = "jane@example.com",
            avatarUrl = "https://example.com/jane.png",
            customAttributes = mapOf("role" to "admin")
        )

        val request = user.toCreateRequest()

        assertThat(request.identifier).isEqualTo("user-123")
        assertThat(request.identifierHash).isEqualTo("hash-abc")
        assertThat(request.name).isEqualTo("Jane Doe")
        assertThat(request.email).isEqualTo("jane@example.com")
        assertThat(request.avatarUrl).isEqualTo("https://example.com/jane.png")
        assertThat(request.customAttributes).containsEntry("role", "admin")
    }

    @Test
    fun `Domain Message toEntity and back preserves data`() {
        val original = ChatwootMessageDto(
            id = 999,
            content = "Round trip test",
            messageType = 1,
            contentType = "text",
            createdAt = 1700000000L,
            conversationId = 100,
            attachments = listOf(
                ChatwootAttachmentDto(id = 1, fileType = "file", dataUrl = "url", thumbUrl = null, fileSize = 500)
            ),
            sender = ChatwootSenderDto(id = 10, name = "Sender", avatarUrl = null, type = "user"),
            echoId = "echo-999"
        ).toDomain()

        val entity = original.toEntity()
        val restored = entity.toDomain()

        assertThat(restored.id).isEqualTo(original.id)
        assertThat(restored.content).isEqualTo(original.content)
        assertThat(restored.messageType).isEqualTo(original.messageType)
        assertThat(restored.createdAt).isEqualTo(original.createdAt)
        assertThat(restored.conversationId).isEqualTo(original.conversationId)
        assertThat(restored.echoId).isEqualTo(original.echoId)
        assertThat(restored.attachments).hasSize(1)
        assertThat(restored.sender?.name).isEqualTo(original.sender?.name)
    }
}
