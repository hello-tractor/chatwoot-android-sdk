package com.hellotractor.chatwoot.presentation

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hellotractor.chatwoot.ChatwootTheme
import com.hellotractor.chatwoot.R
import com.hellotractor.chatwoot.databinding.ActivityChatwootChatBinding
import com.hellotractor.chatwoot.domain.model.ChatwootAttachment
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.presentation.adapter.ChatwootMessageAdapter
import com.hellotractor.chatwoot.presentation.adapter.ChatwootMessageItem
import com.hellotractor.chatwoot.presentation.state.ChatwootUiEffect
import com.hellotractor.chatwoot.presentation.state.ChatwootUiEvent
import com.hellotractor.chatwoot.util.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatwootChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_IDENTIFIER = "chatwoot_user_identifier"
        const val EXTRA_USER_NAME = "chatwoot_user_name"
        const val EXTRA_USER_EMAIL = "chatwoot_user_email"
        const val EXTRA_USER_AVATAR_URL = "chatwoot_user_avatar_url"
        const val EXTRA_USER_IDENTIFIER_HASH = "chatwoot_user_identifier_hash"

        internal var themeOverride: ChatwootTheme? = null
    }

    private lateinit var binding: ActivityChatwootChatBinding
    private val viewModel: ChatwootViewModel by lazy {
        ViewModelProvider(this, ChatwootViewModelFactory())[ChatwootViewModel::class.java]
    }
    private lateinit var messageAdapter: ChatwootMessageAdapter
    private var typingJob: Job? = null

    private val theme: ChatwootTheme
        get() = themeOverride ?: ChatwootTheme.default()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(ChatwootUiEvent.AttachmentSelected(it)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatwootChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupInput()
        applyTheme()
        collectState()
        collectEffects()
        initializeChat()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = theme.toolbarTitle
            subtitle = theme.toolbarSubtitle
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatwootMessageAdapter(theme) { attachment ->
            onAttachmentClicked(attachment)
        }
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy < 0 && layoutManager.findFirstVisibleItemPosition() <= 3) {
                        viewModel.onEvent(ChatwootUiEvent.LoadMoreMessages)
                    }
                }
            })
        }
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text?.toString()?.trim() ?: ""
            val hasPendingAttachment = viewModel.state.value.pendingAttachmentUri != null
            if (content.isNotEmpty() || hasPendingAttachment) {
                viewModel.onEvent(ChatwootUiEvent.SendMessage(content))
                binding.etMessage.text?.clear()
            }
        }

        binding.btnAttach.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnRemoveAttachment.setOnClickListener {
            viewModel.onEvent(ChatwootUiEvent.AttachmentRemoved)
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    viewModel.onEvent(ChatwootUiEvent.StartTyping)
                    typingJob?.cancel()
                    typingJob = lifecycleScope.launch {
                        delay(3000)
                        viewModel.onEvent(ChatwootUiEvent.StopTyping)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMessage.hint = theme.inputHint
    }

    private fun applyTheme() {
        binding.toolbar.setBackgroundColor(theme.primaryColor)
        binding.toolbar.setTitleTextColor(theme.toolbarTextColor)
        binding.toolbar.setSubtitleTextColor(theme.toolbarTextColor)
        binding.toolbar.navigationIcon?.setTint(theme.toolbarTextColor)
        @Suppress("DEPRECATION")
        window.statusBarColor = theme.primaryDarkColor

        binding.root.setBackgroundColor(theme.backgroundColor)
        binding.inputContainer.setBackgroundColor(theme.backgroundColor)
        binding.etMessage.setTextColor(theme.inputTextColor)
        binding.etMessage.setHintTextColor(theme.hintTextColor)
        binding.etMessage.background?.setTint(theme.inputBackgroundColor)
        binding.btnSend.setColorFilter(theme.accentColor)
        binding.btnAttach.setColorFilter(theme.hintTextColor)

        if (theme.showToolbarLogo && theme.logoResId != null) {
            binding.toolbar.setLogo(theme.logoResId!!)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    val items = mutableListOf<ChatwootMessageItem>()
                    items.addAll(state.messages.map { ChatwootMessageItem.Message(it) })
                    if (state.isAgentTyping) {
                        items.add(ChatwootMessageItem.TypingIndicator)
                    }
                    messageAdapter.submitList(items)

                    // Attachment preview bar
                    if (state.pendingAttachmentUri != null) {
                        binding.attachmentPreviewContainer.visibility = View.VISIBLE
                        val fileName = getFileNameFromUri(state.pendingAttachmentUri)
                        binding.tvAttachmentName.text = fileName ?: "Attachment"
                        val mimeType = contentResolver.getType(state.pendingAttachmentUri)
                        if (mimeType?.startsWith("image/") == true) {
                            Glide.with(this@ChatwootChatActivity)
                                .load(state.pendingAttachmentUri)
                                .centerCrop()
                                .into(binding.ivAttachmentPreview)
                        } else {
                            binding.ivAttachmentPreview.setImageResource(R.drawable.ic_chatwoot_file)
                        }
                    } else {
                        binding.attachmentPreviewContainer.visibility = View.GONE
                    }

                    // Upload indicator
                    if (state.isUploading) {
                        binding.btnSend.alpha = 0.5f
                        binding.btnSend.isEnabled = false
                    } else {
                        binding.btnSend.alpha = 1f
                        binding.btnSend.isEnabled = true
                    }

                    when (state.connectionState) {
                        ConnectionState.DISCONNECTED -> {
                            binding.connectionBanner.visibility = View.VISIBLE
                            binding.connectionBanner.text = getString(R.string.chatwoot_disconnected)
                            binding.connectionBanner.setBackgroundColor(0xFFFF5722.toInt())
                        }
                        ConnectionState.CONNECTING -> {
                            binding.connectionBanner.visibility = View.VISIBLE
                            binding.connectionBanner.text = getString(R.string.chatwoot_connecting)
                            binding.connectionBanner.setBackgroundColor(0xFFFF9800.toInt())
                        }
                        ConnectionState.CONNECTED -> {
                            binding.connectionBanner.visibility = View.GONE
                        }
                    }

                    if (state.errorMessage != null) {
                        binding.errorContainer.visibility = View.VISIBLE
                        binding.tvError.text = state.errorMessage
                    } else {
                        binding.errorContainer.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun collectEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ChatwootUiEffect.ScrollToBottom -> {
                            val count = messageAdapter.itemCount
                            if (count > 0) {
                                binding.rvMessages.smoothScrollToPosition(count - 1)
                            }
                        }
                        is ChatwootUiEffect.ShowError -> {
                            Toast.makeText(this@ChatwootChatActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is ChatwootUiEffect.MessageSent -> {}
                        is ChatwootUiEffect.ConversationResolved -> {}
                        is ChatwootUiEffect.OpenImageViewer -> showImageViewer(effect.imageUrl)
                        is ChatwootUiEffect.OpenFileExternal -> openFileExternal(effect.fileUrl, effect.mimeType)
                    }
                }
            }
        }
    }

    private fun onAttachmentClicked(attachment: ChatwootAttachment) {
        val url = attachment.dataUrl ?: return
        val isImage = ChatwootMessageAdapter.IMAGE_TYPES.any { attachment.fileType?.contains(it, ignoreCase = true) == true }
        if (isImage) {
            showImageViewer(url)
        } else {
            openFileExternal(url, null)
        }
    }

    private fun showImageViewer(imageUrl: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_chatwoot_image_viewer)

        val imageView = dialog.findViewById<ImageView>(R.id.iv_fullscreen_image)
        val closeBtn = dialog.findViewById<ImageView>(R.id.btn_close)

        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        closeBtn.setOnClickListener { dialog.dismiss() }
        imageView.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun openFileExternal(fileUrl: String, mimeType: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(fileUrl), mimeType ?: "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun initializeChat() {
        val identifier = intent.getStringExtra(EXTRA_USER_IDENTIFIER) ?: return
        val user = ChatwootUser(
            identifier = identifier,
            identifierHash = intent.getStringExtra(EXTRA_USER_IDENTIFIER_HASH),
            name = intent.getStringExtra(EXTRA_USER_NAME),
            email = intent.getStringExtra(EXTRA_USER_EMAIL),
            avatarUrl = intent.getStringExtra(EXTRA_USER_AVATAR_URL)
        )
        viewModel.initialize(user)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
