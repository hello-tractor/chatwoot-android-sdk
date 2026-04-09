package com.hellotractor.chatwoot.domain.usecase

import com.hellotractor.chatwoot.domain.model.ChatwootContact
import com.hellotractor.chatwoot.domain.model.ChatwootConversation
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootError

class InitializeChatwootUseCase(
    private val repository: ChatwootRepository
) {
    data class InitResult(
        val contact: ChatwootContact,
        val conversation: ChatwootConversation?
    )

    suspend operator fun invoke(user: ChatwootUser): Result<InitResult> {
        val savedContactIdentifier = repository.getContactIdentifier()
        val savedConversationId = repository.getConversationId()
        val savedPubsubToken = repository.getPubsubToken()

        if (savedContactIdentifier != null && savedPubsubToken != null) {
            val contactResult = repository.getContact(savedContactIdentifier)
            if (contactResult.isSuccess) {
                val contact = contactResult.getOrThrow()
                // Sync latest user data to Chatwoot (POST doesn't update existing contacts)
                syncContactData(savedContactIdentifier, user)
                if (savedConversationId != null) {
                    val persistedConversation = repository.getPersistedConversation()
                    if (persistedConversation != null) {
                        return Result.success(InitResult(contact, persistedConversation))
                    }
                }
                return fetchExistingConversation(savedContactIdentifier, contact)
            }
            // Contact gone — fall through to re-create
        }

        return createFreshSession(user)
    }

    private suspend fun createFreshSession(user: ChatwootUser): Result<InitResult> {
        repository.clearSession()

        val contactResult = repository.createContact(user)
        if (contactResult.isFailure) {
            return Result.failure(contactResult.exceptionOrNull() ?: ChatwootError.UnknownError())
        }
        val contact = contactResult.getOrThrow()
        val contactId = contact.contactIdentifier ?: contact.id.toString()

        // POST /contacts doesn't update existing contacts — always PATCH to sync latest fields
        syncContactData(contactId, user)

        return fetchExistingConversation(contactId, contact)
    }

    private suspend fun syncContactData(contactId: String, user: ChatwootUser) {
        val result = repository.updateContact(contactId, user)
        if (result.isFailure) {
            android.util.Log.w("ChatwootSDK", "Failed to sync contact data: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun fetchExistingConversation(
        contactId: String,
        contact: ChatwootContact
    ): Result<InitResult> {
        val conversationsResult = repository.getConversations(contactId)
        if (conversationsResult.isSuccess) {
            val conversations = conversationsResult.getOrThrow()
            val activeConversation = conversations.firstOrNull { it.status != "resolved" }
                ?: conversations.firstOrNull()

            if (activeConversation != null) {
                repository.saveConversationId(activeConversation.id)
                repository.persistConversation(activeConversation)
                return Result.success(InitResult(contact, activeConversation))
            }
        }

        // No existing conversation — don't create one yet, wait for first message
        return Result.success(InitResult(contact, null))
    }
}
