package com.facevideo.app.model

/** Reactions supported for chat messages. The database validates this same allow-list. */
val MESSAGE_REACTION_OPTIONS: List<String> = listOf("👍", "❤️", "😂", "😮", "😢", "🔥", "👏", "👎")

data class MessageReactionEntry(
    val userId: String,
    val reaction: String
)

data class MessageReactionCount(
    val emoji: String,
    val count: Int,
    val selectedByMe: Boolean
)

data class MessageReactionSummary(
    val items: List<MessageReactionCount> = emptyList()
)

/** Aggregates one reaction per participant into Telegram-style emoji counters. */
fun summarizeMessageReactions(
    entries: List<MessageReactionEntry>,
    currentUserId: String
): MessageReactionSummary {
    val validEntries = entries.filter { it.reaction in MESSAGE_REACTION_OPTIONS }
    val items = MESSAGE_REACTION_OPTIONS.mapNotNull { emoji ->
        val matching = validEntries.filter { it.reaction == emoji }
        if (matching.isEmpty()) null else MessageReactionCount(
            emoji = emoji,
            count = matching.size,
            selectedByMe = matching.any { it.userId == currentUserId }
        )
    }
    return MessageReactionSummary(items)
}
