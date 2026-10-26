package com.hellotractor.chatwoot.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.hellotractor.chatwoot.domain.model.ChatwootContact
import com.hellotractor.chatwoot.domain.model.ChatwootConversation
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class InitializeChatwootUseCaseTest {

    private lateinit var repository: ChatwootRepository

    private val user = ChatwootUser(identifier = "agent-1", name = "Agent One")
    private val contact = ChatwootContact(id = 7, contactIdentifier = "source-1", pubsubToken = "token-1")

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.getUserIdentifier() } returns null
        every { repository.getContactIdentifier() } returns null
        every { repository.getPubsubToken() } returns null
        every { repository.getConversationId() } returns null
        coEvery { repository.getPersistedContact() } returns null
        coEvery { repository.getPersistedConversation() } returns null
        coEvery { repository.updateContact(any(), any()) } returns Result.success(contact)
        coEvery { repository.getConversations(any()) } returns Result.success(emptyList())
        coEvery { repository.createContact(any()) } returns Result.success(contact)
    }

    private fun givenSavedSession(conversationId: Int? = null) {
        every { repository.getContactIdentifier() } returns "source-1"
        every { repository.getPubsubToken() } returns "token-1"
        every { repository.getConversationId() } returns conversationId
    }

    private fun conversation(id: Int, status: String = "open") = ChatwootConversation(id = id, status = status)

    @Test
    fun `invoke keeps saved session when contact lookup fails with a network error`() = runTest {
        givenSavedSession(conversationId = 42)
        coEvery { repository.getContact("source-1") } returns Result.failure(ChatwootError.NetworkError())
        coEvery { repository.getPersistedConversation() } returns conversation(42)

        val result = useCase().invoke(user)

        val initResult = result.getOrThrow()
        assertThat(initResult.isOffline).isTrue()
        assertThat(initResult.conversation?.id).isEqualTo(42)
        assertThat(initResult.contact.contactIdentifier).isEqualTo("source-1")
        coVerify(exactly = 0) { repository.clearAll() }
        coVerify(exactly = 0) { repository.createContact(any()) }
    }

    @Test
    fun `invoke keeps saved session when contact lookup fails with a server error`() = runTest {
        givenSavedSession(conversationId = 42)
        coEvery { repository.getContact("source-1") } returns Result.failure(ChatwootError.ApiError(503, "Unavailable"))

        val result = useCase().invoke(user)

        assertThat(result.getOrThrow().conversation?.id).isEqualTo(42)
        coVerify(exactly = 0) { repository.clearAll() }
        coVerify(exactly = 0) { repository.createContact(any()) }
    }

    @Test
    fun `invoke creates a new session when the saved contact no longer exists`() = runTest {
        givenSavedSession(conversationId = 42)
        coEvery { repository.getContact("source-1") } returns Result.failure(ChatwootError.ContactNotFound())

        val result = useCase().invoke(user)

        assertThat(result.isSuccess).isTrue()
        coVerify { repository.clearAll() }
        coVerify { repository.createContact(user) }
        verify { repository.saveUserIdentifier("agent-1") }
    }

    @Test
    fun `invoke clears another user's session before restoring`() = runTest {
        every { repository.getUserIdentifier() } returns "agent-2"

        useCase().invoke(user)

        coVerify { repository.clearAll() }
        coVerify { repository.createContact(user) }
    }

    @Test
    fun `invoke keeps the session of the same user`() = runTest {
        every { repository.getUserIdentifier() } returns "agent-1"
        givenSavedSession()
        coEvery { repository.getContact("source-1") } returns Result.success(contact)

        useCase().invoke(user)

        coVerify(exactly = 0) { repository.clearAll() }
        coVerify(exactly = 0) { repository.createContact(any()) }
        coVerify { repository.updateContact("source-1", user) }
    }

    @Test
    fun `invoke picks the newest open conversation over resolved ones`() = runTest {
        givenSavedSession(conversationId = 1)
        coEvery { repository.getContact("source-1") } returns Result.success(contact)
        coEvery { repository.getConversations("source-1") } returns Result.success(
            listOf(conversation(1), conversation(5), conversation(9, status = "resolved"))
        )

        val result = useCase().invoke(user)

        assertThat(result.getOrThrow().conversation?.id).isEqualTo(5)
        verify { repository.saveConversationId(5) }
    }

    @Test
    fun `invoke picks the newest conversation when all are resolved`() = runTest {
        givenSavedSession()
        coEvery { repository.getContact("source-1") } returns Result.success(contact)
        coEvery { repository.getConversations("source-1") } returns Result.success(
            listOf(conversation(3, status = "resolved"), conversation(8, status = "resolved"))
        )

        val result = useCase().invoke(user)

        assertThat(result.getOrThrow().conversation?.id).isEqualTo(8)
    }

    @Test
    fun `invoke falls back to the saved conversation when listing conversations fails`() = runTest {
        givenSavedSession(conversationId = 42)
        coEvery { repository.getContact("source-1") } returns Result.success(contact)
        coEvery { repository.getConversations("source-1") } returns Result.failure(ChatwootError.NetworkError())

        val result = useCase().invoke(user)

        val initResult = result.getOrThrow()
        assertThat(initResult.conversation?.id).isEqualTo(42)
        assertThat(initResult.isOffline).isTrue()
    }

    @Test
    fun `invoke fails without clearing when listing conversations fails and none is saved`() = runTest {
        givenSavedSession()
        coEvery { repository.getContact("source-1") } returns Result.success(contact)
        coEvery { repository.getConversations("source-1") } returns Result.failure(ChatwootError.NetworkError())

        val result = useCase().invoke(user)

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { repository.clearAll() }
    }

    @Test
    fun `invoke returns no conversation for a new contact without one`() = runTest {
        val result = useCase().invoke(user)

        val initResult = result.getOrThrow()
        assertThat(initResult.contact).isEqualTo(contact)
        assertThat(initResult.conversation).isNull()
        coVerify(exactly = 0) { repository.createConversation(any()) }
    }

    @Test
    fun `invoke fails when creating the contact fails`() = runTest {
        coEvery { repository.createContact(user) } returns Result.failure(ChatwootError.NetworkError())

        val result = useCase().invoke(user)

        assertThat(result.exceptionOrNull()).isInstanceOf(ChatwootError.NetworkError::class.java)
        verify(exactly = 0) { repository.saveUserIdentifier(any()) }
    }

    private fun useCase() = InitializeChatwootUseCase(repository)
}
