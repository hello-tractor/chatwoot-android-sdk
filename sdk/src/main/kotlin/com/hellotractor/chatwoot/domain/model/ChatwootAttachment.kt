package com.hellotractor.chatwoot.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatwootAttachment(
    val id: Int? = null,
    val fileType: String? = null,
    val dataUrl: String? = null,
    val thumbUrl: String? = null,
    val fileSize: Int? = null
) : Parcelable

@Parcelize
data class ChatwootMessageSender(
    val id: Int? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val type: String? = null
) : Parcelable
