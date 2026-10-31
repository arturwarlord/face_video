package com.facevideo.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSummaryRecord(
    val id: String,
    val title: String? = null,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ChatMessageRecord(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class NewMessage(
    @SerialName("chat_id") val chatId: String,
    val body: String
)

/**
 * Queries and realtime updates are always scoped by Supabase Auth + database RLS.
 * No service_role key is used by this Android client.
 */
class SupabaseChatRepository(
    private val client: io.github.jan.supabase.SupabaseClient
) {
    suspend fun listMyChats(): List<ChatSummaryRecord> =
        client.from("chat_summaries")
            .select()
            .decodeList<ChatSummaryRecord>()

    suspend fun listMessages(chatId: String): List<ChatMessageRecord> =
        client.from("messages")
            .select {
                filter {
                    eq("chat_id", chatId)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<ChatMessageRecord>()

    suspend fun sendText(chatId: String, text: String) {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty() && cleanText.length <= 4000) {
            "Сообщение должно содержать от 1 до 4000 символов."
        }
        client.from("messages").insert(NewMessage(chatId = chatId, body = cleanText))
    }

    /**
     * Emits the initial visible message list and subsequent inserts/updates/deletes.
     * The messages table must be enabled in the supabase_realtime publication.
     */
    @OptIn(SupabaseExperimental::class)
    fun observeMessages(chatId: String): Flow<List<ChatMessageRecord>> =
        client.from("messages").selectAsFlow(ChatMessageRecord::id) {
            eq("chat_id", chatId)
        }
}
