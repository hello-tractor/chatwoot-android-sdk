package com.hellotractor.chatwoot.data.remote.request

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("content") val content: String,
    @SerializedName("echo_id") val echoId: String
)
