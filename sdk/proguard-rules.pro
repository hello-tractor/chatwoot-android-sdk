# Chatwoot SDK ProGuard Rules

# Keep Gson serialized/deserialized classes
-keepclassmembers class com.hellotractor.chatwoot.data.remote.dto.** { *; }
-keepclassmembers class com.hellotractor.chatwoot.data.remote.request.** { *; }
-keepclassmembers class com.hellotractor.chatwoot.data.remote.websocket.ActionCableMessage.** { *; }

# Keep Room entities
-keepclassmembers class com.hellotractor.chatwoot.data.local.entity.** { *; }

# Keep domain models (used in public API)
-keepclassmembers class com.hellotractor.chatwoot.domain.model.** { *; }
-keepclassmembers class com.hellotractor.chatwoot.ChatwootConfig { *; }
