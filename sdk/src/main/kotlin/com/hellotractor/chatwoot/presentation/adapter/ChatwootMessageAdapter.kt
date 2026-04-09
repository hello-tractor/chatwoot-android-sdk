package com.hellotractor.chatwoot.presentation.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.hellotractor.chatwoot.ChatwootTheme
import com.hellotractor.chatwoot.R
import com.hellotractor.chatwoot.domain.model.ChatwootAttachment
import com.hellotractor.chatwoot.domain.model.ChatwootMessage
import com.hellotractor.chatwoot.domain.model.ChatwootMessageType
import java.text.SimpleDateFormat
import java.util.*

class ChatwootMessageAdapter(
    private val theme: ChatwootTheme = ChatwootTheme.default(),
    private val onAttachmentClick: ((ChatwootAttachment) -> Unit)? = null
) : ListAdapter<ChatwootMessageItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_SENT = 0
        private const val VIEW_TYPE_RECEIVED = 1
        private const val VIEW_TYPE_TYPING = 2

        internal val IMAGE_TYPES = setOf("image", "image/jpeg", "image/png", "image/gif", "image/webp")

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
                // Chatwoot message_type: 0=incoming (from contact), 1=outgoing (from agent)
                // From the mobile user's perspective: incoming(0) = I sent it, outgoing(1) = agent sent it
                if (item.message.messageType == ChatwootMessageType.INCOMING) VIEW_TYPE_SENT
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
                    is SentViewHolder -> holder.bind(item.message, theme, onAttachmentClick)
                    is ReceivedViewHolder -> holder.bind(item.message, theme, onAttachmentClick)
                }
            }
            is ChatwootMessageItem.TypingIndicator -> {}
        }
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_message_content)
        private val timeText: TextView = itemView.findViewById(R.id.tv_message_time)
        private val bubbleContainer: View = itemView.findViewById(R.id.bubble_container)
        private val attachmentImage: ImageView = itemView.findViewById(R.id.iv_attachment_image)
        private val fileContainer: LinearLayout = itemView.findViewById(R.id.file_attachment_container)
        private val fileName: TextView = itemView.findViewById(R.id.tv_file_name)

        fun bind(message: ChatwootMessage, theme: ChatwootTheme, onAttachmentClick: ((ChatwootAttachment) -> Unit)?) {
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

            // Bind attachments
            bindAttachments(message, attachmentImage, fileContainer, fileName, onAttachmentClick)

            // Show text content (hide if empty and has attachment)
            val hasAttachment = message.attachments.isNotEmpty()
            if (message.content.isNullOrBlank() && hasAttachment) {
                messageText.visibility = View.GONE
            } else {
                messageText.visibility = View.VISIBLE
                messageText.text = message.content ?: ""
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
        private val attachmentImage: ImageView = itemView.findViewById(R.id.iv_attachment_image)
        private val fileContainer: LinearLayout = itemView.findViewById(R.id.file_attachment_container)
        private val fileName: TextView = itemView.findViewById(R.id.tv_file_name)

        fun bind(message: ChatwootMessage, theme: ChatwootTheme, onAttachmentClick: ((ChatwootAttachment) -> Unit)?) {
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

            // Agent name
            if (theme.showAgentName && message.sender?.name != null) {
                senderName.visibility = View.VISIBLE
                senderName.text = message.sender.name
            } else {
                senderName.visibility = View.GONE
            }

            // Agent avatar via Glide
            if (theme.showAgentAvatar) {
                avatarImage.visibility = View.VISIBLE
                if (!message.sender?.avatarUrl.isNullOrBlank()) {
                    Glide.with(itemView)
                        .load(message.sender?.avatarUrl)
                        .transform(CircleCrop())
                        .placeholder(R.drawable.ic_chatwoot_agent_avatar)
                        .error(R.drawable.ic_chatwoot_agent_avatar)
                        .into(avatarImage)
                } else {
                    avatarImage.setImageResource(R.drawable.ic_chatwoot_agent_avatar)
                }
            } else {
                avatarImage.visibility = View.GONE
            }

            // Bind attachments
            bindAttachments(message, attachmentImage, fileContainer, fileName, onAttachmentClick)

            // Show text content (hide if empty and has attachment)
            val hasAttachment = message.attachments.isNotEmpty()
            if (message.content.isNullOrBlank() && hasAttachment) {
                messageText.visibility = View.GONE
            } else {
                messageText.visibility = View.VISIBLE
                messageText.text = message.content ?: ""
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

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

private fun bindAttachments(
    message: ChatwootMessage,
    attachmentImage: ImageView,
    fileContainer: LinearLayout,
    fileName: TextView,
    onAttachmentClick: ((ChatwootAttachment) -> Unit)?
) {
    val attachment = message.attachments.firstOrNull()
    if (attachment == null) {
        attachmentImage.visibility = View.GONE
        fileContainer.visibility = View.GONE
        return
    }

    val isImage = ChatwootMessageAdapter.IMAGE_TYPES.any {
        attachment.fileType?.contains(it, ignoreCase = true) == true
    }

    if (isImage && !attachment.dataUrl.isNullOrBlank()) {
        attachmentImage.visibility = View.VISIBLE
        fileContainer.visibility = View.GONE
        Glide.with(attachmentImage)
            .load(attachment.thumbUrl ?: attachment.dataUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(attachmentImage)
        attachmentImage.setOnClickListener { onAttachmentClick?.invoke(attachment) }
    } else if (!attachment.dataUrl.isNullOrBlank()) {
        attachmentImage.visibility = View.GONE
        fileContainer.visibility = View.VISIBLE
        val name = attachment.dataUrl.substringAfterLast('/')
        fileName.text = name
        fileContainer.setOnClickListener { onAttachmentClick?.invoke(attachment) }
    } else {
        attachmentImage.visibility = View.GONE
        fileContainer.visibility = View.GONE
    }
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
