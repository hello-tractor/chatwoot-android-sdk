package com.hellotractor.chatwoot.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatwootSenderDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("type") val type: String?
)
