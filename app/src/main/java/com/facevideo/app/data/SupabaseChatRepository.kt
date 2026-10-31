package com.facevideo.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
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
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class CreateChatArguments(
    @SerialName("p_member_ids") val memberIds: List<String>,
    @SerialName("p_title") val title: String? = null,
    @SerialName("p_is_group") val isGroup: Boolean = false
)

@Serializable
private data class NewMessage(
    @SerialName("chat_id") val chatId: String,
    val body: String
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

    suspend fun sendText(chatId: String, text: String) {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty() && cleanText.length <= 4000) {
            "Сообщение должно содержать от 1 до 4000 символов."
        }
        client.from("face_video_messages")
            .insert(NewMessage(chatId = chatId, body = cleanText))
    }

    @OptIn(SupabaseExperimental::class)
    fun observeMessages(chatId: String): Flow<List<FaceVideoMessage>> =
        client.from("face_video_messages").selectAsFlow(FaceVideoMessage::id) {
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
        client.from("face_video_push_tokens").insert(
            NewFaceVideoPushToken(userId = userId, token = token),
            upsert = true,
            onConflict = "user_id,token"
        )
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
