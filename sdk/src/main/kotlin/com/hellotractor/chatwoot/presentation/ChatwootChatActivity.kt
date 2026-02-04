package com.hellotractor.chatwoot.presentation

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hellotractor.chatwoot.ChatwootTheme
import com.hellotractor.chatwoot.R
import com.hellotractor.chatwoot.databinding.ActivityChatwootChatBinding
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.presentation.adapter.ChatwootMessageAdapter
import com.hellotractor.chatwoot.presentation.adapter.ChatwootMessageItem
import com.hellotractor.chatwoot.presentation.state.ChatwootUiEffect
import com.hellotractor.chatwoot.presentation.state.ChatwootUiEvent
import com.hellotractor.chatwoot.util.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

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
    private val viewModel: ChatwootViewModel by viewModel()
    private lateinit var messageAdapter: ChatwootMessageAdapter
    private var typingJob: Job? = null

    private val theme: ChatwootTheme
        get() = themeOverride ?: ChatwootTheme.default()

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
        messageAdapter = ChatwootMessageAdapter(theme)
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatwootChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text?.toString()?.trim()
            if (!content.isNullOrEmpty()) {
                viewModel.onEvent(ChatwootUiEvent.SendMessage(content))
                binding.etMessage.text?.clear()
            }
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
                        is ChatwootUiEffect.ShowError -> {}
                        is ChatwootUiEffect.MessageSent -> {}
                        is ChatwootUiEffect.ConversationResolved -> {}
                    }
                }
            }
        }
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
