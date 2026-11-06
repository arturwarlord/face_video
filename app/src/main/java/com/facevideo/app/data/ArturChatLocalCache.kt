package com.facevideo.app.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Small user-scoped startup cache.
 *
 * The cache is only a fast first paint: Supabase remains the source of truth.
 * Chats/unread state and each chat timeline are stored in separate private
 * SharedPreferences files so one large conversation cannot inflate one XML file.
 */
class ArturChatLocalCache(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun readChats(userId: String): List<FaceVideoChatSummary> =
        readList(userPreferences(userId).getString(KEY_CHATS, null))

    fun saveChats(userId: String, chats: List<FaceVideoChatSummary>) {
        userPreferences(userId).edit()
            .putString(KEY_CHATS, json.encodeToString(chats))
            .apply()
    }

    fun readUnreadSummaries(userId: String): Map<String, FaceVideoChatUnreadSummary> =
        readList<FaceVideoChatUnreadSummary>(
            userPreferences(userId).getString(KEY_UNREAD, null)
        ).associateBy { it.chatId }

    fun saveUnreadSummaries(
        userId: String,
        summaries: Map<String, FaceVideoChatUnreadSummary>
    ) {
        userPreferences(userId).edit()
            .putString(KEY_UNREAD, json.encodeToString(summaries.values.toList()))
            .apply()
    }

    fun readMessages(userId: String, chatId: String): List<FaceVideoMessage> {
        val preferences = chatPreferences(userId, chatId)
        return readList(preferences.getString(KEY_MESSAGES, null))
    }

    fun saveMessages(userId: String, chatId: String, messages: List<FaceVideoMessage>) {
        val recent = sortChatMessagesChronologically(messages)
            .takeLast(MAX_CACHED_MESSAGES)
        chatPreferences(userId, chatId).edit()
            .putString(KEY_MESSAGES, json.encodeToString(recent))
            .apply()
    }

    private inline fun <reified T> readList(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<T>>(raw) }
            .getOrElse { emptyList() }
    }

    private fun userPreferences(userId: String) =
        appContext.getSharedPreferences(
            "arturchat_cache_user_${safeKey(userId)}",
            Context.MODE_PRIVATE
        )

    private fun chatPreferences(userId: String, chatId: String) =
        appContext.getSharedPreferences(
            "arturchat_cache_user_${safeKey(userId)}_chat_${safeKey(chatId)}",
            Context.MODE_PRIVATE
        )

    private fun safeKey(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private companion object {
        const val KEY_CHATS = "chat_summaries_v1"
        const val KEY_UNREAD = "unread_summaries_v1"
        const val KEY_MESSAGES = "messages_v1"
        const val MAX_CACHED_MESSAGES = 80
    }
}
