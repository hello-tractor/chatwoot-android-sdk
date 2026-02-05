package com.hellotractor.chatwoot.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.hellotractor.chatwoot.BuildConfig
import com.hellotractor.chatwoot.ChatwootConfig
import com.hellotractor.chatwoot.data.remote.dto.ChatwootMessageDto
import com.hellotractor.chatwoot.data.mapper.toDomain
import com.hellotractor.chatwoot.util.ChatwootConstants
import com.hellotractor.chatwoot.util.ChatwootError
import com.hellotractor.chatwoot.util.ChatwootWebSocketEvent
import com.hellotractor.chatwoot.util.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*

/**
 * Features:
 * - Auto-reconnect with exponential backoff (3s -> 6s -> 12s -> max 30s)
 * - Presence heartbeat every 30 seconds
 * - Typed event stream via SharedFlow
 * - Connection state via StateFlow
 */
class ChatwootWebSocketManager(
    private val config: ChatwootConfig,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson = Gson()
) {
    companion object {
        private const val TAG = "ChatwootWS"
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, message)
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(TAG, message, throwable)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<ChatwootWebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChatwootWebSocketEvent> = _events

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var webSocket: WebSocket? = null
    private var pubsubToken: String? = null
    private var conversationId: Int? = null
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private var presenceJob: Job? = null
    private var isManuallyDisconnected = false

    fun connect(pubsubToken: String, conversationId: Int) {
        this.pubsubToken = pubsubToken
        this.conversationId = conversationId
        this.isManuallyDisconnected = false
        this.reconnectAttempts = 0
        doConnect()
    }

    fun disconnect() {
        isManuallyDisconnected = true
        reconnectJob?.cancel()
        presenceJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendAction(action: String, conversationId: Int? = null) {
        val token = pubsubToken ?: return
        val identifierJson = gson.toJson(ActionCableChannelIdentifier(pubsubToken = token))
        val dataJson = gson.toJson(ActionCableActionData(action = action, conversationId = conversationId))
        val envelope = ActionCableEnvelope(
            command = ChatwootConstants.ACTION_CABLE_COMMAND_MESSAGE,
            identifier = identifierJson,
            data = dataJson
        )
        webSocket?.send(gson.toJson(envelope))
    }

    fun sendTyping(conversationId: Int) {
        sendAction("typing_on", conversationId)
    }

    fun sendStopTyping(conversationId: Int) {
        sendAction("typing_off", conversationId)
    }

    private fun doConnect() {
        pubsubToken ?: return
        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url(config.webSocketUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logDebug("WebSocket opened")
                reconnectAttempts = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logDebug("WebSocket closing: $code $reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                logDebug("WebSocket closed: $code $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                presenceJob?.cancel()
                if (!isManuallyDisconnected) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                logError("WebSocket failure: ${t.message}", t)
                _connectionState.value = ConnectionState.DISCONNECTED
                _events.tryEmit(ChatwootWebSocketEvent.Error(
                    ChatwootError.WebSocketError(t.message ?: "Connection failed")
                ))
                presenceJob?.cancel()
                if (!isManuallyDisconnected) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            @Suppress("DEPRECATION")
            val jsonElement = JsonParser().parse(text)
            if (!jsonElement.isJsonObject) return
            val json = jsonElement.asJsonObject

            val type = json.get("type")?.asString
            when (type) {
                "welcome" -> {
                    _events.tryEmit(ChatwootWebSocketEvent.Welcome)
                    subscribe()
                }
                "ping" -> {
                    _events.tryEmit(ChatwootWebSocketEvent.Ping)
                }
                "confirm_subscription" -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    _events.tryEmit(ChatwootWebSocketEvent.ConfirmedSubscription)
                    startPresenceHeartbeat()
                }
                else -> {
                    val message = json.getAsJsonObject("message")
                    if (message != null) {
                        handleDataMessage(message.toString())
                    }
                }
            }
        } catch (e: Exception) {
            logError("Failed to parse WS message: ${e.message}", e)
        }
    }

    private fun handleDataMessage(messageJson: String) {
        try {
            @Suppress("DEPRECATION")
            val json = JsonParser().parse(messageJson).asJsonObject
            val event = json.get("event")?.asString

            when (event) {
                "message.created" -> {
                    val dataJson: String = json.get("data")?.toString() ?: return
                    val messageDto: ChatwootMessageDto = gson.fromJson(dataJson, ChatwootMessageDto::class.java)
                    _events.tryEmit(ChatwootWebSocketEvent.MessageCreated(messageDto.toDomain()))
                }
                "message.updated" -> {
                    val dataJson: String = json.get("data")?.toString() ?: return
                    val messageDto: ChatwootMessageDto = gson.fromJson(dataJson, ChatwootMessageDto::class.java)
                    _events.tryEmit(ChatwootWebSocketEvent.MessageUpdated(messageDto.toDomain()))
                }
                "conversation.typing_on" -> {
                    _events.tryEmit(ChatwootWebSocketEvent.TypingStarted)
                }
                "conversation.typing_off" -> {
                    _events.tryEmit(ChatwootWebSocketEvent.TypingStopped)
                }
                "presence.update" -> {
                    val data = json.get("data")
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val usersMap: Map<String, Any> = if (data != null) {
                        gson.fromJson(data.toString(), mapType) ?: emptyMap()
                    } else emptyMap()
                    _events.tryEmit(ChatwootWebSocketEvent.PresenceUpdate(usersMap))
                }
                "conversation.status_changed" -> {
                    val data = json.getAsJsonObject("data")
                    val convId = data?.get("id")?.asInt ?: return
                    val status = data.get("status")?.asString ?: return
                    _events.tryEmit(ChatwootWebSocketEvent.ConversationStatusChanged(convId, status))
                }
            }
        } catch (e: Exception) {
            logError("Failed to parse data message: ${e.message}", e)
        }
    }

    private fun subscribe() {
        val token = pubsubToken ?: return
        val identifierJson = gson.toJson(ActionCableChannelIdentifier(pubsubToken = token))
        val subscribeCommand = ActionCableEnvelope(
            command = ChatwootConstants.ACTION_CABLE_COMMAND_SUBSCRIBE,
            identifier = identifierJson
        )
        webSocket?.send(gson.toJson(subscribeCommand))
    }

    private fun startPresenceHeartbeat() {
        presenceJob?.cancel()
        presenceJob = scope.launch {
            while (isActive) {
                delay(ChatwootConstants.WS_PRESENCE_INTERVAL_MS)
                val token = pubsubToken ?: break
                val identifierJson = gson.toJson(ActionCableChannelIdentifier(pubsubToken = token))
                val presenceData = gson.toJson(ActionCablePresenceData(conversationId = conversationId))
                val envelope = ActionCableEnvelope(
                    command = ChatwootConstants.ACTION_CABLE_COMMAND_MESSAGE,
                    identifier = identifierJson,
                    data = presenceData
                )
                webSocket?.send(gson.toJson(envelope))
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            logDebug("Max reconnection attempts ($MAX_RECONNECT_ATTEMPTS) reached, giving up")
            _events.tryEmit(ChatwootWebSocketEvent.Error(
                ChatwootError.WebSocketError("Connection failed after $MAX_RECONNECT_ATTEMPTS attempts")
            ))
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = calculateBackoff()
            logDebug("Reconnecting in ${delay}ms (attempt $reconnectAttempts)")
            delay(delay)
            reconnectAttempts++
            doConnect()
        }
    }

    private fun calculateBackoff(): Long {
        val delay = ChatwootConstants.WS_RECONNECT_BASE_DELAY_MS * (1L shl reconnectAttempts.coerceAtMost(4))
        return delay.coerceAtMost(ChatwootConstants.WS_RECONNECT_MAX_DELAY_MS)
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
