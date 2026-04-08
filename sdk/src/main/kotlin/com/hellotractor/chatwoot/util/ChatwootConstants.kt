package com.hellotractor.chatwoot.util

internal object ChatwootConstants {
    const val PREFS_NAME = "chatwoot_sdk_prefs"
    const val KEY_CONTACT_IDENTIFIER = "contact_identifier"
    const val KEY_CONTACT_ID = "contact_id"
    const val KEY_PUBSUB_TOKEN = "pubsub_token"
    const val KEY_CONVERSATION_ID = "conversation_id"
    const val KEY_LAST_SEEN_AT = "last_seen_at"

    const val DATABASE_NAME = "chatwoot_sdk_database"

    const val WS_RECONNECT_BASE_DELAY_MS = 3000L
    const val WS_RECONNECT_MAX_DELAY_MS = 30000L
    const val WS_PRESENCE_INTERVAL_MS = 30000L

    const val ACTION_CABLE_COMMAND_SUBSCRIBE = "subscribe"
    const val ACTION_CABLE_COMMAND_MESSAGE = "message"

    const val MESSAGES_PAGE_SIZE = 50
    const val MESSAGES_MAX_PER_CONVERSATION = 500
    const val MESSAGES_RETENTION_DAYS = 90L
}
