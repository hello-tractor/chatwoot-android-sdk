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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatwootMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatwootMessageEntity)

    @Query("DELETE FROM chatwoot_messages WHERE conversation_id = :conversationId")
    suspend fun deleteAll(conversationId: Int)

    @Query("DELETE FROM chatwoot_messages")
    suspend fun deleteAllMessages()
}
