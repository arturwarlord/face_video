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
data class ArturChatNavigationState(
    val selectedTab: String = "CHATS",
    val liveChatId: String? = null,
    val profileUserId: String? = null,
    val storiesUserId: String? = null,
    val demoChatId: String? = null
)

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

    /**
     * Persist the last visible destination independently of the Activity process.
     * Supabase remains the source of truth; IDs are resolved against the local
     * chat cache first and refreshed from the server when the app resumes.
     */
    fun readNavigationState(userId: String): ArturChatNavigationState {
        val preferences = userPreferences(userId)
        return ArturChatNavigationState(
            selectedTab = preferences.getString(KEY_NAV_TAB, "CHATS") ?: "CHATS",
            liveChatId = preferences.getString(KEY_NAV_LIVE_CHAT, null),
            profileUserId = preferences.getString(KEY_NAV_PROFILE, null),
            storiesUserId = preferences.getString(KEY_NAV_STORIES, null),
            demoChatId = preferences.getString(KEY_NAV_DEMO_CHAT, null)
        )
    }

    fun saveNavigationState(userId: String, state: ArturChatNavigationState) {
        userPreferences(userId).edit()
            .putString(KEY_NAV_TAB, state.selectedTab)
            .putNullableString(KEY_NAV_LIVE_CHAT, state.liveChatId)
            .putNullableString(KEY_NAV_PROFILE, state.profileUserId)
            .putNullableString(KEY_NAV_STORIES, state.storiesUserId)
            .putNullableString(KEY_NAV_DEMO_CHAT, state.demoChatId)
            .apply()
    }

    /** Unsaved text is stored per account and per conversation. */
    fun readDraft(userId: String, chatId: String): String =
        chatPreferences(userId, chatId).getString(KEY_DRAFT, "") ?: ""

    fun saveDraft(userId: String, chatId: String, draft: String) {
        val preferences = chatPreferences(userId, chatId)
        preferences.edit().apply {
            if (draft.isBlank()) remove(KEY_DRAFT)
            else putString(KEY_DRAFT, draft)
        }.apply()
    }

    /**
     * Synchronously flush the current composer value before the app is backgrounded
     * or the chat screen is removed. Normal keystrokes still use saveDraft() so
     * the UI never waits for disk I/O on every character.
     */
    fun flushDraft(userId: String, chatId: String, draft: String) {
        val editor = chatPreferences(userId, chatId).edit()
        if (draft.isBlank()) editor.remove(KEY_DRAFT) else editor.putString(KEY_DRAFT, draft)
        if (!editor.commit()) {
            android.util.Log.w("ArturChatDraft", "Could not flush draft for chat $chatId")
        }
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


    fun hasChatsSnapshot(userId: String): Boolean =
        userPreferences(userId).contains(KEY_CHATS)

    fun hasStoriesSnapshot(userId: String): Boolean =
        userPreferences(userId).contains(KEY_STORIES)

    fun hasFriendshipsSnapshot(userId: String): Boolean =
        hasPageSnapshot(userId, "friends_relationships")

    fun hasChannelsSnapshot(userId: String): Boolean =
        hasPageSnapshot(userId, "community_channels")

    fun hasCallsSnapshot(userId: String): Boolean =
        hasPageSnapshot(userId, "calls_history")

    fun hasChannelPostsSnapshot(userId: String, channelId: String): Boolean =
        hasPageSnapshot(userId, "channel_posts_${safeKey(channelId)}")

    fun hasChannelMembersSnapshot(userId: String, channelId: String): Boolean =
        hasPageSnapshot(userId, "channel_members_${safeKey(channelId)}")

    fun hasGroupSnapshot(userId: String, chatId: String): Boolean =
        hasPageItem(userId, "group_details_${safeKey(chatId)}")

    fun hasGroupMembersSnapshot(userId: String, chatId: String): Boolean =
        hasPageSnapshot(userId, "group_members_${safeKey(chatId)}")

    fun hasGroupTopicsSnapshot(userId: String, chatId: String): Boolean =
        hasPageSnapshot(userId, "group_topics_${safeKey(chatId)}")

    fun hasFriendshipSnapshot(userId: String, profileId: String): Boolean =
        screenPreferences(userId, "friendship_${safeKey(profileId)}").contains(KEY_SCREEN_HAS_NULL)

    fun hasScheduledMessagesSnapshot(userId: String, chatId: String): Boolean =
        hasPageSnapshot(userId, "scheduled_${safeKey(chatId)}")

    fun removeStoryReactionSummary(userId: String, storyId: String) {
        screenPreferences(userId, "story_reactions_${safeKey(storyId)}")
            .edit()
            .remove(KEY_SCREEN_ITEM)
            .apply()
    }

    private fun hasPageSnapshot(userId: String, scope: String): Boolean =
        screenPreferences(userId, scope).contains(KEY_SCREEN_ITEMS)

    private fun hasPageItem(userId: String, scope: String): Boolean =
        screenPreferences(userId, scope).contains(KEY_SCREEN_ITEM)

    /** Cached friends and pending relationship requests for this signed-in account. */
    fun readFriendships(userId: String): List<FaceVideoFriendship> =
        readPageList(userId, "friends_relationships")

    fun saveFriendships(userId: String, rows: List<FaceVideoFriendship>) =
        savePageList(userId, "friends_relationships", rows, MAX_CACHED_FRIENDSHIPS)

    fun readFriendship(userId: String, profileId: String): FaceVideoFriendship? =
        readPageItem(userId, "friendship_${safeKey(profileId)}")

    fun saveFriendship(userId: String, profileId: String, friendship: FaceVideoFriendship?) {
        val preferences = screenPreferences(userId, "friendship_${safeKey(profileId)}")
        preferences.edit().putBoolean(KEY_SCREEN_HAS_NULL, friendship == null).apply()
        if (friendship == null) preferences.edit().remove(KEY_SCREEN_ITEM).apply()
        else savePageItem(userId, "friendship_${safeKey(profileId)}", friendship)
    }

    fun readFriendProfiles(userId: String): Map<String, FaceVideoProfile> =
        readPageList<FaceVideoProfile>(userId, "friends_profiles").associateBy { it.id }

    fun saveFriendProfiles(userId: String, profiles: Collection<FaceVideoProfile>) =
        savePageList(userId, "friends_profiles", profiles.toList(), MAX_CACHED_PROFILES)

    fun readChannels(userId: String): List<FaceVideoChannelEntry> =
        readPageList(userId, "community_channels")

    fun saveChannels(userId: String, channels: List<FaceVideoChannelEntry>) =
        savePageList(userId, "community_channels", channels, MAX_CACHED_CHANNELS)

    fun readChannelPosts(viewerUserId: String, channelId: String): List<FaceVideoChannelPost> =
        readPageList(viewerUserId, "channel_posts_${safeKey(channelId)}")

    fun saveChannelPosts(viewerUserId: String, channelId: String, posts: List<FaceVideoChannelPost>) =
        savePageList(viewerUserId, "channel_posts_${safeKey(channelId)}", posts, MAX_CACHED_CHANNEL_POSTS)

    fun readChannelMembers(viewerUserId: String, channelId: String): List<FaceVideoChannelMemberProfile> =
        readPageList(viewerUserId, "channel_members_${safeKey(channelId)}")

    fun saveChannelMembers(
        viewerUserId: String,
        channelId: String,
        members: List<FaceVideoChannelMemberProfile>
    ) = savePageList(
        viewerUserId,
        "channel_members_${safeKey(channelId)}",
        members,
        MAX_CACHED_MEMBERS
    )

    fun readGroup(viewerUserId: String, chatId: String): FaceVideoGroup? =
        readPageItem(viewerUserId, "group_details_${safeKey(chatId)}")

    fun saveGroup(viewerUserId: String, group: FaceVideoGroup) =
        savePageItem(viewerUserId, "group_details_${safeKey(group.id)}", group)

    fun readGroupMembers(viewerUserId: String, chatId: String): List<FaceVideoGroupMemberProfile> =
        readPageList(viewerUserId, "group_members_${safeKey(chatId)}")

    fun saveGroupMembers(
        viewerUserId: String,
        chatId: String,
        members: List<FaceVideoGroupMemberProfile>
    ) = savePageList(
        viewerUserId,
        "group_members_${safeKey(chatId)}",
        members,
        MAX_CACHED_MEMBERS
    )

    fun readGroupTopics(viewerUserId: String, chatId: String): List<FaceVideoGroupTopic> =
        readPageList(viewerUserId, "group_topics_${safeKey(chatId)}")

    fun saveGroupTopics(viewerUserId: String, chatId: String, topics: List<FaceVideoGroupTopic>) =
        savePageList(viewerUserId, "group_topics_${safeKey(chatId)}", topics, MAX_CACHED_TOPICS)

    fun readCalls(userId: String): List<FaceVideoCall> =
        readPageList(userId, "calls_history")

    fun saveCalls(userId: String, calls: List<FaceVideoCall>) =
        savePageList(userId, "calls_history", calls, MAX_CACHED_CALLS)

    fun readCallProfiles(userId: String): Map<String, FaceVideoProfile> =
        readPageList<FaceVideoProfile>(userId, "calls_profiles").associateBy { it.id }

    fun saveCallProfiles(userId: String, profiles: Collection<FaceVideoProfile>) =
        savePageList(userId, "calls_profiles", profiles.toList(), MAX_CACHED_PROFILES)

    fun readProfileStats(viewerUserId: String, profileId: String): FaceVideoProfileStats? =
        readPageItem(viewerUserId, "profile_stats_${safeKey(profileId)}")

    fun saveProfileStats(viewerUserId: String, profileId: String, stats: FaceVideoProfileStats) =
        savePageItem(viewerUserId, "profile_stats_${safeKey(profileId)}", stats)

    fun readChatCallHistory(userId: String, chatId: String): List<FaceVideoCallHistory> =
        readPageList(userId, "chat_calls_${safeKey(chatId)}")

    fun saveChatCallHistory(
        userId: String,
        chatId: String,
        history: List<FaceVideoCallHistory>
    ) = savePageList(userId, "chat_calls_${safeKey(chatId)}", history, MAX_CACHED_CALLS)

    fun readScheduledMessages(userId: String, chatId: String): List<FaceVideoScheduledMessage> =
        readPageList(userId, "scheduled_${safeKey(chatId)}")

    fun saveScheduledMessages(
        userId: String,
        chatId: String,
        messages: List<FaceVideoScheduledMessage>
    ) = savePageList(userId, "scheduled_${safeKey(chatId)}", messages, MAX_CACHED_SCHEDULED_MESSAGES)

    fun readMessageReactions(userId: String, chatId: String): List<FaceVideoMessageReaction> =
        readPageList(userId, "message_reactions_${safeKey(chatId)}")

    fun saveMessageReactions(
        userId: String,
        chatId: String,
        reactions: List<FaceVideoMessageReaction>
    ) = savePageList(userId, "message_reactions_${safeKey(chatId)}", reactions, MAX_CACHED_REACTIONS)

    fun readMessageReceipts(userId: String, chatId: String): List<FaceVideoMessageReceipt> =
        readPageList(userId, "message_receipts_${safeKey(chatId)}")

    fun saveMessageReceipts(
        userId: String,
        chatId: String,
        receipts: List<FaceVideoMessageReceipt>
    ) = savePageList(userId, "message_receipts_${safeKey(chatId)}", receipts, MAX_CACHED_RECEIPTS)

    fun readStoryReactionSummary(viewerUserId: String, storyId: String): com.facevideo.app.model.StoryReactionSummary? =
        readPageItem(viewerUserId, "story_reactions_${safeKey(storyId)}")

    fun saveStoryReactionSummary(
        viewerUserId: String,
        storyId: String,
        summary: com.facevideo.app.model.StoryReactionSummary
    ) = savePageItem(viewerUserId, "story_reactions_${safeKey(storyId)}", summary)

    fun readMutedChatIds(userId: String): Set<String> =
        userPreferences(userId).getStringSet(KEY_MUTED_CHAT_IDS, emptySet()).orEmpty().toSet()

    fun saveMutedChatIds(userId: String, chatIds: Set<String>) {
        userPreferences(userId).edit()
            .putStringSet(KEY_MUTED_CHAT_IDS, chatIds.toSet())
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


    /**
     * Screen snapshots are isolated by account and destination so a long channel
     * feed or busy call list cannot inflate the main account preferences XML.
     * Snapshots are a first-paint cache only; each screen refreshes from Supabase.
     */
    private fun screenPreferences(userId: String, scope: String) =
        appContext.getSharedPreferences(
            "arturchat_cache_user_${safeKey(userId)}_screen_${safeKey(scope)}",
            Context.MODE_PRIVATE
        )

    private inline fun <reified T> readPageList(userId: String, scope: String): List<T> =
        readList(screenPreferences(userId, scope).getString(KEY_SCREEN_ITEMS, null))

    private inline fun <reified T> savePageList(
        userId: String,
        scope: String,
        items: List<T>,
        maxItems: Int
    ) {
        val bounded = items.take(maxItems)
        screenPreferences(userId, scope).edit()
            .putString(KEY_SCREEN_ITEMS, json.encodeToString(bounded))
            .apply()
    }

    private inline fun <reified T> readPageItem(userId: String, scope: String): T? =
        screenPreferences(userId, scope).getString(KEY_SCREEN_ITEM, null)
            ?.let { raw -> runCatching { json.decodeFromString<T>(raw) }.getOrNull() }

    private inline fun <reified T> savePageItem(userId: String, scope: String, item: T) {
        screenPreferences(userId, scope).edit()
            .putString(KEY_SCREEN_ITEM, json.encodeToString(item))
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

    private fun android.content.SharedPreferences.Editor.putNullableString(
        key: String,
        value: String?
    ): android.content.SharedPreferences.Editor =
        if (value == null) remove(key) else putString(key, value)

    private fun safeKey(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private companion object {
        const val KEY_CHATS = "chat_summaries_v1"
        const val KEY_UNREAD = "unread_summaries_v1"
        const val KEY_MESSAGES = "messages_v1"
        const val KEY_STORIES = "visible_stories_v1"
        const val KEY_PROFILE_PREFIX = "profile_v1_"
        const val KEY_NAV_TAB = "navigation_tab_v1"
        const val KEY_NAV_LIVE_CHAT = "navigation_live_chat_v1"
        const val KEY_NAV_PROFILE = "navigation_profile_v1"
        const val KEY_NAV_STORIES = "navigation_stories_v1"
        const val KEY_NAV_DEMO_CHAT = "navigation_demo_chat_v1"
        const val KEY_DRAFT = "unsent_draft_v1"
        const val KEY_MUTED_CHAT_IDS = "muted_chat_ids_v1"
        const val KEY_SCREEN_ITEMS = "items_v1"
        const val KEY_SCREEN_ITEM = "item_v1"
        const val KEY_SCREEN_HAS_NULL = "has_snapshot_even_if_null_v1"
        const val MAX_CACHED_MESSAGES = 80
        const val MAX_CACHED_FRIENDSHIPS = 500
        const val MAX_CACHED_PROFILES = 500
        const val MAX_CACHED_CHANNELS = 300
        const val MAX_CACHED_CHANNEL_POSTS = 120
        const val MAX_CACHED_MEMBERS = 500
        const val MAX_CACHED_TOPICS = 200
        const val MAX_CACHED_CALLS = 200
        const val MAX_CACHED_SCHEDULED_MESSAGES = 100
        const val MAX_CACHED_REACTIONS = 1500
        const val MAX_CACHED_RECEIPTS = 2000
        const val MAX_CACHED_STORIES = 200
    }
}
