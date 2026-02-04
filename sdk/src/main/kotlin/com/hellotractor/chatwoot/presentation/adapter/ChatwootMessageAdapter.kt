package com.hellotractor.chatwoot.presentation.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hellotractor.chatwoot.ChatwootTheme
import com.hellotractor.chatwoot.R
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import java.text.SimpleDateFormat
import java.util.*

class ChatwootMessageAdapter(
    private val theme: ChatwootTheme = ChatwootTheme.default()
) : ListAdapter<ChatwootMessageItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_SENT = 0
        private const val VIEW_TYPE_RECEIVED = 1
        private const val VIEW_TYPE_TYPING = 2

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatwootMessageItem>() {
            override fun areItemsTheSame(oldItem: ChatwootMessageItem, newItem: ChatwootMessageItem): Boolean {
                return when {
                    oldItem is ChatwootMessageItem.Message && newItem is ChatwootMessageItem.Message ->
                        oldItem.message.id == newItem.message.id
                    oldItem is ChatwootMessageItem.TypingIndicator && newItem is ChatwootMessageItem.TypingIndicator -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: ChatwootMessageItem, newItem: ChatwootMessageItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatwootMessageItem.Message -> {
                if (item.message.messageType == ChatwootMessageType.OUTGOING) VIEW_TYPE_SENT
                else VIEW_TYPE_RECEIVED
            }
            is ChatwootMessageItem.TypingIndicator -> VIEW_TYPE_TYPING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                inflater.inflate(R.layout.item_chatwoot_message_sent, parent, false)
            )
            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(
                inflater.inflate(R.layout.item_chatwoot_message_received, parent, false)
            )
            VIEW_TYPE_TYPING -> TypingViewHolder(
                inflater.inflate(R.layout.item_chatwoot_typing_indicator, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatwootMessageItem.Message -> {
                when (holder) {
                    is SentViewHolder -> holder.bind(item.message, theme)
                    is ReceivedViewHolder -> holder.bind(item.message, theme)
                }
            }
            is ChatwootMessageItem.TypingIndicator -> {}
        }
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_message_content)
        private val timeText: TextView = itemView.findViewById(R.id.tv_message_time)
        private val bubbleContainer: View = itemView.findViewById(R.id.bubble_container)

        fun bind(message: ChatwootMessage, theme: ChatwootTheme) {
            messageText.text = message.content ?: ""
            messageText.setTextColor(theme.sentMessageTextColor)

            val bg = bubbleContainer.background
            if (bg is GradientDrawable) {
                bg.setColor(theme.sentMessageBubbleColor)
            } else {
                val drawable = GradientDrawable().apply {
                    setColor(theme.sentMessageBubbleColor)
                    cornerRadius = 16f * itemView.resources.displayMetrics.density
                }
                bubbleContainer.background = drawable
            }

            if (theme.showTimestamps) {
                timeText.visibility = View.VISIBLE
                timeText.text = formatTime(message.createdAt, theme.dateFormat)
                timeText.setTextColor(theme.timestampTextColor)
            } else {
                timeText.visibility = View.GONE
            }
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_message_content)
        private val timeText: TextView = itemView.findViewById(R.id.tv_message_time)
        private val senderName: TextView = itemView.findViewById(R.id.tv_sender_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val bubbleContainer: View = itemView.findViewById(R.id.bubble_container)

        fun bind(message: ChatwootMessage, theme: ChatwootTheme) {
            messageText.text = message.content ?: ""
            messageText.setTextColor(theme.receivedMessageTextColor)

            val bg = bubbleContainer.background
            if (bg is GradientDrawable) {
                bg.setColor(theme.receivedMessageBubbleColor)
            } else {
                val drawable = GradientDrawable().apply {
                    setColor(theme.receivedMessageBubbleColor)
                    cornerRadius = 16f * itemView.resources.displayMetrics.density
                }
                bubbleContainer.background = drawable
            }

            if (message.sender?.name != null) {
                senderName.visibility = View.VISIBLE
                senderName.text = message.sender.name
            } else {
                senderName.visibility = View.GONE
            }

            avatarImage.visibility = if (theme.showAgentAvatar) View.VISIBLE else View.GONE

            if (theme.showTimestamps) {
                timeText.visibility = View.VISIBLE
                timeText.text = formatTime(message.createdAt, theme.dateFormat)
                timeText.setTextColor(theme.timestampTextColor)
            } else {
                timeText.visibility = View.GONE
            }
        }
    }

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

sealed class ChatwootMessageItem {
    data class Message(val message: ChatwootMessage) : ChatwootMessageItem()
    data object TypingIndicator : ChatwootMessageItem()
}

private fun formatTime(epochSeconds: Long, format: String): String {
    return try {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.format(Date(epochSeconds * 1000))
    } catch (e: Exception) {
        ""
    }
}
