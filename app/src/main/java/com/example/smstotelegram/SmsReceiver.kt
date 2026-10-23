package com.example.smstotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import kotlin.concurrent.thread

/**
 * Receives incoming SMS and forwards sender + text to Telegram.
 * Multipart messages from the same sender are concatenated.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages: Array<SmsMessage> =
            Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().displayOriginatingAddress ?: "unknown"
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }

        val prefs = Prefs(context)
        if (!prefs.isConfigured) {
            Log.w(TAG, "Bot token / chat id not set — skipping forward")
            return
        }

        val text = "📩 SMS от $sender\n\n$body"

        // Keep the broadcast alive while we do the network call on a background thread.
        val pending = goAsync()
        thread {
            try {
                TelegramClient.sendMessage(prefs.botToken, prefs.chatId, text)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
