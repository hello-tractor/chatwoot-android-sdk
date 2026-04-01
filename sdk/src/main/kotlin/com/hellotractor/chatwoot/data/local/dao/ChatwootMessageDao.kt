package com.hellotractor.chatwoot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hellotractor.chatwoot.data.local.entity.ChatwootMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatwootMessageDao {

    @Query("SELECT * FROM chatwoot_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    fun observeMessages(conversationId: Int): Flow<List<ChatwootMessageEntity>>

    @Query("SELECT * FROM chatwoot_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    suspend fun getMessages(conversationId: Int): List<ChatwootMessageEntity>

    @Query("SELECT * FROM chatwoot_messages WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPaged(conversationId: Int, limit: Int, offset: Int): List<ChatwootMessageEntity>

    @Query("SELECT COUNT(*) FROM chatwoot_messages WHERE conversation_id = :conversationId")
    suspend fun getMessageCount(conversationId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatwootMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatwootMessageEntity)

    @Query("DELETE FROM chatwoot_messages WHERE conversation_id = :conversationId")
    suspend fun deleteAll(conversationId: Int)

    @Query("DELETE FROM chatwoot_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM chatwoot_messages WHERE created_at < :beforeEpochSeconds")
    suspend fun deleteOlderThan(beforeEpochSeconds: Long)

    @Query("DELETE FROM chatwoot_messages WHERE conversation_id = :conversationId AND id NOT IN (SELECT id FROM chatwoot_messages WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT :keepCount)")
    suspend fun trimToCount(conversationId: Int, keepCount: Int)
}
