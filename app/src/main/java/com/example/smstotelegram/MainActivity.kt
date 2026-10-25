package com.example.smstotelegram

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.smstotelegram.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val requestSmsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            binding.permissionStatus.text = if (granted)
                getString(R.string.perm_granted)
            else
                getString(R.string.perm_denied)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        binding.tokenInput.setText(prefs.botToken)

        binding.testButton.setOnClickListener { sendTest() }
        binding.grantButton.setOnClickListener { ensureSmsPermission() }
        binding.batteryButton.setOnClickListener { requestIgnoreBatteryOptimizations() }
        binding.checkButton.setOnClickListener { checkBotConnection() }
        binding.findChatButton.setOnClickListener { findChat() }

        // Persist the token as it is typed — no Save button needed.
        binding.tokenInput.doAfterTextChanged {
            prefs.botToken = it?.toString().orEmpty()
            updateTestEnabled()
        }

        updateChatStatus()
        updateTestEnabled()
        refreshStatus()
        // Check whether the bot service is reachable right at startup.
        checkBotConnection()
    }

    override fun onResume() {
        super.onResume()
        // The battery dialog takes the user out of the app; re-check on return.
        refreshStatus()
    }

    private fun refreshStatus() {
        updatePermissionStatus()
        updateBatteryStatus()
    }

    private fun findChat() {
        val token = binding.tokenInput.text?.toString().orEmpty().trim()
        if (token.isEmpty()) {
            Toast.makeText(this, R.string.chat_need_token, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.botToken = token
        binding.chatStatus.text = getString(R.string.chat_searching)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { TelegramClient.findChat(token) }
            when (result) {
                is ChatLookup.Found -> {
                    prefs.chatId = result.chatId
                    prefs.chatName = result.chatName
                    updateChatStatus()
                    updateTestEnabled()
                    checkBotConnection()
                }
                is ChatLookup.NoMessages ->
                    binding.chatStatus.text = getString(R.string.chat_none)
                is ChatLookup.Error ->
                    binding.chatStatus.text = getString(R.string.chat_error, result.message)
            }
        }
    }

    private fun updateChatStatus() {
        binding.chatStatus.text = if (prefs.chatId.isNotEmpty())
            getString(R.string.chat_found, prefs.chatName, prefs.chatId)
        else
            getString(R.string.chat_not_set)
    }

    /** Test can be sent only once the token and chat are both set. */
    private fun updateTestEnabled() {
        binding.testButton.isEnabled = prefs.isConfigured
    }

    private fun sendTest() {
        if (!prefs.isConfigured) {
            Toast.makeText(this, R.string.fill_fields, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                TelegramClient.sendMessage(
                    prefs.botToken,
                    prefs.chatId,
                    getString(R.string.test_message)
                )
            }
            Toast.makeText(
                this@MainActivity,
                if (ok) R.string.test_ok else R.string.test_fail,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun ensureSmsPermission() {
        if (!hasSmsPermission()) {
            requestSmsPermission.launch(Manifest.permission.RECEIVE_SMS)
        }
        updatePermissionStatus()
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun updatePermissionStatus() {
        binding.permissionStatus.text = if (hasSmsPermission())
            getString(R.string.perm_granted)
        else
            getString(R.string.perm_denied)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    @android.annotation.SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) {
            updateBatteryStatus()
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open the battery optimization settings list.
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun updateBatteryStatus() {
        binding.batteryStatus.text = if (isIgnoringBatteryOptimizations())
            getString(R.string.battery_granted)
        else
            getString(R.string.battery_denied)
    }

    /**
     * Checks that the Telegram bot service is reachable (getMe) and updates the
     * status line. On failure, reveals the "Check connection" button so the user
     * can retry; on success the button is hidden.
     */
    private fun checkBotConnection() {
        if (!prefs.isConfigured) {
            binding.connectionStatus.text = getString(R.string.fill_fields)
            binding.checkButton.visibility = View.GONE
            return
        }
        binding.connectionStatus.text = getString(R.string.conn_checking)
        binding.checkButton.visibility = View.GONE
        lifecycleScope.launch {
            val available = withContext(Dispatchers.IO) {
                TelegramClient.checkConnection(prefs.botToken)
            }
            if (available) {
                binding.connectionStatus.text = getString(R.string.conn_available)
                binding.checkButton.visibility = View.GONE
            } else {
                binding.connectionStatus.text = getString(R.string.conn_unavailable)
                binding.checkButton.visibility = View.VISIBLE
            }
        }
    }
}
