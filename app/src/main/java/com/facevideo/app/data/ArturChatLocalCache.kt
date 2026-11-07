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

    /** Cached profile data is user-scoped and is only used for the first paint. */
    fun readProfile(viewerUserId: String, profileId: String): FaceVideoProfile? {
        val raw = userPreferences(viewerUserId).getString(profileKey(profileId), null)
            ?: return null
        return runCatching { json.decodeFromString<FaceVideoProfile>(raw) }.getOrNull()
    }

    fun saveProfile(viewerUserId: String, profile: FaceVideoProfile) {
        userPreferences(viewerUserId).edit()
            .putString(profileKey(profile.id), json.encodeToString(profile))
            .apply()
    }

    /** Signed story URLs are deliberately not persisted; fresh URLs are requested after startup. */
    @Synchronized
    fun readStories(viewerUserId: String): List<FaceVideoStoryCard> {
        val stories = readList<FaceVideoStoryCard>(
            userPreferences(viewerUserId).getString(KEY_STORIES, null)
        ).filter(::isStoryActive)
        return stories.map { it.copy(mediaUrl = "") }
    }

    @Synchronized
    fun saveStories(viewerUserId: String, stories: List<FaceVideoStoryCard>) {
        val active = stories.filter(::isStoryActive).take(MAX_CACHED_STORIES)
            .map { it.copy(mediaUrl = "") }
        userPreferences(viewerUserId).edit()
            .putString(KEY_STORIES, json.encodeToString(active))
            .apply()
    }

    private fun profileKey(profileId: String) = "$KEY_PROFILE_PREFIX${safeKey(profileId)}"

    private fun isStoryActive(story: FaceVideoStoryCard): Boolean = runCatching {
        java.time.Instant.parse(story.expiresAt).isAfter(java.time.Instant.now())
    }.getOrDefault(false)

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

    @Synchronized
    fun readMessages(userId: String, chatId: String): List<FaceVideoMessage> {
        val preferences = chatPreferences(userId, chatId)
        return readList(preferences.getString(KEY_MESSAGES, null))
    }

    @Synchronized
    fun saveMessages(userId: String, chatId: String, messages: List<FaceVideoMessage>) {
        val recent = sortChatMessagesChronologically(messages)
            .takeLast(MAX_CACHED_MESSAGES)
        chatPreferences(userId, chatId).edit()
            .putString(KEY_MESSAGES, json.encodeToString(recent))
            .apply()
    }

    /** Seeds a chat only when this device has no timeline cached yet. */
    @Synchronized
    fun seedMessagesIfMissing(userId: String, chatId: String, messages: List<FaceVideoMessage>) {
        val preferences = chatPreferences(userId, chatId)
        if (preferences.getString(KEY_MESSAGES, null).isNullOrBlank() && messages.isNotEmpty()) {
            val recent = sortChatMessagesChronologically(messages).takeLast(MAX_CACHED_MESSAGES)
            preferences.edit().putString(KEY_MESSAGES, json.encodeToString(recent)).apply()
        }
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
        const val KEY_STORIES = "visible_stories_v1"
        const val KEY_PROFILE_PREFIX = "profile_v1_"
        const val MAX_CACHED_MESSAGES = 80
        const val MAX_CACHED_STORIES = 200
    }
}
