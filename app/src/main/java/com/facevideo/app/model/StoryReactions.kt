package com.facevideo.app.model

/**
 * Quick reactions supported by story viewers.
 * The same list is used to validate client actions and render reaction chips.
 */
val STORY_REACTION_OPTIONS: List<String> = listOf("❤️", "😂", "😮", "😢", "🔥", "👏")

data class StoryReactionEntry(
    val userId: String,
    val reaction: String
)

data class StoryReactionSummary(
    val counts: Map<String, Int> = emptyMap(),
    val currentUserReaction: String? = null
) {
    val totalCount: Int
        get() = counts.values.sum()
}

/** Builds per-emoji totals and the current user's selected reaction from visible rows. */
fun summarizeStoryReactions(
    entries: List<StoryReactionEntry>,
    currentUserId: String
): StoryReactionSummary {
    val validEntries = entries.filter { it.reaction in STORY_REACTION_OPTIONS }
    val counts = STORY_REACTION_OPTIONS.mapNotNull { emoji ->
        val count = validEntries.count { it.reaction == emoji }
        if (count > 0) emoji to count else null
    }.toMap()
    val ownReaction = validEntries.firstOrNull { it.userId == currentUserId }?.reaction
    return StoryReactionSummary(counts = counts, currentUserReaction = ownReaction)
}

/** Tapping the currently selected emoji removes it; another emoji replaces it. */
fun nextStoryReaction(currentReaction: String?, tappedReaction: String): String? {
    require(tappedReaction in STORY_REACTION_OPTIONS) { "Неподдерживаемая реакция на историю." }
    return if (currentReaction == tappedReaction) null else tappedReaction
}
