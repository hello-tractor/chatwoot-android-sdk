package com.hellotractor.chatwoot.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LoadMessagesUseCaseTest {

    private lateinit var repository: ChatwootRepository
    private lateinit var useCase: LoadMessagesUseCase

    private val cachedMessages = listOf(
        ChatwootMessage(id = 1, content = "Cached 1", messageType = ChatwootMessageType.OUTGOING, createdAt = 1L, conversationId = 100),
        ChatwootMessage(id = 2, content = "Cached 2", messageType = ChatwootMessageType.INCOMING, createdAt = 2L, conversationId = 100)
    )

    private val remoteMessages = listOf(
        ChatwootMessage(id = 1, content = "Remote 1", messageType = ChatwootMessageType.OUTGOING, createdAt = 1L, conversationId = 100),
        ChatwootMessage(id = 2, content = "Remote 2", messageType = ChatwootMessageType.INCOMING, createdAt = 2L, conversationId = 100),
        ChatwootMessage(id = 3, content = "Remote 3", messageType = ChatwootMessageType.INCOMING, createdAt = 3L, conversationId = 100)
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = LoadMessagesUseCase(repository)
    }

    @Test
    fun `invoke returns remote messages when cache exists and API succeeds`() = runTest {
        coEvery { repository.getPersistedMessages(100) } returns cachedMessages
        coEvery { repository.getMessages("contact", 100) } returns Result.success(remoteMessages)

        val result = useCase("contact", 100)

        assertThat(result.isSuccess).isTrue()
        val loadResult = result.getOrNull()!!
        assertThat(loadResult.messages).hasSize(3)
        assertThat(loadResult.fromCache).isFalse()
    }

    @Test
    fun `invoke returns cached messages when cache exists and API fails`() = runTest {
        coEvery { repository.getPersistedMessages(100) } returns cachedMessages
        coEvery { repository.getMessages("contact", 100) } returns Result.failure(Exception("Network error"))

        val result = useCase("contact", 100)

        assertThat(result.isSuccess).isTrue()
        val loadResult = result.getOrNull()!!
        assertThat(loadResult.messages).hasSize(2)
        assertThat(loadResult.fromCache).isTrue()
    }

    @Test
    fun `invoke returns remote messages when no cache and API succeeds`() = runTest {
        coEvery { repository.getPersistedMessages(100) } returns emptyList()
        coEvery { repository.getMessages("contact", 100) } returns Result.success(remoteMessages)

        val result = useCase("contact", 100)

        assertThat(result.isSuccess).isTrue()
        val loadResult = result.getOrNull()!!
        assertThat(loadResult.messages).hasSize(3)
        assertThat(loadResult.fromCache).isFalse()
    }

    @Test
    fun `invoke returns failure when no cache and API fails`() = runTest {
        coEvery { repository.getPersistedMessages(100) } returns emptyList()
        coEvery { repository.getMessages("contact", 100) } returns Result.failure(Exception("Network error"))

        val result = useCase("contact", 100)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error")
    }
}
