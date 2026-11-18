package com.facevideo.app.model

/** Quick reactions available on chat messages. */
val MESSAGE_REACTION_OPTIONS: List<String> =
    listOf("👍", "❤️", "😂", "😮", "😢", "🔥", "👏", "👎")

data class MessageReactionEntry(
    val messageId: String,
    val userId: String,
    val reaction: String
)

data class MessageReactionCount(
    val reaction: String,
    val count: Int,
    val selectedByMe: Boolean
)

/**
 * Groups visible reaction rows by message and emoji.
 * Rows with unsupported emoji values are ignored defensively.
 */
fun summarizeMessageReactions(
    entries: List<MessageReactionEntry>,
    currentUserId: String
): Map<String, List<MessageReactionCount>> {
    return entries
        .filter { it.reaction in MESSAGE_REACTION_OPTIONS }
        .groupBy { it.messageId }
        .mapValues { (_, messageEntries) ->
            MESSAGE_REACTION_OPTIONS.mapNotNull { emoji ->
                val matching = messageEntries.filter { it.reaction == emoji }
                if (matching.isEmpty()) {
                    null
                } else {
                    MessageReactionCount(
                        reaction = emoji,
                        count = matching.size,
                        selectedByMe = matching.any { it.userId == currentUserId }
                    )
                }
            }
        }
}
