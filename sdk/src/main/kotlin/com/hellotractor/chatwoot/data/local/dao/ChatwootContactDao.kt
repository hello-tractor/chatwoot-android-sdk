package com.hellotractor.chatwoot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hellotractor.chatwoot.data.local.entity.ChatwootContactEntity

@Dao
interface ChatwootContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ChatwootContactEntity)

    @Query("SELECT * FROM chatwoot_contacts WHERE id = :contactId LIMIT 1")
    suspend fun getContact(contactId: Int): ChatwootContactEntity?

    @Query("SELECT * FROM chatwoot_contacts LIMIT 1")
    suspend fun getFirstContact(): ChatwootContactEntity?

    @Query("DELETE FROM chatwoot_contacts")
    suspend fun deleteAll()
}
