package com.hellotractor.chatwoot.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hellotractor.chatwoot.domain.model.ChatwootAttachment

@Entity(tableName = "chatwoot_messages")
data class ChatwootMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "content") val content: String?,
    @ColumnInfo(name = "message_type") val messageType: Int,
    @ColumnInfo(name = "content_type") val contentType: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "conversation_id") val conversationId: Int,
    @ColumnInfo(name = "attachments") val attachments: List<ChatwootAttachment>,
    @ColumnInfo(name = "sender_name") val senderName: String?,
    @ColumnInfo(name = "sender_avatar_url") val senderAvatarUrl: String?,
    @ColumnInfo(name = "sender_type") val senderType: String?,
    @ColumnInfo(name = "echo_id") val echoId: String?
)
