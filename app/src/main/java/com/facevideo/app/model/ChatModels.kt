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

data class ChatMessage(
    val id: String,
    val text: String,
    val time: String,
    val isMine: Boolean
)

fun filterChats(chats: List<ChatSummary>, query: String): List<ChatSummary> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return chats
    return chats.filter {
        it.title.contains(normalized, ignoreCase = true) ||
            it.preview.contains(normalized, ignoreCase = true)
    }
}
