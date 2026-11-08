package com.facevideo.app.model

data class ChatSummary(
    val id: String,
    val title: String,
    val preview: String,
    val time: String,
    val unreadCount: Int = 0,
    val online: Boolean = false,
    val initials: String,
    val avatarStyle: Int = 0
)

enum class MessageDeliveryStatus {
    SENT,
    DELIVERED,
    READ
}

data class ChatMessage(
    val id: String,
    val text: String,
    val time: String,
    val isMine: Boolean,
    val deliveryStatus: MessageDeliveryStatus? = null,
    val forwardedFromName: String? = null,
    val isDeleted: Boolean = false,
    val attachmentMimeType: String? = null,
    val attachmentDurationMs: Long? = null
)

fun filterChats(chats: List<ChatSummary>, query: String): List<ChatSummary> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return chats
    return chats.filter {
        it.title.contains(normalized, ignoreCase = true) ||
            it.preview.contains(normalized, ignoreCase = true)
    }
}

fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
}
