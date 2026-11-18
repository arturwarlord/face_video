package com.facevideo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageReactionsTest {
    @Test
    fun aggregatesCountsAndMarksTheCurrentUsersReaction() {
        val summary = summarizeMessageReactions(
            listOf(
                MessageReactionEntry("me", "❤️"),
                MessageReactionEntry("friend-1", "❤️"),
                MessageReactionEntry("friend-2", "😂"),
                MessageReactionEntry("friend-3", "🔥")
            ),
            currentUserId = "me"
        )

        assertEquals(listOf("❤️", "😂", "🔥"), summary.items.map { it.emoji })
        assertEquals(listOf(2, 1, 1), summary.items.map { it.count })
        assertTrue(summary.items.first().selectedByMe)
        assertFalse(summary.items[1].selectedByMe)
    }

    @Test
    fun ignoresReactionsOutsideTheSupportedSet() {
        val summary = summarizeMessageReactions(
            listOf(
                MessageReactionEntry("me", "❤️"),
                MessageReactionEntry("other", "not-an-emoji")
            ),
            currentUserId = "me"
        )
        assertEquals(1, summary.items.size)
        assertEquals("❤️", summary.items.single().emoji)
        assertEquals(1, summary.items.single().count)
    }

    @Test
    fun emptyMessageHasNoReactionChips() {
        assertTrue(
            summarizeMessageReactions(emptyList(), currentUserId = "me").items.isEmpty()
        )
    }
}
