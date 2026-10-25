package com.example.smstotelegram

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Result of looking up the chat id via getUpdates. */
sealed class ChatLookup {
    data class Found(val chatId: String, val chatName: String) : ChatLookup()
    /** API reachable, but no recent messages to the bot. */
    object NoMessages : ChatLookup()
    data class Error(val message: String) : ChatLookup()
}

/** Minimal Telegram Bot API client using the standard library only. */
object TelegramClient {

    private const val TAG = "TelegramClient"

    /**
     * Sends [text] to [chatId] using [botToken] via the sendMessage method.
     * Blocking call — run it off the main thread. Returns true on HTTP 200.
     */
    fun sendMessage(botToken: String, chatId: String, text: String): Boolean {
        val endpoint = "https://api.telegram.org/bot$botToken/sendMessage"
        val body = buildString {
            append("chat_id=").append(enc(chatId))
            append("&text=").append(enc(text))
            append("&disable_web_page_preview=true")
        }

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
                )
            }
            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                true
            } else {
                val err = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                Log.w(TAG, "Telegram API returned $code: $err")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Checks that the Telegram Bot API is reachable and the token is valid,
     * without sending any message (calls the getMe method).
     * Blocking call — run it off the main thread. Returns true on HTTP 200.
     */
    fun checkConnection(botToken: String): Boolean {
        if (botToken.isBlank()) return false
        val endpoint = "https://api.telegram.org/bot$botToken/getMe"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.w(TAG, "Connection check failed", e)
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Finds the chat id by reading recent updates (getUpdates). Picks the chat
     * from the most recent message-like update. Blocking — run off main thread.
     */
    fun findChat(botToken: String): ChatLookup {
        if (botToken.isBlank()) return ChatLookup.Error("empty token")
        val endpoint = "https://api.telegram.org/bot$botToken/getUpdates"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val code = conn.responseCode
            val stream = if (code == HttpURLConnection.HTTP_OK) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) {
                return ChatLookup.Error(json.optString("description", "HTTP $code"))
            }
            val updates = json.optJSONArray("result")
            if (updates == null || updates.length() == 0) return ChatLookup.NoMessages

            val keys = listOf(
                "message", "edited_message", "channel_post",
                "edited_channel_post", "my_chat_member"
            )
            for (i in updates.length() - 1 downTo 0) {
                val upd = updates.optJSONObject(i) ?: continue
                for (k in keys) {
                    val msg = upd.optJSONObject(k) ?: continue
                    val chat = msg.optJSONObject("chat") ?: continue
                    if (!chat.has("id")) continue
                    return ChatLookup.Found(chat.get("id").toString(), chatName(chat))
                }
            }
            ChatLookup.NoMessages
        } catch (e: Exception) {
            Log.w(TAG, "findChat failed", e)
            ChatLookup.Error(e.message ?: "network error")
        } finally {
            conn?.disconnect()
        }
    }

    private fun chatName(chat: JSONObject): String {
        chat.optString("title").takeIf { it.isNotEmpty() }?.let { return it }
        val full = (chat.optString("first_name") + " " + chat.optString("last_name")).trim()
        if (full.isNotEmpty()) return full
        chat.optString("username").takeIf { it.isNotEmpty() }?.let { return "@$it" }
        return "chat"
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
