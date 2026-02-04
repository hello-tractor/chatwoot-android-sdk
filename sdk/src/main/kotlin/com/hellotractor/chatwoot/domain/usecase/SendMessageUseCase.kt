package com.hellotractor.chatwoot.domain.usecase

import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import java.util.UUID

class SendMessageUseCase(
    private val repository: ChatwootRepository
) {
    suspend operator fun invoke(
        contactId: String,
        conversationId: Int,
        content: String
    ): Result<ChatwootMessage> {
        val echoId = UUID.randomUUID().toString()

        val optimistic = ChatwootMessage(
            id = echoId.hashCode(),
            content = content,
            messageType = ChatwootMessageType.OUTGOING,
            createdAt = System.currentTimeMillis() / 1000,
            conversationId = conversationId,
            echoId = echoId
        )
        repository.persistMessage(optimistic)

        val result = repository.sendMessage(contactId, conversationId, content, echoId)
        if (result.isSuccess) {
            repository.persistMessage(result.getOrThrow())
        }
        return result
    }
}
