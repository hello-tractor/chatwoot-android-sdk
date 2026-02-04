package com.hellotractor.chatwoot.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chatwoot_conversations")
data class ChatwootConversationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "inbox_id") val inboxId: Int?,
    @ColumnInfo(name = "contact_id") val contactId: Int?,
    @ColumnInfo(name = "status") val status: String?
)
