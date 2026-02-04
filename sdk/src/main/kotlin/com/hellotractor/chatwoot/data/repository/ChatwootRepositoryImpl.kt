package com.hellotractor.chatwoot.data.repository

import android.content.SharedPreferences
import com.hellotractor.chatwoot.data.local.dao.ChatwootContactDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootConversationDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootMessageDao
import com.hellotractor.chatwoot.data.mapper.*
import com.hellotractor.chatwoot.data.remote.api.ChatwootApiService
import com.hellotractor.chatwoot.data.remote.request.SendMessageRequest
import com.hellotractor.chatwoot.domain.model.*
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootConstants
import com.hellotractor.chatwoot.util.ChatwootError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatwootRepositoryImpl(
    private val apiService: ChatwootApiService,
    private val messageDao: ChatwootMessageDao,
    private val contactDao: ChatwootContactDao,
    private val conversationDao: ChatwootConversationDao,
    private val prefs: SharedPreferences
) : ChatwootRepository {

    override suspend fun createContact(user: ChatwootUser): Result<ChatwootContact> = apiCall {
        val response = apiService.createContact(user.toCreateRequest())
        if (response.isSuccessful) {
            val dto = response.body() ?: return@apiCall Result.failure(ChatwootError.ContactNotFound())
            val contact = dto.toDomain()
            saveContactId(contact.id)
            contact.contactIdentifier?.let { saveContactIdentifier(it) }
            contact.pubsubToken?.let { savePubsubToken(it) }
            persistContact(contact)
            Result.success(contact)
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override suspend fun getContact(contactId: String): Result<ChatwootContact> = apiCall {
        val response = apiService.getContact(contactId)
        if (response.isSuccessful) {
            val dto = response.body() ?: return@apiCall Result.failure(ChatwootError.ContactNotFound())
            Result.success(dto.toDomain())
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override suspend fun updateContact(contactId: String, user: ChatwootUser): Result<ChatwootContact> = apiCall {
        val response = apiService.updateContact(contactId, user.toUpdateRequest())
        if (response.isSuccessful) {
            val dto = response.body() ?: return@apiCall Result.failure(ChatwootError.ContactNotFound())
            val contact = dto.toDomain()
            persistContact(contact)
            Result.success(contact)
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override suspend fun createConversation(contactId: String): Result<ChatwootConversation> = apiCall {
        val response = apiService.createConversation(contactId)
        if (response.isSuccessful) {
            val dto = response.body() ?: return@apiCall Result.failure(ChatwootError.ConversationNotFound())
            val conversation = dto.toDomain()
            saveConversationId(conversation.id)
            persistConversation(conversation)
            Result.success(conversation)
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override suspend fun getConversations(contactId: String): Result<List<ChatwootConversation>> = apiCall {
        val response = apiService.getConversations(contactId)
        if (response.isSuccessful) {
            val dtos = response.body() ?: emptyList()
            Result.success(dtos.map { it.toDomain() })
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override suspend fun sendMessage(
        contactId: String,
        conversationId: Int,
        content: String,
        echoId: String
    ): Result<ChatwootMessage> = apiCall {
        val request = SendMessageRequest(content = content, echoId = echoId)
        val response = apiService.sendMessage(contactId, conversationId, request)
        if (response.isSuccessful) {
            val dto = response.body() ?: return@apiCall Result.failure(ChatwootError.SendMessageError())
            val message = dto.toDomain()
            persistMessage(message)
            Result.success(message)
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override suspend fun getMessages(contactId: String, conversationId: Int): Result<List<ChatwootMessage>> = apiCall {
        val response = apiService.getMessages(contactId, conversationId)
        if (response.isSuccessful) {
            val dtos = response.body() ?: emptyList()
            val messages = dtos.map { it.toDomain() }
            persistMessages(messages)
            Result.success(messages)
        } else {
            Result.failure(ChatwootError.ApiError(response.code(), response.message()))
        }
    }

    override fun observeMessages(conversationId: Int): Flow<List<ChatwootMessage>> =
        messageDao.observeMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getPersistedMessages(conversationId: Int): List<ChatwootMessage> =
        messageDao.getMessages(conversationId).map { it.toDomain() }

    override suspend fun persistMessages(messages: List<ChatwootMessage>) {
        messageDao.upsertAll(messages.map { it.toEntity() })
    }

    override suspend fun persistMessage(message: ChatwootMessage) {
        messageDao.upsert(message.toEntity())
    }

    override suspend fun persistContact(contact: ChatwootContact) {
        contactDao.upsert(contact.toEntity())
    }

    override suspend fun persistConversation(conversation: ChatwootConversation) {
        conversationDao.upsert(conversation.toEntity())
    }

    override suspend fun getPersistedContact(): ChatwootContact? =
        contactDao.getFirstContact()?.toDomain()

    override suspend fun getPersistedConversation(): ChatwootConversation? =
        conversationDao.getFirstConversation()?.toDomain()

    override fun saveContactIdentifier(identifier: String) {
        prefs.edit().putString(ChatwootConstants.KEY_CONTACT_IDENTIFIER, identifier).apply()
    }

    override fun getContactIdentifier(): String? =
        prefs.getString(ChatwootConstants.KEY_CONTACT_IDENTIFIER, null)

    override fun saveContactId(contactId: Int) {
        prefs.edit().putInt(ChatwootConstants.KEY_CONTACT_ID, contactId).apply()
    }

    override fun getContactId(): Int? {
        val id = prefs.getInt(ChatwootConstants.KEY_CONTACT_ID, -1)
        return if (id == -1) null else id
    }

    override fun savePubsubToken(token: String) {
        prefs.edit().putString(ChatwootConstants.KEY_PUBSUB_TOKEN, token).apply()
    }

    override fun getPubsubToken(): String? =
        prefs.getString(ChatwootConstants.KEY_PUBSUB_TOKEN, null)

    override fun saveConversationId(conversationId: Int) {
        prefs.edit().putInt(ChatwootConstants.KEY_CONVERSATION_ID, conversationId).apply()
    }

    override fun getConversationId(): Int? {
        val id = prefs.getInt(ChatwootConstants.KEY_CONVERSATION_ID, -1)
        return if (id == -1) null else id
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    private inline fun <T> apiCall(block: () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: Exception) {
            Result.failure(ChatwootError.NetworkError(e.message ?: "Network request failed"))
        }
    }
}
