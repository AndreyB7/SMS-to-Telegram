package com.example.smstotelegram

import android.content.Context

/** Thin wrapper over SharedPreferences for the bot token and chat id. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var botToken: String
        get() = sp.getString(KEY_TOKEN, "").orEmpty()
        set(value) = sp.edit().putString(KEY_TOKEN, value.trim()).apply()

    var chatId: String
        get() = sp.getString(KEY_CHAT, "").orEmpty()
        set(value) = sp.edit().putString(KEY_CHAT, value.trim()).apply()

    /** Human-readable name of the found chat, for display only. */
    var chatName: String
        get() = sp.getString(KEY_CHAT_NAME, "").orEmpty()
        set(value) = sp.edit().putString(KEY_CHAT_NAME, value).apply()

    val isConfigured: Boolean
        get() = botToken.isNotEmpty() && chatId.isNotEmpty()

    companion object {
        private const val FILE = "sms_to_telegram_prefs"
        private const val KEY_TOKEN = "bot_token"
        private const val KEY_CHAT = "chat_id"
        private const val KEY_CHAT_NAME = "chat_name"
    }
}
