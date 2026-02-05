# Chatwoot Android SDK

Native Kotlin SDK for integrating [Chatwoot](https://www.chatwoot.com/) live chat into Android apps. Built for Hello Tractor apps but designed to be reusable across any Android project.

## Features

- Full native Kotlin implementation (not a WebView wrapper)
- Real-time messaging via WebSocket (ActionCable protocol)
- Offline support with Room persistence
- Customizable theming
- DI-agnostic (works with Koin, Hilt, or no DI framework)

## Installation

### JitPack

[![](https://jitpack.io/v/hello-tractor/chatwoot-android-sdk.svg)](https://jitpack.io/#hello-tractor/chatwoot-android-sdk)

Add JitPack repository to your project's `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.hello-tractor:chatwoot-android-sdk:1.0.0'
}
```

> **Note:** For private repos, each developer needs to add their JitPack auth token.
> See [JitPack Private Repos](https://jitpack.io/docs/PRIVATE/) for setup.

### Local Module (Alternative)

Clone this repo alongside your project and include it as a local module:

```groovy
// settings.gradle
include ':chatwoot-sdk'
project(':chatwoot-sdk').projectDir = new File('../chatwoot-android-sdk/sdk')

// app/build.gradle
dependencies {
    implementation project(':chatwoot-sdk')
}
```

## Setup

### 1. Get Chatwoot Credentials

1. Log into your Chatwoot dashboard
2. Go to **Settings > Inboxes**
3. Create or select an **API** inbox
4. Copy the **Inbox Identifier** (a UUID string)
5. Note your Chatwoot instance URL (e.g., `https://app.chatwoot.com` or your self-hosted URL)

### 2. Initialize the SDK

Initialize once in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ChatwootSDK.init(
            context = this,
            config = ChatwootConfig(
                baseUrl = "https://app.chatwoot.com",  // Your Chatwoot instance
                inboxIdentifier = "your-inbox-uuid"    // From Chatwoot dashboard
            )
        )
    }
}
```

### 3. Launch the Chat

From anywhere in your app:

```kotlin
ChatwootLauncher.launch(
    context = this,
    user = ChatwootUser(
        identifier = "user-123",           // Unique user ID (required)
        name = "John Doe",                 // Display name (optional)
        email = "john@example.com",        // Email (optional)
        avatarUrl = "https://...",         // Avatar URL (optional)
        identifierHash = "hmac-hash",      // HMAC hash for identity validation (optional)
        customAttributes = mapOf(          // Custom data (optional)
            "plan" to "premium",
            "company" to "Acme Inc"
        )
    )
)
```

## Theming

### Default Theme

```kotlin
ChatwootLauncher.launch(context, user)  // Uses default theme
```

### Hello Tractor Theme

```kotlin
ChatwootLauncher.launch(
    context = this,
    user = user,
    theme = ChatwootTheme.helloTractor()
)
```

### Custom Theme

```kotlin
val customTheme = ChatwootTheme(
    // Colors
    primaryColor = 0xFF6200EE.toInt(),
    primaryDarkColor = 0xFF3700B3.toInt(),
    accentColor = 0xFF03DAC5.toInt(),
    backgroundColor = 0xFFFFFFFF.toInt(),

    // Message bubbles
    sentMessageBubbleColor = 0xFF6200EE.toInt(),
    sentMessageTextColor = 0xFFFFFFFF.toInt(),
    receivedMessageBubbleColor = 0xFFE0E0E0.toInt(),
    receivedMessageTextColor = 0xFF000000.toInt(),

    // Toolbar
    toolbarTitle = "Support",
    toolbarSubtitle = "We typically reply in a few minutes",
    toolbarTextColor = 0xFFFFFFFF.toInt(),

    // Input
    inputHint = "Type a message...",
    inputTextColor = 0xFF000000.toInt(),
    inputBackgroundColor = 0xFFF5F5F5.toInt(),

    // Options
    showTimestamps = true,
    showAgentAvatar = true,
    dateFormat = "hh:mm a"
)

ChatwootLauncher.launch(context, user, customTheme)
```

## Integration with DI Frameworks

### No DI Framework

```kotlin
// Just call init directly
ChatwootSDK.init(context, config)
```

### Koin

```kotlin
startKoin {
    androidContext(this@App)
    modules(
        chatwootModule(ChatwootConfig(
            baseUrl = "https://app.chatwoot.com",
            inboxIdentifier = "your-inbox-uuid"
        ))
    )
}
```

### Hilt

Create a module that initializes the SDK:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ChatwootModule {

    @Provides
    @Singleton
    fun provideChatwootConfig(): ChatwootConfig = ChatwootConfig(
        baseUrl = BuildConfig.CHATWOOT_BASE_URL,
        inboxIdentifier = BuildConfig.CHATWOOT_INBOX_ID
    )

    @Provides
    @Singleton
    fun initializeChatwoot(
        @ApplicationContext context: Context,
        config: ChatwootConfig
    ): ChatwootSDK {
        ChatwootSDK.init(context, config)
        return ChatwootSDK
    }
}
```

## Identity Validation (Optional)

For enhanced security, you can enable [identity validation](https://www.chatwoot.com/docs/product/channels/live-chat/sdk/identity-validation) by generating an HMAC hash server-side:

```kotlin
// Server-side (Node.js example):
const crypto = require('crypto');
const identifierHash = crypto
    .createHmac('sha256', CHATWOOT_IDENTITY_TOKEN)
    .update(userIdentifier)
    .digest('hex');

// Client-side:
ChatwootLauncher.launch(
    context = this,
    user = ChatwootUser(
        identifier = "user-123",
        identifierHash = identifierHashFromServer  // Pass the HMAC hash
    )
)
```

## Sample App

The `sample/` module demonstrates SDK usage. To run it:

1. Open the project in Android Studio
2. Edit `sample/build.gradle` with your Chatwoot credentials:
   ```groovy
   buildConfigField "String", "CHATWOOT_BASE_URL", '"https://your-instance.chatwoot.com"'
   buildConfigField "String", "CHATWOOT_INBOX_ID", '"your-inbox-identifier"'
   ```
3. Run the `sample` app configuration
4. Enter any user identifier and tap "Launch Chat"

## Running Tests

```bash
./gradlew :sdk:testDebugUnitTest
```

## Architecture

```
sdk/
├── ChatwootSDK.kt              # Main entry point, service locator
├── ChatwootConfig.kt           # Configuration (baseUrl, inboxIdentifier)
├── ChatwootLauncher.kt         # Public API to launch chat
├── ChatwootTheme.kt            # Theming configuration
│
├── data/
│   ├── local/                  # Room database (separate from app DB)
│   ├── remote/
│   │   ├── api/                # Retrofit API service
│   │   ├── dto/                # Data transfer objects
│   │   ├── request/            # API request bodies
│   │   └── websocket/          # WebSocket manager (ActionCable)
│   ├── mapper/                 # DTO <-> Domain <-> Entity mappers
│   └── repository/             # Repository implementation
│
├── domain/
│   ├── model/                  # Domain models
│   ├── repository/             # Repository interface
│   └── usecase/                # Business logic
│
├── presentation/
│   ├── ChatwootChatActivity.kt # Chat screen
│   ├── ChatwootViewModel.kt    # ViewModel with StateFlow
│   └── adapter/                # RecyclerView adapter
│
├── di/                         # Optional Koin module
└── util/                       # Constants, errors, events
```

## API Reference

### ChatwootSDK

| Method | Description |
|--------|-------------|
| `init(context, config)` | Initialize the SDK (call once) |
| `isInitialized` | Check if SDK is initialized |
| `destroy()` | Clean up resources |

### ChatwootLauncher

| Method | Description |
|--------|-------------|
| `launch(context, user, theme?)` | Open the chat screen |

### ChatwootUser

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identifier` | String | Yes | Unique user ID |
| `name` | String? | No | Display name |
| `email` | String? | No | Email address |
| `avatarUrl` | String? | No | Profile picture URL |
| `identifierHash` | String? | No | HMAC hash for identity validation |
| `customAttributes` | Map<String, Any>? | No | Custom metadata |

### ChatwootConfig

| Field | Type | Description |
|-------|------|-------------|
| `baseUrl` | String | Chatwoot instance URL |
| `inboxIdentifier` | String | Inbox API identifier (UUID) |

## License

Private - Hello Tractor internal use.
