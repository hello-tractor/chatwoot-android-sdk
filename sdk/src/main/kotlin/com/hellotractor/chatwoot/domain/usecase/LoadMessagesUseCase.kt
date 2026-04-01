package com.hellotractor.chatwoot.domain.usecase

import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootConstants

class LoadMessagesUseCase(
    private val repository: ChatwootRepository
) {
    data class LoadResult(
        val messages: List<ChatwootMessage>,
        val fromCache: Boolean,
        val hasMore: Boolean = false
    )

    suspend operator fun invoke(contactId: String, conversationId: Int): Result<LoadResult> {
        // Run retention cleanup on initial load
        runRetention(conversationId)

        val cached = repository.getPersistedMessages(conversationId)
        if (cached.isNotEmpty()) {
            val remoteResult = repository.getMessages(contactId, conversationId)
            return if (remoteResult.isSuccess) {
                val messages = remoteResult.getOrThrow()
                val hasMore = messages.size >= ChatwootConstants.MESSAGES_PAGE_SIZE
                Result.success(LoadResult(messages, fromCache = false, hasMore = hasMore))
            } else {
                Result.success(LoadResult(cached, fromCache = true, hasMore = cached.size >= ChatwootConstants.MESSAGES_PAGE_SIZE))
            }
        }

        val remoteResult = repository.getMessages(contactId, conversationId)
        return if (remoteResult.isSuccess) {
            val messages = remoteResult.getOrThrow()
            val hasMore = messages.size >= ChatwootConstants.MESSAGES_PAGE_SIZE
            Result.success(LoadResult(messages, fromCache = false, hasMore = hasMore))
        } else {
            Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to load messages"))
        }
    }

    suspend fun loadMore(conversationId: Int, offset: Int): LoadResult {
        val page = repository.getPersistedMessagesPaged(
            conversationId,
            ChatwootConstants.MESSAGES_PAGE_SIZE,
            offset
        )
        return LoadResult(
            messages = page.reversed(),
            fromCache = true,
            hasMore = page.size >= ChatwootConstants.MESSAGES_PAGE_SIZE
        )
    }

    private var lastRetentionRun: Long = 0L

    private suspend fun runRetention(conversationId: Int) {
        val now = System.currentTimeMillis()
        if (now - lastRetentionRun < 86_400_000L) return // Once per day max
        lastRetentionRun = now

        val cutoff = (now / 1000) - (ChatwootConstants.MESSAGES_RETENTION_DAYS * 86400)
        repository.deleteOldMessages(cutoff)
        repository.trimMessages(conversationId, ChatwootConstants.MESSAGES_MAX_PER_CONVERSATION)
    }
}
