package com.hellotractor.chatwoot.data.remote.api

import com.hellotractor.chatwoot.data.remote.dto.ChatwootContactDto
import com.hellotractor.chatwoot.data.remote.dto.ChatwootConversationDto
import com.hellotractor.chatwoot.data.remote.dto.ChatwootMessageDto
import com.hellotractor.chatwoot.data.remote.request.CreateContactRequest
import com.hellotractor.chatwoot.data.remote.request.SendMessageRequest
import com.hellotractor.chatwoot.data.remote.request.UpdateContactRequest
import retrofit2.Response
import retrofit2.http.*

interface ChatwootApiService {

    @POST("contacts")
    suspend fun createContact(
        @Body request: CreateContactRequest
    ): Response<ChatwootContactDto>

    @GET("contacts/{contactId}")
    suspend fun getContact(
        @Path("contactId") contactId: String
    ): Response<ChatwootContactDto>

    @PATCH("contacts/{contactId}")
    suspend fun updateContact(
        @Path("contactId") contactId: String,
        @Body request: UpdateContactRequest
    ): Response<ChatwootContactDto>

    @POST("contacts/{contactId}/conversations")
    suspend fun createConversation(
        @Path("contactId") contactId: String
    ): Response<ChatwootConversationDto>

    @GET("contacts/{contactId}/conversations")
    suspend fun getConversations(
        @Path("contactId") contactId: String
    ): Response<List<ChatwootConversationDto>>

    @POST("contacts/{contactId}/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("contactId") contactId: String,
        @Path("conversationId") conversationId: Int,
        @Body request: SendMessageRequest
    ): Response<ChatwootMessageDto>

    @GET("contacts/{contactId}/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("contactId") contactId: String,
        @Path("conversationId") conversationId: Int
    ): Response<List<ChatwootMessageDto>>

    @PATCH("contacts/{contactId}/conversations/{conversationId}/messages/{messageId}")
    suspend fun updateMessage(
        @Path("contactId") contactId: String,
        @Path("conversationId") conversationId: Int,
        @Path("messageId") messageId: Int,
        @Body request: SendMessageRequest
    ): Response<ChatwootMessageDto>
}
