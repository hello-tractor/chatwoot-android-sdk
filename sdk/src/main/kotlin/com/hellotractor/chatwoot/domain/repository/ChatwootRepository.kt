package com.hellotractor.chatwoot.domain.repository

import com.hellotractor.chatwoot.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ChatwootRepository {
    suspend fun createContact(user: ChatwootUser): Result<ChatwootContact>
    suspend fun getContact(contactId: String): Result<ChatwootContact>
    suspend fun updateContact(contactId: String, user: ChatwootUser): Result<ChatwootContact>
    suspend fun createConversation(contactId: String): Result<ChatwootConversation>
    suspend fun getConversations(contactId: String): Result<List<ChatwootConversation>>
    suspend fun sendMessage(contactId: String, conversationId: Int, content: String, echoId: String): Result<ChatwootMessage>
    suspend fun getMessages(contactId: String, conversationId: Int): Result<List<ChatwootMessage>>

    fun observeMessages(conversationId: Int): Flow<List<ChatwootMessage>>
    suspend fun getPersistedMessages(conversationId: Int): List<ChatwootMessage>
    suspend fun persistMessages(messages: List<ChatwootMessage>)
    suspend fun persistMessage(message: ChatwootMessage)
    suspend fun persistContact(contact: ChatwootContact)
    suspend fun persistConversation(conversation: ChatwootConversation)
    suspend fun getPersistedContact(): ChatwootContact?
    suspend fun getPersistedConversation(): ChatwootConversation?

    fun saveContactIdentifier(identifier: String)
    fun getContactIdentifier(): String?
    fun saveContactId(contactId: Int)
    fun getContactId(): Int?
    fun savePubsubToken(token: String)
    fun getPubsubToken(): String?
    fun saveConversationId(conversationId: Int)
    fun getConversationId(): Int?
    fun clearSession()
}
