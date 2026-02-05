package com.hellotractor.chatwoot.sample

import android.app.Application
import com.hellotractor.chatwoot.ChatwootConfig
import com.hellotractor.chatwoot.ChatwootSDK

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SDK with your Chatwoot instance
        ChatwootSDK.init(
            context = this,
            config = ChatwootConfig(
                baseUrl = BuildConfig.CHATWOOT_BASE_URL,
                inboxIdentifier = BuildConfig.CHATWOOT_INBOX_ID
            )
        )
    }
}
