package com.hellotractor.chatwoot.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatwootConversationDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("inbox_id") val inboxId: Int?,
    @SerializedName("messages") val messages: List<ChatwootMessageDto>?,
    @SerializedName("contact") val contact: ChatwootContactDto?,
    @SerializedName("status") val status: String?
)
