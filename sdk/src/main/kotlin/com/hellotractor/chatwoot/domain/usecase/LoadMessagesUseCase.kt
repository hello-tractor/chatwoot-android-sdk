package com.hellotractor.chatwoot.domain.usecase

import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository

class LoadMessagesUseCase(
    private val repository: ChatwootRepository
) {
    data class LoadResult(
        val messages: List<ChatwootMessage>,
        val fromCache: Boolean
    )

    suspend operator fun invoke(contactId: String, conversationId: Int): Result<LoadResult> {
        val cached = repository.getPersistedMessages(conversationId)
        if (cached.isNotEmpty()) {
            val remoteResult = repository.getMessages(contactId, conversationId)
            return if (remoteResult.isSuccess) {
                Result.success(LoadResult(remoteResult.getOrThrow(), fromCache = false))
            } else {
                Result.success(LoadResult(cached, fromCache = true))
            }
        }

        val remoteResult = repository.getMessages(contactId, conversationId)
        return if (remoteResult.isSuccess) {
            Result.success(LoadResult(remoteResult.getOrThrow(), fromCache = false))
        } else {
            Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to load messages"))
        }
    }
}
