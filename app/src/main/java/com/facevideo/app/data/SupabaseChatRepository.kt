package com.facevideo.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FaceVideoChatSummary(
    val id: String,
    val title: String,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("last_message_preview") val lastMessagePreview: String = "",
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FaceVideoProfile(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
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
private data class DeletedMessage(@SerialName("deleted_at") val deletedAt: String, @SerialName("deleted_by") val deletedBy: String, val body: String)

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
    @SerialName("display_name") val displayName: String
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

    suspend fun getMyProfile(userId: String): FaceVideoProfile? =
        client.from("face_video_profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeList<FaceVideoProfile>()
            .firstOrNull()

    suspend fun updateMyProfile(
        userId: String,
        username: String,
        displayName: String
    ): FaceVideoProfile {
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        val cleanDisplayName = displayName.trim()
        require(cleanUsername.matches(Regex("^[a-z0-9_]{3,24}$"))) {
            "Псевдоним должен содержать 3–24 символа: латинские буквы, цифры или _."
        }
        require(cleanDisplayName.length <= 48) {
            "Имя должно содержать не более 48 символов."
        }
        return client.from("face_video_profiles")
            .update(UpdateFaceVideoProfile(username = cleanUsername, displayName = cleanDisplayName)) {
                select()
                filter { eq("id", userId) }
            }
            .decodeSingle<FaceVideoProfile>()
    }
    suspend fun listMyChats(): List<FaceVideoChatSummary> =
        client.from("face_video_chat_summaries")
            .select()
            .decodeList<FaceVideoChatSummary>()

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

    suspend fun hideMessagesForMe(messageIds: List<String>) {
        messageIds.distinct().forEach { id ->
            client.from("face_video_hidden_messages").upsert(
                HiddenMessage(userId = currentUserId(), messageId = id)
            )
        }
    }

    suspend fun hideChatForMe(chatId: String) {
        client.from("face_video_hidden_chats").upsert(
            HiddenChat(userId = currentUserId(), chatId = chatId)
        )
    }

    suspend fun deleteMessageForEveryone(messageId: String) {
        client.from("face_video_messages").update(
            DeletedMessage(deletedAt = java.time.Instant.now().toString(), deletedBy = currentUserId(), body = "Это сообщение удалено")
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

    private fun currentUserId(): String =
        client.auth.currentUserOrNull()?.id ?: error("Необходима авторизация.")

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
        )
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
