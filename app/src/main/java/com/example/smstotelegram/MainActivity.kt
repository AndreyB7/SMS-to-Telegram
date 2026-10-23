package com.example.smstotelegram

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        binding.chatIdInput.setText(prefs.chatId)

        binding.saveButton.setOnClickListener { save() }
        binding.testButton.setOnClickListener { sendTest() }
        binding.grantButton.setOnClickListener { ensureSmsPermission() }

        updatePermissionStatus()
    }

    private fun save() {
        prefs.botToken = binding.tokenInput.text?.toString().orEmpty()
        prefs.chatId = binding.chatIdInput.text?.toString().orEmpty()
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        ensureSmsPermission()
    }

    private fun sendTest() {
        save()
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
}
