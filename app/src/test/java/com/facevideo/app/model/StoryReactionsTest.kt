package com.facevideo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoryReactionsTest {
    @Test
    fun summaryCountsEachReactionAndReturnsCurrentUsersChoice() {
        val summary = summarizeStoryReactions(
            listOf(
                StoryReactionEntry("friend-1", "❤️"),
                StoryReactionEntry("friend-2", "❤️"),
                StoryReactionEntry("friend-3", "😂"),
                StoryReactionEntry("me", "🔥")
            ),
            currentUserId = "me"
        )

        assertEquals(2, summary.counts["❤️"])
        assertEquals(1, summary.counts["😂"])
        assertEquals(1, summary.counts["🔥"])
        assertEquals(4, summary.totalCount)
        assertEquals("🔥", summary.currentUserReaction)
    }

    @Test
    fun unsupportedReactionRowsAreIgnored() {
        val summary = summarizeStoryReactions(
            listOf(
                StoryReactionEntry("friend-1", "❤️"),
                StoryReactionEntry("friend-2", "🚀")
            ),
            currentUserId = "me"
        )

        assertEquals(mapOf("❤️" to 1), summary.counts)
        assertEquals(1, summary.totalCount)
        assertNull(summary.currentUserReaction)
    }

    @Test
    fun tappingSameReactionRemovesIt() {
        assertNull(nextStoryReaction("❤️", "❤️"))
    }

    @Test
    fun tappingAnotherReactionReplacesCurrentChoice() {
        assertEquals("🔥", nextStoryReaction("❤️", "🔥"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedReactionCannotBeSelected() {
        nextStoryReaction(null, "🚀")
    }
}
