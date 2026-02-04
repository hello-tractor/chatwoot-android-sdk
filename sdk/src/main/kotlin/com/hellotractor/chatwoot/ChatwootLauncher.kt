package com.hellotractor.chatwoot

import android.content.Context
import android.content.Intent
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.presentation.ChatwootChatActivity

/**
 * Public API for launching the Chatwoot chat screen.
 *
 * Usage:
 * ```
 * ChatwootLauncher.launch(
 *     context = this,
 *     user = ChatwootUser(
 *         identifier = "user-123",
 *         name = "John Doe",
 *         email = "john@example.com"
 *     ),
 *     theme = ChatwootTheme.helloTractor()  // or custom theme
 * )
 * ```
 */
object ChatwootLauncher {

    fun launch(
        context: Context,
        user: ChatwootUser,
        theme: ChatwootTheme = ChatwootTheme.default()
    ) {
        ChatwootChatActivity.themeOverride = theme
        val intent = Intent(context, ChatwootChatActivity::class.java).apply {
            putExtra(ChatwootChatActivity.EXTRA_USER_IDENTIFIER, user.identifier)
            putExtra(ChatwootChatActivity.EXTRA_USER_NAME, user.name)
            putExtra(ChatwootChatActivity.EXTRA_USER_EMAIL, user.email)
            putExtra(ChatwootChatActivity.EXTRA_USER_AVATAR_URL, user.avatarUrl)
            putExtra(ChatwootChatActivity.EXTRA_USER_IDENTIFIER_HASH, user.identifierHash)
        }
        context.startActivity(intent)
    }
}
