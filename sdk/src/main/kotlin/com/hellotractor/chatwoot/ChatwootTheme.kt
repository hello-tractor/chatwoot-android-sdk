package com.hellotractor.chatwoot

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class ChatwootTheme(
    @ColorInt val primaryColor: Int = 0xFF1B5E20.toInt(),
    @ColorInt val primaryDarkColor: Int = 0xFF003300.toInt(),
    @ColorInt val accentColor: Int = 0xFF4CAF50.toInt(),
    @ColorInt val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    @ColorInt val sentMessageBubbleColor: Int = 0xFF1B5E20.toInt(),
    @ColorInt val sentMessageTextColor: Int = 0xFFFFFFFF.toInt(),
    @ColorInt val receivedMessageBubbleColor: Int = 0xFFF5F5F5.toInt(),
    @ColorInt val receivedMessageTextColor: Int = 0xFF212121.toInt(),
    @ColorInt val toolbarTextColor: Int = 0xFFFFFFFF.toInt(),
    @ColorInt val inputBackgroundColor: Int = 0xFFF5F5F5.toInt(),
    @ColorInt val inputTextColor: Int = 0xFF212121.toInt(),
    @ColorInt val hintTextColor: Int = 0xFF9E9E9E.toInt(),
    @ColorInt val timestampTextColor: Int = 0xFF9E9E9E.toInt(),
    @DrawableRes val logoResId: Int? = null,
    val toolbarTitle: String = "Support Chat",
    val toolbarSubtitle: String? = null,
    val inputHint: String = "Type a message...",
    val fontFamily: String? = null,
    val showToolbarLogo: Boolean = false,
    val showAgentAvatar: Boolean = true,
    val showTimestamps: Boolean = true,
    val dateFormat: String = "hh:mm a"
) {
    companion object {
        fun helloTractor(): ChatwootTheme = ChatwootTheme(
            primaryColor = 0xFF1B5E20.toInt(),
            primaryDarkColor = 0xFF003300.toInt(),
            accentColor = 0xFF4CAF50.toInt(),
            toolbarTitle = "Hello Tractor Support",
            showToolbarLogo = true
        )

        fun default(): ChatwootTheme = ChatwootTheme()
    }
}
