package com.hellotractor.chatwoot.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hellotractor.chatwoot.domain.model.ChatwootAttachment

class ChatwootTypeConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromAttachmentList(attachments: List<ChatwootAttachment>): String {
        return gson.toJson(attachments)
    }

    @TypeConverter
    fun toAttachmentList(json: String): List<ChatwootAttachment> {
        val type = object : TypeToken<List<ChatwootAttachment>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
