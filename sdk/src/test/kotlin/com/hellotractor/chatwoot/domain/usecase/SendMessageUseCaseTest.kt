package com.hellotractor.chatwoot.domain.usecase

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
        val messageSlot = slot<ChatwootMessage>()
        coEvery { repository.persistMessage(capture(messageSlot)) } returns Unit
        coEvery { repository.sendMessage(any(), any(), any(), any()) } returns Result.success(
            ChatwootMessage(
                id = 123,
                content = "Hello",
                messageType = ChatwootMessageType.OUTGOING,
                createdAt = 1700000000L,
                conversationId = 50,
                echoId = "any-echo"
            )
        )

        useCase("contact-1", 50, "Hello")

        // First persistMessage call is the optimistic message
        coVerify(atLeast = 1) { repository.persistMessage(any()) }
        val optimistic = messageSlot.captured
        assertThat(optimistic.content).isEqualTo("Hello")
        assertThat(optimistic.messageType).isEqualTo(ChatwootMessageType.OUTGOING)
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
}
