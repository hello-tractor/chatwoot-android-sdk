package com.hellotractor.chatwoot.domain

import com.google.common.truth.Truth.assertThat
import com.hellotractor.chatwoot.data.remote.websocket.ChatwootWebSocketManager
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import com.hellotractor.chatwoot.domain.repository.ChatwootRepository
import com.hellotractor.chatwoot.util.ChatwootWebSocketEvent
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ChatwootUnreadManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var webSocketManager: ChatwootWebSocketManager
    private lateinit var repository: ChatwootRepository
    private lateinit var eventFlow: MutableSharedFlow<ChatwootWebSocketEvent>
    private lateinit var manager: ChatwootUnreadManager

    @Before
    fun setup() {
        eventFlow = MutableSharedFlow(extraBufferCapacity = 64)
        webSocketManager = mockk {
            every { events } returns eventFlow
        }
        repository = mockk {
            every { getLastSeenAt() } returns 0L
            justRun { saveLastSeenAt(any()) }
        }
        manager = ChatwootUnreadManager(webSocketManager, repository, testScope)
        manager.startListening()
    }

    private fun outgoingMessage(id: Int = 1, createdAt: Long = 1000L) = ChatwootMessage(
        id = id,
        content = "Hello from agent",
        messageType = ChatwootMessageType.OUTGOING,
        createdAt = createdAt,
        conversationId = 100
    )

    // 1. Initial state is empty
    @Test
    fun `initial state has count 0 and no messages`() {
        val state = manager.unreadState.value
        assertThat(state.count).isEqualTo(0)
        assertThat(state.latestMessages).isEmpty()
        assertThat(state.hasUnread).isFalse()
    }

    // 2. Agent message (OUTGOING) increments unread count
    @Test
    fun `outgoing message increments unread count`() = runTest(testDispatcher) {
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(outgoingMessage()))

        val state = manager.unreadState.value
        assertThat(state.count).isEqualTo(1)
        assertThat(state.hasUnread).isTrue()
    }

    // 3. User message (INCOMING) is ignored
    @Test
    fun `incoming message does not increment unread count`() = runTest(testDispatcher) {
        val incoming = ChatwootMessage(
            id = 1,
            content = "Hello from user",
            messageType = ChatwootMessageType.INCOMING,
            createdAt = 1000L,
            conversationId = 100
        )
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(incoming))

        assertThat(manager.unreadState.value.count).isEqualTo(0)
    }

    // 4. Activity message is ignored
    @Test
    fun `activity message does not increment unread count`() = runTest(testDispatcher) {
        val activity = ChatwootMessage(
            id = 1,
            content = "Conversation was created",
            messageType = ChatwootMessageType.ACTIVITY,
            createdAt = 1000L,
            conversationId = 100
        )
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(activity))

        assertThat(manager.unreadState.value.count).isEqualTo(0)
    }

    // 5. Messages while chat is open are not counted
    @Test
    fun `messages while chat is open are ignored`() = runTest(testDispatcher) {
        manager.isChatOpen = true
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(outgoingMessage()))

        assertThat(manager.unreadState.value.count).isEqualTo(0)
    }

    // 6. Messages older than lastSeenAt are not counted
    @Test
    fun `messages older than lastSeenAt are ignored`() = runTest(testDispatcher) {
        every { repository.getLastSeenAt() } returns 100L
        val oldMessage = outgoingMessage(createdAt = 50L)
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(oldMessage))

        assertThat(manager.unreadState.value.count).isEqualTo(0)
    }

    // 7. Duplicate messages are not counted
    @Test
    fun `duplicate message id is counted only once`() = runTest(testDispatcher) {
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(outgoingMessage(id = 42)))
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(outgoingMessage(id = 42)))

        assertThat(manager.unreadState.value.count).isEqualTo(1)
    }

    // 8. markSeen resets state and saves lastSeenAt
    @Test
    fun `markSeen resets count to 0 and persists lastSeenAt`() = runTest(testDispatcher) {
        eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(outgoingMessage()))
        assertThat(manager.unreadState.value.count).isEqualTo(1)

        manager.markSeen()

        assertThat(manager.unreadState.value.count).isEqualTo(0)
        assertThat(manager.unreadState.value.latestMessages).isEmpty()
        assertThat(manager.unreadState.value.hasUnread).isFalse()
        verify { repository.saveLastSeenAt(any()) }
    }

    // 9. latestMessages caps at 3 (MAX_PREVIEW_MESSAGES)
    @Test
    fun `latestMessages contains at most 3 messages`() = runTest(testDispatcher) {
        for (i in 1..5) {
            eventFlow.emit(ChatwootWebSocketEvent.MessageCreated(outgoingMessage(id = i, createdAt = i.toLong() * 100)))
        }

        val state = manager.unreadState.value
        assertThat(state.count).isEqualTo(5)
        assertThat(state.latestMessages).hasSize(3)
        // Should be the last 3 messages
        assertThat(state.latestMessages.map { it.id }).containsExactly(3, 4, 5).inOrder()
    }

    // 10. Non-MessageCreated events are ignored
    @Test
    fun `typing started event does not affect unread state`() = runTest(testDispatcher) {
        eventFlow.emit(ChatwootWebSocketEvent.TypingStarted)

        assertThat(manager.unreadState.value.count).isEqualTo(0)
        assertThat(manager.unreadState.value.hasUnread).isFalse()
    }
}
