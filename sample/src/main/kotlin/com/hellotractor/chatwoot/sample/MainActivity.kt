package com.hellotractor.chatwoot.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hellotractor.chatwoot.ChatwootLauncher
import com.hellotractor.chatwoot.ChatwootTheme
import com.hellotractor.chatwoot.domain.model.ChatwootUser
import com.hellotractor.chatwoot.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLaunchChat.setOnClickListener {
            launchChat()
        }

        binding.btnLaunchChatHelloTractor.setOnClickListener {
            launchChatWithHelloTractorTheme()
        }
    }

    private fun launchChat() {
        val identifier = binding.etIdentifier.text?.toString()?.trim()
        val name = binding.etName.text?.toString()?.trim()
        val email = binding.etEmail.text?.toString()?.trim()

        if (identifier.isNullOrEmpty()) {
            Toast.makeText(this, "User identifier is required", Toast.LENGTH_SHORT).show()
            return
        }

        ChatwootLauncher.launch(
            context = this,
            user = ChatwootUser(
                identifier = identifier,
                name = name?.takeIf { it.isNotEmpty() },
                email = email?.takeIf { it.isNotEmpty() }
            )
        )
    }

    private fun launchChatWithHelloTractorTheme() {
        val identifier = binding.etIdentifier.text?.toString()?.trim()
        val name = binding.etName.text?.toString()?.trim()
        val email = binding.etEmail.text?.toString()?.trim()

        if (identifier.isNullOrEmpty()) {
            Toast.makeText(this, "User identifier is required", Toast.LENGTH_SHORT).show()
            return
        }

        ChatwootLauncher.launch(
            context = this,
            user = ChatwootUser(
                identifier = identifier,
                name = name?.takeIf { it.isNotEmpty() },
                email = email?.takeIf { it.isNotEmpty() }
            ),
            theme = ChatwootTheme.helloTractor()
        )
    }
}
