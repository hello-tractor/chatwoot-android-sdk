# Changelog

## 1.2.6

Fixes for chat history going missing and chat failing to load.

- **Keep the session on network errors.** Opening the chat while offline or during a server error used
  to delete the saved session and create a new contact, which hid the user's previous conversation.
  The session is now only replaced when Chatwoot reports the contact no longer exists (404), and the
  chat opens from the cache while Chatwoot can't be reached.
- **Clear the session for a different user.** The SDK remembers which `identifier` owns the session
  and starts fresh when another user opens the chat on the same device. New
  `ChatwootSDK.clearSession(context)` to call on logout.
- **Pick the right conversation.** The conversation list is fetched on every open (the newest open
  conversation wins, else the newest resolved one) instead of reusing whichever was cached first.
- **Send the full user.** `phone` and `customAttributes` passed to `ChatwootLauncher.launch` were
  dropped before reaching Chatwoot; they're now sent.
- **Retry works.** The Retry button and the "Disconnected" banner now retry; if setup itself failed,
  Retry runs setup again.
- **Catch up after reconnecting.** Messages are re-fetched once the WebSocket resubscribes, so ones
  sent while it was down appear. Live messages are saved to the cache even when the chat is closed.
- **Don't lose failed messages.** A message that fails to send is put back in the input (and its
  attachment stays selected) instead of being cleared; no placeholder is left in the cache.
- **No duplicate sockets.** Reconnecting closes the previous socket, and callbacks from a replaced
  socket are ignored.
- **Cheaper init.** `ChatwootSDK.init` no longer opens encrypted preferences on the calling thread,
  so it's safe to call from `Application.onCreate` or any thread.
- **Readable errors.** Network failures show "No internet connection…" instead of the raw exception
  text, and a new chat no longer shows "Disconnected" before its first message (there's no live
  connection to have until a conversation exists).
- **Edge-to-edge chat screen.** The chat screen now handles the system bars itself: the toolbar
  extends under the status bar (light icons) and the input stays above the gesture bar and the
  keyboard. Host apps should drop their own inset workarounds for `ChatwootChatActivity` (and not
  pad it from a global activity callback). `ChatwootTheme.primaryDarkColor` is no longer used.
- **Theme survives recreation.** The theme is passed in the launch Intent instead of a static field,
  so it's kept when Android recreates the chat screen. `ChatwootTheme` is now `Parcelable`.
