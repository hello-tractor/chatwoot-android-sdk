package com.hellotractor.chatwoot.data.remote.websocket

import com.google.gson.annotations.SerializedName

/**
 * ActionCable wire format models used for WebSocket communication.
 * Chatwoot uses Rails ActionCable protocol.
 */
data class ActionCableEnvelope(
    @SerializedName("type") val type: String? = null,
    @SerializedName("command") val command: String? = null,
    @SerializedName("identifier") val identifier: String? = null,
    @SerializedName("data") val data: String? = null,
    @SerializedName("message") val message: ActionCablePayload? = null
)

data class ActionCablePayload(
    @SerializedName("event") val event: String? = null,
    @SerializedName("data") val data: Any? = null
)

data class ActionCableChannelIdentifier(
    @SerializedName("channel") val channel: String = "RoomChannel",
    @SerializedName("pubsub_token") val pubsubToken: String
)

data class ActionCableActionData(
    @SerializedName("action") val action: String,
    @SerializedName("conversation_id") val conversationId: Int? = null
)

data class ActionCablePresenceData(
    @SerializedName("action") val action: String = "update_presence",
    @SerializedName("conversation_id") val conversationId: Int? = null
)
