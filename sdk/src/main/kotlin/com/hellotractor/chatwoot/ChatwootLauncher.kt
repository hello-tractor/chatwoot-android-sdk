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
 * // 1. Initialize once (e.g., in Application.onCreate()):
 * ChatwootSDK.init(
 *     context = applicationContext,
 *     config = ChatwootConfig(baseUrl = "https://...", inboxIdentifier = "...")
 * )
 *
 * // 2. Launch chat from anywhere:
 * ChatwootLauncher.launch(
 *     context = this,
 *     user = ChatwootUser(
 *         identifier = "user-123",
 *         name = "John Doe",
 *         email = "john@example.com"
 *     ),
 *     theme = ChatwootTheme.helloTractor()
 * )
 * ```
 */
object ChatwootLauncher {

    fun launch(
        context: Context,
        user: ChatwootUser,
        theme: ChatwootTheme = ChatwootTheme.default()
    ) {
        check(ChatwootSDK.isInitialized) {
            "ChatwootSDK is not initialized. Call ChatwootSDK.init(context, config) before launching chat."
        }

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
