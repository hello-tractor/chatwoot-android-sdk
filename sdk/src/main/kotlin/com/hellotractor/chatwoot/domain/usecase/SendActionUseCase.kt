package com.hellotractor.chatwoot.domain.usecase

import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager

class SendActionUseCase(
    private val webSocketManager: ChatwootWebSocketManager
) {
    fun sendTyping(conversationId: Int) {
        webSocketManager.sendTyping(conversationId)
    }

    fun sendStopTyping(conversationId: Int) {
        webSocketManager.sendStopTyping(conversationId)
    }
}
