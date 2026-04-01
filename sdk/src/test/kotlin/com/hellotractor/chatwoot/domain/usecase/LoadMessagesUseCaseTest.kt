package com.hellotractor.chatwoot.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootConstants
import io.mockk.coEvery
import io.mockk.coVerify
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
        repository = mockk(relaxed = true)
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

    @Test
    fun `invoke runs retention cleanup`() = runTest {
        coEvery { repository.getPersistedMessages(100) } returns emptyList()
        coEvery { repository.getMessages("contact", 100) } returns Result.success(remoteMessages)

        useCase("contact", 100)

        coVerify { repository.deleteOldMessages(any()) }
        coVerify { repository.trimMessages(100, ChatwootConstants.MESSAGES_MAX_PER_CONVERSATION) }
    }

    @Test
    fun `invoke sets hasMore true when messages fill a page`() = runTest {
        val fullPage = (1..ChatwootConstants.MESSAGES_PAGE_SIZE).map {
            ChatwootMessage(id = it, content = "Msg $it", messageType = ChatwootMessageType.INCOMING, createdAt = it.toLong(), conversationId = 100)
        }
        coEvery { repository.getPersistedMessages(100) } returns emptyList()
        coEvery { repository.getMessages("contact", 100) } returns Result.success(fullPage)

        val result = useCase("contact", 100)

        assertThat(result.getOrNull()!!.hasMore).isTrue()
    }

    @Test
    fun `invoke sets hasMore false when messages are fewer than page size`() = runTest {
        coEvery { repository.getPersistedMessages(100) } returns emptyList()
        coEvery { repository.getMessages("contact", 100) } returns Result.success(remoteMessages)

        val result = useCase("contact", 100)

        assertThat(result.getOrNull()!!.hasMore).isFalse()
    }

    @Test
    fun `loadMore returns paged messages from local DB`() = runTest {
        val pagedMessages = listOf(
            ChatwootMessage(id = 10, content = "Old 1", messageType = ChatwootMessageType.INCOMING, createdAt = 10L, conversationId = 100),
            ChatwootMessage(id = 11, content = "Old 2", messageType = ChatwootMessageType.INCOMING, createdAt = 11L, conversationId = 100)
        )
        coEvery {
            repository.getPersistedMessagesPaged(100, ChatwootConstants.MESSAGES_PAGE_SIZE, 5)
        } returns pagedMessages

        val result = useCase.loadMore(100, 5)

        assertThat(result.messages).hasSize(2)
        assertThat(result.fromCache).isTrue()
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `loadMore sets hasMore true when full page returned`() = runTest {
        val fullPage = (1..ChatwootConstants.MESSAGES_PAGE_SIZE).map {
            ChatwootMessage(id = it, content = "Msg $it", messageType = ChatwootMessageType.INCOMING, createdAt = it.toLong(), conversationId = 100)
        }
        coEvery {
            repository.getPersistedMessagesPaged(100, ChatwootConstants.MESSAGES_PAGE_SIZE, 0)
        } returns fullPage

        val result = useCase.loadMore(100, 0)

        assertThat(result.hasMore).isTrue()
    }
}
