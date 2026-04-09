package com.hellotractor.chatwoot.domain.usecase

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SendMessageUseCaseTest {

    private lateinit var repository: ChatwootRepository
    private lateinit var useCase: SendMessageUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = SendMessageUseCase(repository)
    }

    @Test
    fun `invoke persists optimistic message before sending`() = runTest {
        val capturedMessages = mutableListOf<ChatwootMessage>()
        coEvery { repository.persistMessage(capture(capturedMessages)) } returns Unit
        coEvery { repository.sendMessage(any(), any(), any(), any()) } returns Result.success(
            ChatwootMessage(
                id = 123,
                content = "Hello",
                messageType = ChatwootMessageType.INCOMING,
                createdAt = 1700000000L,
                conversationId = 50,
                echoId = "any-echo"
            )
        )

        useCase("contact-1", 50, "Hello")

        // First persistMessage call is the optimistic message
        coVerify(atLeast = 1) { repository.persistMessage(any()) }
        val optimistic = capturedMessages.first()
        assertThat(optimistic.content).isEqualTo("Hello")
        assertThat(optimistic.messageType).isEqualTo(ChatwootMessageType.INCOMING)
        assertThat(optimistic.conversationId).isEqualTo(50)
        assertThat(optimistic.echoId).isNotNull()
    }

    @Test
    fun `invoke generates unique echoId`() = runTest {
        val echoIdSlot = slot<String>()
        coEvery { repository.sendMessage(any(), any(), any(), capture(echoIdSlot)) } returns Result.success(
            ChatwootMessage(
                id = 1,
                content = "Test",
                messageType = ChatwootMessageType.OUTGOING,
                createdAt = 1L,
                conversationId = 1
            )
        )

        useCase("c1", 1, "Test")
        val firstEchoId = echoIdSlot.captured

        useCase("c1", 1, "Test2")
        val secondEchoId = echoIdSlot.captured

        assertThat(firstEchoId).isNotEqualTo(secondEchoId)
    }

    @Test
    fun `invoke returns success when API succeeds`() = runTest {
        val serverMessage = ChatwootMessage(
            id = 500,
            content = "Sent",
            messageType = ChatwootMessageType.OUTGOING,
            createdAt = 1700000000L,
            conversationId = 100,
            echoId = "server-echo"
        )
        coEvery { repository.sendMessage(any(), any(), any(), any()) } returns Result.success(serverMessage)

        val result = useCase("contact", 100, "Sent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.id).isEqualTo(500)
    }

    @Test
    fun `invoke returns failure when API fails`() = runTest {
        coEvery { repository.sendMessage(any(), any(), any(), any()) } returns Result.failure(
            Exception("Network error")
        )

        val result = useCase("contact", 100, "Message")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error")
    }

    @Test
    fun `invoke persists server response on success`() = runTest {
        val serverMessage = ChatwootMessage(
            id = 999,
            content = "Final",
            messageType = ChatwootMessageType.OUTGOING,
            createdAt = 1700000000L,
            conversationId = 100,
            echoId = "final-echo"
        )
        coEvery { repository.sendMessage(any(), any(), any(), any()) } returns Result.success(serverMessage)

        useCase("contact", 100, "Final")

        // Should be called twice: optimistic + server response
        coVerify(exactly = 2) { repository.persistMessage(any()) }
    }

    @Test
    fun `invoke with attachment calls sendMessageWithAttachment`() = runTest {
        val mockUri = mockk<Uri>()
        val serverMessage = ChatwootMessage(
            id = 200,
            content = "With attachment",
            messageType = ChatwootMessageType.OUTGOING,
            createdAt = 1700000000L,
            conversationId = 100
        )
        coEvery {
            repository.sendMessageWithAttachment(any(), any(), any(), any(), any())
        } returns Result.success(serverMessage)

        val result = useCase("contact", 100, "With attachment", mockUri)

        assertThat(result.isSuccess).isTrue()
        coVerify { repository.sendMessageWithAttachment("contact", 100, "With attachment", any(), mockUri) }
        coVerify(exactly = 0) { repository.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke without attachment calls sendMessage`() = runTest {
        val serverMessage = ChatwootMessage(
            id = 201,
            content = "Text only",
            messageType = ChatwootMessageType.OUTGOING,
            createdAt = 1700000000L,
            conversationId = 100
        )
        coEvery { repository.sendMessage(any(), any(), any(), any()) } returns Result.success(serverMessage)

        val result = useCase("contact", 100, "Text only", null)

        assertThat(result.isSuccess).isTrue()
        coVerify { repository.sendMessage("contact", 100, "Text only", any()) }
        coVerify(exactly = 0) { repository.sendMessageWithAttachment(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `invoke with attachment and blank content shows placeholder in optimistic message`() = runTest {
        val mockUri = mockk<Uri>()
        val messageSlot = mutableListOf<ChatwootMessage>()
        coEvery { repository.persistMessage(capture(messageSlot)) } returns Unit
        coEvery {
            repository.sendMessageWithAttachment(any(), any(), any(), any(), any())
        } returns Result.success(
            ChatwootMessage(
                id = 300,
                content = "",
                messageType = ChatwootMessageType.OUTGOING,
                createdAt = 1700000000L,
                conversationId = 100
            )
        )

        useCase("contact", 100, "", mockUri)

        val optimistic = messageSlot.first()
        assertThat(optimistic.content).isEqualTo("Sending attachment...")
    }

    @Test
    fun `invoke with attachment failure returns error`() = runTest {
        val mockUri = mockk<Uri>()
        coEvery {
            repository.sendMessageWithAttachment(any(), any(), any(), any(), any())
        } returns Result.failure(Exception("Upload failed"))

        val result = useCase("contact", 100, "Pic", mockUri)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Upload failed")
    }
}
