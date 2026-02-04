package com.hellotractor.chatwoot.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatwootMessageDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("content") val content: String?,
    @SerializedName("message_type") val messageType: Int?,
    @SerializedName("content_type") val contentType: String?,
    @SerializedName("created_at") val createdAt: Long?,
    @SerializedName("conversation_id") val conversationId: Int?,
    @SerializedName("attachments") val attachments: List<ChatwootAttachmentDto>?,
    @SerializedName("sender") val sender: ChatwootSenderDto?,
    @SerializedName("echo_id") val echoId: String?
)

data class ChatwootAttachmentDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("file_type") val fileType: String?,
    @SerializedName("data_url") val dataUrl: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    @SerializedName("file_size") val fileSize: Int?
)
