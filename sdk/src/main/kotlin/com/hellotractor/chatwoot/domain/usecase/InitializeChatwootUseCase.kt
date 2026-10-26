package com.hellotractor.chatwoot.domain.usecase

import com.hellotractor.chatwoot.domain.model.ChatwootContact
import com.hellotractor.chatwoot.domain.model.ChatwootConversation
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootError

/**
 * Restores (or creates) the contact's chat session.
 *
 * The saved session is the only link to the contact's conversation history, so it is only thrown
 * away when Chatwoot says the contact no longer exists (404) or a different user is signed in.
 * Network and server errors keep it and fall back to the cached contact and conversation.
 */
class InitializeChatwootUseCase(
    private val repository: ChatwootRepository
) {
    data class InitResult(
        val contact: ChatwootContact,
        val conversation: ChatwootConversation?,
        /** True when Chatwoot couldn't be reached and the session was restored from the cache. */
        val isOffline: Boolean = false
    )

    suspend operator fun invoke(user: ChatwootUser): Result<InitResult> {
        val savedUserIdentifier = repository.getUserIdentifier()
        if (savedUserIdentifier != null && savedUserIdentifier != user.identifier) {
            // Another user's session is on this device (e.g. a shared phone that wasn't reset on logout)
            repository.clearAll()
        }

        val savedContactIdentifier = repository.getContactIdentifier()
        val savedPubsubToken = repository.getPubsubToken()

        if (savedContactIdentifier != null && savedPubsubToken != null) {
            val contactResult = repository.getContact(savedContactIdentifier)
            if (contactResult.isSuccess) {
                repository.saveUserIdentifier(user.identifier)
                // Sync latest user data to Chatwoot (POST doesn't update existing contacts)
                syncContactData(savedContactIdentifier, user)
                return resolveConversation(savedContactIdentifier, contactResult.getOrThrow())
            }
            if (contactResult.exceptionOrNull() !is ChatwootError.ContactNotFound) {
                return restoreFromCache(savedContactIdentifier, savedPubsubToken)
            }
            // Contact deleted on Chatwoot — fall through to re-create
        }

        return createFreshSession(user)
    }

    private suspend fun createFreshSession(user: ChatwootUser): Result<InitResult> {
        repository.clearAll()

        val contactResult = repository.createContact(user)
        if (contactResult.isFailure) {
            return Result.failure(contactResult.exceptionOrNull() ?: ChatwootError.UnknownError())
        }
        val contact = contactResult.getOrThrow()
        val contactId = contact.contactIdentifier ?: contact.id.toString()
        repository.saveUserIdentifier(user.identifier)

        // POST /contacts doesn't update existing contacts — always PATCH to sync latest fields
        syncContactData(contactId, user)

        return resolveConversation(contactId, contact)
    }

    private suspend fun syncContactData(contactId: String, user: ChatwootUser) {
        val result = repository.updateContact(contactId, user)
        if (result.isFailure) {
            android.util.Log.w("ChatwootSDK", "Failed to sync contact data: ${result.exceptionOrNull()?.message}")
        }
    }

    /**
     * Picks the conversation to show: the newest open one, else the newest resolved one (sending to
     * it reopens it). Always asks Chatwoot first so a conversation support started is picked up.
     */
    private suspend fun resolveConversation(
        contactId: String,
        contact: ChatwootContact
    ): Result<InitResult> {
        val conversationsResult = repository.getConversations(contactId)
        if (conversationsResult.isFailure) {
            val persisted = persistedConversation()
                ?: return Result.failure(conversationsResult.exceptionOrNull() ?: ChatwootError.UnknownError())
            return Result.success(InitResult(contact, persisted, isOffline = true))
        }

        val conversations = conversationsResult.getOrThrow()
        val conversation = conversations.filter { it.status != "resolved" }.maxByOrNull { it.id }
            ?: conversations.maxByOrNull { it.id }
            // No conversation yet — don't create one, wait for the first message
            ?: return Result.success(InitResult(contact, null))

        repository.saveConversationId(conversation.id)
        repository.persistConversation(conversation)
        return Result.success(InitResult(contact, conversation))
    }

    private suspend fun restoreFromCache(contactIdentifier: String, pubsubToken: String): Result<InitResult> {
        val contact = repository.getPersistedContact() ?: ChatwootContact(
            id = repository.getContactId() ?: 0,
            contactIdentifier = contactIdentifier,
            pubsubToken = pubsubToken
        )
        return Result.success(InitResult(contact, persistedConversation(), isOffline = true))
    }

    /** The saved conversation; only its id is needed, so a bare one is used if it isn't cached. */
    private suspend fun persistedConversation(): ChatwootConversation? {
        val conversationId = repository.getConversationId() ?: return null
        return repository.getPersistedConversation()?.takeIf { it.id == conversationId }
            ?: ChatwootConversation(id = conversationId)
    }
}
