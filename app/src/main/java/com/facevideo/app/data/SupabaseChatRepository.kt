package com.facevideo.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FaceVideoChatSummary(
    val id: String,
    val title: String,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("last_message_preview") val lastMessagePreview: String = "",
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    // Enriched on the client from chat members + profiles; not columns in the SQL view.
    @SerialName("peer_user_id") val peerUserId: String? = null,
    @SerialName("peer_avatar_url") val peerAvatarUrl: String? = null
)

@Serializable
private data class FaceVideoOnlinePresenceRow(
    @SerialName("user_id") val userId: String,
    @SerialName("last_seen_at") val lastSeenAt: String
)

@Serializable
data class FaceVideoProfile(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("status_text") val statusText: String = "",
    val about: String = ""
)

@Serializable
data class FaceVideoFriendship(
    val id: String,
    @SerialName("user_low") val userLow: String,
    @SerialName("user_high") val userHigh: String,
    @SerialName("requested_by") val requestedBy: String,
    val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FaceVideoStory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_path") val mediaPath: String,
    val caption: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String
)

data class FaceVideoStoryCard(
    val storyId: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val mediaUrl: String,
    val caption: String,
    val createdAt: String,
    val expiresAt: String
)

@Serializable
private data class NewFaceVideoStory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_path") val mediaPath: String,
    val caption: String,
    @SerialName("expires_at") val expiresAt: String
)



@Serializable
data class FaceVideoMessage(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("attachment_path") val attachmentPath: String? = null,
    @SerialName("attachment_mime_type") val attachmentMimeType: String? = null,
    @SerialName("attachment_file_name") val attachmentFileName: String? = null,
    @SerialName("attachment_size") val attachmentSize: Long? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deleted_by") val deletedBy: String? = null,
    @SerialName("forwarded_from_sender_id") val forwardedFromSenderId: String? = null,
    @SerialName("forwarded_from_name") val forwardedFromName: String? = null
)

@Serializable
data class FaceVideoChatUnreadCount(
    @SerialName("chat_id") val chatId: String,
    @SerialName("unread_count") val unreadCount: Long
)

@Serializable
data class FaceVideoChatUnreadSummary(
    @SerialName("chat_id") val chatId: String,
    @SerialName("unread_count") val unreadCount: Long,
    @SerialName("last_unread_body") val lastUnreadBody: String? = null,
    @SerialName("last_unread_at") val lastUnreadAt: String? = null
)

@Serializable
data class FaceVideoMessageReceipt(
    val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null
)

@Serializable
private data class FaceVideoChatMemberUserId(
    @SerialName("user_id") val userId: String
)

@Serializable
private data class FaceVideoChatAvatarMember(
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class MarkChatReadArguments(
    @SerialName("p_chat_id") val chatId: String
)

@Serializable
private data class MarkMessagesDeliveredArguments(
    @SerialName("p_message_ids") val messageIds: List<String>
)

@Serializable
private data class CreateChatArguments(
    @SerialName("p_member_ids") val memberIds: List<String>,
    @SerialName("p_title") val title: String? = null,
    @SerialName("p_is_group") val isGroup: Boolean = false
)

@Serializable
private data class HiddenMessage(@SerialName("user_id") val userId: String, @SerialName("message_id") val messageId: String)

@Serializable
private data class HiddenChat(@SerialName("user_id") val userId: String, @SerialName("chat_id") val chatId: String)

@Serializable
private data class FriendTargetArguments(
    @SerialName("p_target_user_id") val targetUserId: String
)

@Serializable
private data class RespondFriendRequestArguments(
    @SerialName("p_target_user_id") val targetUserId: String,
    @SerialName("p_accept") val accept: Boolean
)


@Serializable
private data class DeletedMessage(
    @SerialName("deleted_at") val deletedAt: String,
    @SerialName("deleted_by") val deletedBy: String,
    val body: String,
    @SerialName("attachment_path") val attachmentPath: String? = null,
    @SerialName("attachment_mime_type") val attachmentMimeType: String? = null,
    @SerialName("attachment_file_name") val attachmentFileName: String? = null,
    @SerialName("attachment_size") val attachmentSize: Long? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null
)

@Serializable
private data class NewMessage(
    @SerialName("chat_id") val chatId: String,
    val body: String,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("attachment_path") val attachmentPath: String? = null,
    @SerialName("attachment_mime_type") val attachmentMimeType: String? = null,
    @SerialName("attachment_file_name") val attachmentFileName: String? = null,
    @SerialName("attachment_size") val attachmentSize: Long? = null,
    @SerialName("forwarded_from_sender_id") val forwardedFromSenderId: String? = null,
    @SerialName("forwarded_from_name") val forwardedFromName: String? = null
)

@Serializable
private data class UpdateFaceVideoProfile(
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("status_text") val statusText: String,
    val about: String,
    @SerialName("avatar_url") val avatarUrl: String?
)

@Serializable
private data class NewFaceVideoPushToken(
    @SerialName("user_id") val userId: String,
    val token: String,
    val platform: String = "android"
)

/**
 * User-scoped operations. The Supabase database enforces access through RLS.
 * Never add a service_role key to this Android client.
 */
class SupabaseChatRepository(
    private val client: SupabaseClient
) {
    companion object {
        private const val STORIES_BUCKET = "face-video-stories"
    }


    suspend fun getMyProfile(userId: String): FaceVideoProfile? =
        client.from("face_video_profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeList<FaceVideoProfile>()
            .firstOrNull()

    suspend fun getProfile(userId: String): FaceVideoProfile? =
        client.from("face_video_profiles")
            .select { filter { eq("id", userId) } }
            .decodeList<FaceVideoProfile>()
            .firstOrNull()

    /** Lists relationships visible to this user (accepted friends and pending requests). */
    suspend fun listMyFriendships(userId: String): List<FaceVideoFriendship> {
        val asLow = client.from("face_video_friendships").select {
            filter { eq("user_low", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<FaceVideoFriendship>()
        val asHigh = client.from("face_video_friendships").select {
            filter { eq("user_high", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<FaceVideoFriendship>()
        return (asLow + asHigh).distinctBy { it.id }
    }

    suspend fun getFriendshipWith(userId: String, targetUserId: String): FaceVideoFriendship? =
        listMyFriendships(userId).firstOrNull { row ->
            (row.userLow == userId && row.userHigh == targetUserId) ||
                (row.userHigh == userId && row.userLow == targetUserId)
        }

    suspend fun sendFriendRequest(targetUserId: String): String =
        client.postgrest.rpc(
            "face_video_send_friend_request",
            FriendTargetArguments(targetUserId)
        ).decodeAs<String>()

    suspend fun respondToFriendRequest(targetUserId: String, accept: Boolean): String =
        client.postgrest.rpc(
            "face_video_respond_friend_request",
            RespondFriendRequestArguments(targetUserId, accept)
        ).decodeAs<String>()

    suspend fun cancelFriendRequest(targetUserId: String): Boolean =
        client.postgrest.rpc(
            "face_video_cancel_friend_request",
            FriendTargetArguments(targetUserId)
        ).decodeAs<Boolean>()

    suspend fun removeFriend(targetUserId: String): Boolean =
        client.postgrest.rpc(
            "face_video_remove_friend",
            FriendTargetArguments(targetUserId)
        ).decodeAs<Boolean>()

    /**
     * Story rows are filtered by RLS: only the author and their accepted friends
     * can receive them, and expired entries are never returned.
     */
    suspend fun listVisibleStories(): List<FaceVideoStoryCard> {
        val rows = client.from("face_video_stories").select {
            order("created_at", Order.DESCENDING)
        }.decodeList<FaceVideoStory>()
        if (rows.isEmpty()) return emptyList()

        val profileCache = mutableMapOf<String, FaceVideoProfile?>()
        return rows.mapNotNull { story ->
            val profile = profileCache.getOrPut(story.userId) { getProfile(story.userId) } ?: return@mapNotNull null
            val signedUrl = client.storage.from(STORIES_BUCKET).createSignedUrl(
                path = story.mediaPath,
                expiresIn = 3.minutes
            )
            FaceVideoStoryCard(
                storyId = story.id,
                userId = story.userId,
                username = profile.username,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                mediaUrl = signedUrl,
                caption = story.caption,
                createdAt = story.createdAt,
                expiresAt = story.expiresAt
            )
        }
    }

    /** Publishes a JPEG photo story. The database enforces a 24-hour lifetime. */
    suspend fun publishStory(userId: String, bytes: ByteArray, caption: String): FaceVideoStoryCard {
        require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) {
            "Фото для истории должно быть не больше 10 МБ."
        }
        val cleanCaption = caption.trim().take(500)
        val storyId = UUID.randomUUID().toString()
        val path = "$userId/$storyId.jpg"
        client.storage.from(STORIES_BUCKET).upload(path = path, data = bytes) {
            contentType = ContentType.Image.JPEG
        }

        val row = client.from("face_video_stories").insert(
            NewFaceVideoStory(
                id = storyId,
                userId = userId,
                mediaPath = path,
                caption = cleanCaption,
                // Trigger overwrites this with server time + 24 hours.
                expiresAt = java.time.Instant.now().plusSeconds(24 * 60 * 60).toString()
            )
        ) { select() }.decodeSingle<FaceVideoStory>()

        val profile = getProfile(userId) ?: throw IllegalStateException("Профиль пользователя не найден.")
        val signedUrl = client.storage.from(STORIES_BUCKET).createSignedUrl(
            path = row.mediaPath,
            expiresIn = 3.minutes
        )
        return FaceVideoStoryCard(
            storyId = row.id,
            userId = userId,
            username = profile.username,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            mediaUrl = signedUrl,
            caption = row.caption,
            createdAt = row.createdAt,
            expiresAt = row.expiresAt
        )
    }

    suspend fun deleteStory(storyId: String, mediaPath: String) {
        client.from("face_video_stories").delete {
            filter { eq("id", storyId) }
        }
        client.storage.from(STORIES_BUCKET).delete(mediaPath)
    }

    suspend fun updateMyProfile(
        userId: String,
        username: String,
        displayName: String,
        statusText: String = "",
        about: String = "",
        avatarUrl: String? = null
    ): FaceVideoProfile {
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        val cleanDisplayName = displayName.trim()
        val cleanStatusText = statusText.trim()
        val cleanAbout = about.trim()
        require(cleanUsername.matches(Regex("^[a-z0-9_]{3,24}$"))) {
            "Псевдоним должен содержать 3–24 символа: латинские буквы, цифры или _."
        }
        require(cleanDisplayName.length <= 48) {
            "Имя должно содержать не более 48 символов."
        }
        require(cleanStatusText.length <= 80) { "Статус должен содержать не более 80 символов." }
        require(cleanAbout.length <= 500) { "Раздел «О себе» должен содержать не более 500 символов." }
        val existing = getMyProfile(userId)
        return client.from("face_video_profiles")
            .update(UpdateFaceVideoProfile(
                username = cleanUsername,
                displayName = cleanDisplayName,
                statusText = cleanStatusText,
                about = cleanAbout,
                avatarUrl = avatarUrl ?: existing?.avatarUrl
            )) {
                select()
                filter { eq("id", userId) }
            }
            .decodeSingle<FaceVideoProfile>()
    }
    /** Uploads a profile avatar to the user's own Storage folder and returns its public URL. */
    suspend fun uploadProfileAvatar(userId: String, bytes: ByteArray, mimeType: String = "image/jpeg"): String {
        require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) {
            "Фото профиля должно быть не больше 5 МБ."
        }
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp")) {
            "Поддерживаются JPEG, PNG и WebP."
        }
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val path = "$userId/avatar.$extension"
        client.storage.from("arturchat-avatars").upload(path = path, data = bytes) {
            contentType = ContentType.parse(mimeType)
            upsert = true
        }
        return client.storage.from("arturchat-avatars").publicUrl(path)
    }

    /**
     * Loads chat summaries and enriches direct chats with the other participant's
     * profile photo. Two batched queries avoid an N+1 request per chat row.
     */
    /** Save the current user's latest online heartbeat without depending on Realtime. */
    suspend fun heartbeatOnlinePresence(userId: String) {
        client.from("face_video_online_presence").insert(
            FaceVideoOnlinePresenceRow(
                userId = userId,
                lastSeenAt = java.time.Instant.now().toString()
            ),
            upsert = true,
            onConflict = "user_id"
        )
    }

    /** Users with a heartbeat in the last 45 seconds are treated as online. */
    suspend fun listOnlineUserIds(): Set<String> {
        val cutoff = java.time.Instant.now().minusSeconds(45).toString()
        return client.from("face_video_online_presence").select {
            filter { gt("last_seen_at", cutoff) }
        }.decodeList<FaceVideoOnlinePresenceRow>()
            .mapTo(mutableSetOf()) { it.userId }
    }

    suspend fun listMyChats(currentUserId: String): List<FaceVideoChatSummary> {
        val chats = client.from("face_video_chat_summaries")
            .select()
            .decodeList<FaceVideoChatSummary>()
        val directChatIds = chats.filterNot { it.isGroup }.map { it.id }.distinct()
        if (directChatIds.isEmpty()) return chats

        val memberships = client.from("face_video_chat_members").select {
            filter { isIn("chat_id", directChatIds) }
        }.decodeList<FaceVideoChatAvatarMember>()
        val peerByChat = memberships.groupBy { it.chatId }.mapNotNull { (chatId, rows) ->
            rows.firstOrNull { it.userId != currentUserId }?.userId?.let { chatId to it }
        }.toMap()
        val peerIds = peerByChat.values.distinct()
        if (peerIds.isEmpty()) return chats

        val profiles = client.from("face_video_profiles").select {
            filter { isIn("id", peerIds) }
        }.decodeList<FaceVideoProfile>().associateBy { it.id }

        return chats.map { chat ->
            val peerId = peerByChat[chat.id]
            chat.copy(
                peerUserId = peerId,
                peerAvatarUrl = peerId?.let { profiles[it]?.avatarUrl }
            )
        }
    }

    suspend fun getChat(chatId: String): FaceVideoChatSummary? =
        client.from("face_video_chat_summaries")
            .select {
                filter { eq("id", chatId) }
            }
            .decodeList<FaceVideoChatSummary>()
            .firstOrNull()

    /** Returns the other participant of a direct chat; RLS limits rows to chat members. */
    suspend fun getDirectChatPeerId(chatId: String, currentUserId: String): String? =
        client.from("face_video_chat_members")
            .select {
                filter { eq("chat_id", chatId) }
            }
            .decodeList<FaceVideoChatMemberUserId>()
            .firstOrNull { it.userId != currentUserId }
            ?.userId

    suspend fun findUser(username: String): FaceVideoProfile? {
        val clean = username.trim().removePrefix("@").lowercase()
        if (!clean.matches(Regex("^[a-z0-9_]{3,24}$"))) return null
        return client.from("face_video_profiles")
            .select {
                filter { eq("username", clean) }
            }
            .decodeList<FaceVideoProfile>()
            .firstOrNull()
    }

    /** Reuse an existing direct conversation before creating a new one. */
    suspend fun getOrCreateDirectChat(
        otherUserId: String,
        currentUserId: String
    ): FaceVideoChatSummary? {
        val existing = listMyChats(currentUserId).firstOrNull { chat ->
            !chat.isGroup && getDirectChatPeerId(chat.id, currentUserId) == otherUserId
        }
        if (existing != null) return existing
        val chatId = createDirectChat(otherUserId)
        return getChat(chatId)
    }

    suspend fun createDirectChat(otherUserId: String): String {
        val result = client.postgrest.rpc(
            "face_video_create_chat",
            CreateChatArguments(memberIds = listOf(otherUserId))
        )
        return result.decodeAs<String>()
    }

    suspend fun listMessages(chatId: String): List<FaceVideoMessage> =
        client.from("face_video_messages")
            .select {
                filter { eq("chat_id", chatId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<FaceVideoMessage>()

    suspend fun hideMessagesForMe(messageIds: List<String>, userId: String) {
        messageIds.distinct().forEach { id ->
            client.from("face_video_hidden_messages").insert(
                HiddenMessage(userId = userId, messageId = id)
            )
        }
    }

    suspend fun hideChatForMe(chatId: String, userId: String) {
        client.from("face_video_hidden_chats").insert(
            HiddenChat(userId = userId, chatId = chatId)
        )
    }

    suspend fun deleteMessageForEveryone(messageId: String, userId: String) {
        client.from("face_video_messages").update(
            DeletedMessage(
                deletedAt = java.time.Instant.now().toString(),
                deletedBy = userId,
                body = "Это сообщение удалено"
            )
        ) {
            filter { eq("id", messageId) }
        }
    }

    suspend fun forwardMessage(
        source: FaceVideoMessage,
        targetChatId: String,
        senderId: String,
        sourceName: String
    ): FaceVideoMessage {
        if (source.deletedAt != null) throw IllegalArgumentException("Удалённое сообщение нельзя переслать.")
        if (source.attachmentPath != null) {
            val bytes = downloadChatAttachment(source.attachmentPath)
            val newPath = uploadChatImage(targetChatId, senderId, bytes)
            return client.from("face_video_messages").insert(
                NewMessage(
                    chatId = targetChatId,
                    body = source.body,
                    attachmentPath = newPath,
                    attachmentMimeType = source.attachmentMimeType,
                    attachmentFileName = source.attachmentFileName,
                    attachmentSize = source.attachmentSize,
                    forwardedFromSenderId = source.senderId,
                    forwardedFromName = sourceName
                )
            ) { select() }.decodeSingle<FaceVideoMessage>()
        }
        return client.from("face_video_messages").insert(
            NewMessage(
                chatId = targetChatId,
                body = source.body,
                forwardedFromSenderId = source.senderId,
                forwardedFromName = sourceName
            )
        ) { select() }.decodeSingle<FaceVideoMessage>()
    }

    /** Returns unread counts and the newest incoming unread message for every chat. */
    suspend fun getUnreadSummaries(): Map<String, FaceVideoChatUnreadSummary> {
        val rows = client.postgrest.rpc("face_video_get_unread_summaries")
            .decodeList<FaceVideoChatUnreadSummary>()
        return rows.associateBy { it.chatId }
    }

    /** Marks newly received messages as delivered for this signed-in recipient. */
    suspend fun markMessagesDelivered(messageIds: List<String>) {
        messageIds.distinct().chunked(100).forEach { batch ->
            if (batch.isNotEmpty()) {
                client.postgrest.rpc(
                    "face_video_mark_messages_delivered",
                    MarkMessagesDeliveredArguments(batch)
                )
            }
        }
    }

    /** Advances the current user's read watermark and receipt state for this chat. */
    suspend fun markChatRead(chatId: String) {
        client.postgrest.rpc(
            "face_video_mark_chat_read",
            MarkChatReadArguments(chatId)
        )
    }

    /**
     * Inserts a message and returns the persisted row. The caller can render this
     * immediately, without waiting for the Realtime subscription to echo the INSERT.
     */
    suspend fun sendText(
        chatId: String,
        text: String,
        replyToMessageId: String? = null
    ): FaceVideoMessage {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty() && cleanText.length <= 4000) {
            "Сообщение должно содержать от 1 до 4000 символов."
        }
        return client.from("face_video_messages")
            .insert(
                NewMessage(
                    chatId = chatId,
                    body = cleanText,
                    replyToMessageId = replyToMessageId
                )
            ) {
                select()
            }
            .decodeSingle<FaceVideoMessage>()
    }

    /**
     * Uploads an image to a private conversation bucket.
     * The storage RLS policy checks both the conversation membership and the sender folder.
     * Images are normalized to JPEG on the device before they reach this method.
     */
    suspend fun uploadChatImage(
        chatId: String,
        senderId: String,
        bytes: ByteArray
    ): String {
        require(bytes.isNotEmpty() && bytes.size <= MAX_CHAT_IMAGE_BYTES) {
            "Фото должно быть не больше 10 МБ."
        }
        val path = "$chatId/$senderId/${UUID.randomUUID()}.jpg"
        client.storage.from(CHAT_ATTACHMENTS_BUCKET).upload(
            path = path,
            data = bytes
        ) {
            contentType = ContentType.Image.JPEG
        }
        return path
    }

    /** Inserts the image message only after its file has been uploaded successfully. */
    suspend fun sendImage(
        chatId: String,
        imagePath: String,
        fileName: String,
        fileSize: Long,
        replyToMessageId: String? = null
    ): FaceVideoMessage {
        require(imagePath.startsWith("$chatId/")) {
            "Вложение не относится к этой переписке."
        }
        require(fileSize in 1..MAX_CHAT_IMAGE_BYTES.toLong()) {
            "Фото должно быть не больше 10 МБ."
        }
        val safeName = fileName.trim().take(255).ifBlank { "photo.jpg" }
        return client.from("face_video_messages")
            .insert(
                NewMessage(
                    chatId = chatId,
                    body = "[Фото]",
                    replyToMessageId = replyToMessageId,
                    attachmentPath = imagePath,
                    attachmentMimeType = "image/jpeg",
                    attachmentFileName = safeName,
                    attachmentSize = fileSize
                )
            ) {
                select()
            }
            .decodeSingle<FaceVideoMessage>()
    }

    /** Downloads a private attachment using the current signed-in user's JWT. */
    suspend fun downloadChatAttachment(path: String): ByteArray {
        require(path.split('/').size == 3) { "Некорректный путь вложения." }
        return client.storage.from(CHAT_ATTACHMENTS_BUCKET).downloadAuthenticated(path)
    }

    @OptIn(SupabaseExperimental::class)
    fun observeMessages(chatId: String): Flow<List<FaceVideoMessage>> =
        client.from("face_video_messages").selectAsFlow(FaceVideoMessage::id) {
            eq("chat_id", chatId)
        }

    /** Live delivery/read receipts visible to the sender and recipient. */
    @OptIn(SupabaseExperimental::class)
    fun observeMessageReceipts(chatId: String): Flow<List<FaceVideoMessageReceipt>> =
        client.from("face_video_message_receipts").selectAsFlow(FaceVideoMessageReceipt::id) {
            eq("chat_id", chatId)
        }

    /**
     * Realtime feed for all messages visible to the signed-in user.
     * Row Level Security limits this feed to chats the user belongs to.
     */
    @OptIn(SupabaseExperimental::class)
    fun observeMyMessages(): Flow<List<FaceVideoMessage>> =
        client.from("face_video_messages").selectAsFlow(FaceVideoMessage::id) {}

    suspend fun registerPushToken(userId: String, token: String) {
        require(token.length in 20..4096) { "Недопустимый FCM-токен." }
        val existing = client.from("face_video_push_tokens").select {
            filter {
                eq("user_id", userId)
                eq("token", token)
            }
        }.decodeList<NewFaceVideoPushToken>()
        if (existing.isEmpty()) {
            client.from("face_video_push_tokens").insert(
                NewFaceVideoPushToken(userId = userId, token = token)
            )
        }
    }

    suspend fun removePushToken(userId: String, token: String) {
        client.from("face_video_push_tokens").delete {
            filter {
                eq("user_id", userId)
                eq("token", token)
            }
        }
    }
}


private const val CHAT_ATTACHMENTS_BUCKET = "chat-attachments"
private const val MAX_CHAT_IMAGE_BYTES = 10 * 1024 * 1024
