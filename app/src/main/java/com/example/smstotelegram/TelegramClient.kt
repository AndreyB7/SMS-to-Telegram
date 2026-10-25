package com.example.smstotelegram

import android.util.Log
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
