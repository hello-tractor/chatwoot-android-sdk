package com.hellotractor.chatwoot.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chatwoot_contacts")
data class ChatwootContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "contact_identifier") val contactIdentifier: String?,
    @ColumnInfo(name = "pubsub_token") val pubsubToken: String?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "email") val email: String?
)
