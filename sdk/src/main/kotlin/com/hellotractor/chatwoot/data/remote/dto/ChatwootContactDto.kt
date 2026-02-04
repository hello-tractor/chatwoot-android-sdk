package com.hellotractor.chatwoot.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatwootContactDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("source_id") val sourceId: String?,
    @SerializedName("pubsub_token") val pubsubToken: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("identifier") val identifier: String?,
    @SerializedName("identifier_hash") val identifierHash: String?
)
