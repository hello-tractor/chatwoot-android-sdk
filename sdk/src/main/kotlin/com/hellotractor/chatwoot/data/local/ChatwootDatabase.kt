package com.hellotractor.chatwoot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hellotractor.chatwoot.data.local.dao.ChatwootContactDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootConversationDao
import com.hellotractor.chatwoot.data.local.dao.ChatwootMessageDao
import com.hellotractor.chatwoot.data.local.entity.ChatwootContactEntity
import com.hellotractor.chatwoot.data.local.entity.ChatwootConversationEntity
import com.hellotractor.chatwoot.data.local.entity.ChatwootMessageEntity

@Database(
    version = 1,
    entities = [
        ChatwootMessageEntity::class,
        ChatwootContactEntity::class,
        ChatwootConversationEntity::class
    ],
    exportSchema = true
)
@TypeConverters(ChatwootTypeConverters::class)
abstract class ChatwootDatabase : RoomDatabase() {
    abstract fun messageDao(): ChatwootMessageDao
    abstract fun contactDao(): ChatwootContactDao
    abstract fun conversationDao(): ChatwootConversationDao
}
