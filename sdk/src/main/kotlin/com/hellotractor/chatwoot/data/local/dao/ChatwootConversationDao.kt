package com.hellotractor.chatwoot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hellotractor.chatwoot.data.local.entity.ChatwootConversationEntity

@Dao
interface ChatwootConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ChatwootConversationEntity)

    @Query("SELECT * FROM chatwoot_conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: Int): ChatwootConversationEntity?

    @Query("SELECT * FROM chatwoot_conversations LIMIT 1")
    suspend fun getFirstConversation(): ChatwootConversationEntity?

    @Query("DELETE FROM chatwoot_conversations")
    suspend fun deleteAll()
}
