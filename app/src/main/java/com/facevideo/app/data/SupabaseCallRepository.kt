package com.facevideo.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FaceVideoCall(
    val id: String,
    @SerialName("caller_id") val callerId: String,
    @SerialName("callee_id") val calleeId: String,
    @SerialName("media_type") val mediaType: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("answered_at") val answeredAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null
)

@Serializable
data class FaceVideoCallSignal(
    val id: String,
    @SerialName("call_id") val callId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("signal_type") val signalType: String,
    val payload: JsonObject,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class FaceVideoChatMember(
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class NewFaceVideoCall(
    @SerialName("caller_id") val callerId: String,
    @SerialName("callee_id") val calleeId: String,
    @SerialName("media_type") val mediaType: String
)

@Serializable
private data class NewFaceVideoCallSignal(
    @SerialName("call_id") val callId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("signal_type") val signalType: String,
    val payload: JsonObject
)

@Serializable
private data class FaceVideoCallStatusUpdate(val status: String)

/**
 * User-scoped call and signaling operations. RLS ensures that only the two
 * participants can see a call and that signaling payloads go only to the peer.
 */
class SupabaseCallRepository(private val client: SupabaseClient) {

    suspend fun currentAccessToken(): String =
        client.auth.currentSessionOrNull()?.accessToken
            ?: throw IllegalStateException("Сессия ArturChat не авторизована.")

    suspend fun startDirectCall(
        chatId: String,
        callerId: String,
        mediaType: String
    ): FaceVideoCall {
        require(mediaType == "audio" || mediaType == "video") {
            "Неизвестный тип звонка."
        }

        val chat = client.from("face_video_chat_summaries")
            .select {
                filter { eq("id", chatId) }
            }
            .decodeList<FaceVideoChatSummary>()
            .firstOrNull()
            ?: throw IllegalStateException("Переписка больше недоступна.")

        require(!chat.isGroup) {
            "Групповые звонки пока не поддерживаются."
        }

        val members = client.from("face_video_chat_members")
            .select {
                filter { eq("chat_id", chatId) }
            }
            .decodeList<FaceVideoChatMember>()

        val peerId = members.singleOrNull { it.userId != callerId }?.userId
            ?: throw IllegalStateException("Не удалось определить собеседника.")

        return client.from("face_video_calls")
            .insert(NewFaceVideoCall(callerId, peerId, mediaType)) {
                select()
            }
            .decodeSingle<FaceVideoCall>()
    }

    suspend fun getProfile(userId: String): FaceVideoProfile? =
        client.from("face_video_profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeList<FaceVideoProfile>()
            .firstOrNull()

    suspend fun answerCall(callId: String) {
        updateStatus(callId, "accepted")
    }

    suspend fun declineCall(callId: String) {
        updateStatus(callId, "declined")
    }

    suspend fun cancelCall(callId: String) {
        updateStatus(callId, "cancelled")
    }

    suspend fun endCall(callId: String) {
        updateStatus(callId, "ended")
    }

    private suspend fun updateStatus(callId: String, status: String) {
        client.from("face_video_calls").update(FaceVideoCallStatusUpdate(status)) {
            filter { eq("id", callId) }
        }
    }

    suspend fun sendSignal(
        callId: String,
        senderId: String,
        recipientId: String,
        signalType: String,
        payload: JsonObject
    ) {
        require(signalType in setOf("offer", "answer", "ice")) {
            "Неизвестный тип сигнала."
        }
        client.from("face_video_call_signals").insert(
            NewFaceVideoCallSignal(
                callId = callId,
                senderId = senderId,
                recipientId = recipientId,
                signalType = signalType,
                payload = payload
            )
        )
    }

    suspend fun listSignals(callId: String, recipientId: String): List<FaceVideoCallSignal> =
        client.from("face_video_call_signals")
            .select {
                filter {
                    eq("call_id", callId)
                    eq("recipient_id", recipientId)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<FaceVideoCallSignal>()

    @OptIn(SupabaseExperimental::class)
    fun observeIncomingCalls(userId: String): Flow<List<FaceVideoCall>> =
        client.from("face_video_calls").selectAsFlow(FaceVideoCall::id) {
            eq("callee_id", userId)
            eq("status", "ringing")
        }

    @OptIn(SupabaseExperimental::class)
    fun observeSignals(callId: String, recipientId: String): Flow<List<FaceVideoCallSignal>> =
        client.from("face_video_call_signals").selectAsFlow(FaceVideoCallSignal::id) {
            eq("call_id", callId)
            eq("recipient_id", recipientId)
        }

    @OptIn(SupabaseExperimental::class)
    fun observeCall(callId: String): Flow<List<FaceVideoCall>> =
        client.from("face_video_calls").selectAsFlow(FaceVideoCall::id) {
            eq("id", callId)
        }
}
