package com.hellotractor.chatwoot.data.remote.request

import com.google.gson.annotations.SerializedName

data class UpdateContactRequest(
    @SerializedName("identifier") val identifier: String? = null,
    @SerializedName("identifier_hash") val identifierHash: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("custom_attributes") val customAttributes: Map<String, Any>? = null
)
