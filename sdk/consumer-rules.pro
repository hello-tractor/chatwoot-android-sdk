# Consumer ProGuard rules for Chatwoot SDK
# These rules are applied to the consuming app

-keepclassmembers class com.hellotractor.chatwoot.data.remote.dto.** { *; }
-keepclassmembers class com.hellotractor.chatwoot.data.remote.request.** { *; }
-keepclassmembers class com.hellotractor.chatwoot.domain.model.** { *; }
-keepclassmembers class com.hellotractor.chatwoot.ChatwootConfig { *; }
