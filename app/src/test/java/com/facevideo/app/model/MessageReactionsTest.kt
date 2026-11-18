package com.facevideo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageReactionsTest {
    @Test
    fun summaryGroupsCountsAndMarksCurrentUsersReaction() {
        val summary = summarizeMessageReactions(
            listOf(
                MessageReactionEntry("message-1", "alice", "❤️"),
                MessageReactionEntry("message-1", "bob", "❤️"),
                MessageReactionEntry("message-1", "me", "🔥"),
                MessageReactionEntry("message-2", "alice", "😂")
            ),
            currentUserId = "me"
        )

        assertEquals(2, summary["message-1"]?.first { it.reaction == "❤️" }?.count)
        assertFalse(summary["message-1"]?.first { it.reaction == "❤️" }?.selectedByMe ?: true)
        assertTrue(summary["message-1"]?.first { it.reaction == "🔥" }?.selectedByMe ?: false)
        assertEquals(1, summary["message-2"]?.first { it.reaction == "😂" }?.count)
        assertEquals(listOf("❤️", "🔥"), summary["message-1"]?.map { it.reaction })
    }

    @Test
    fun emptyAndUnsupportedRowsAreNotRendered() {
        val summary = summarizeMessageReactions(
            listOf(
                MessageReactionEntry("message-1", "alice", "🚀"),
                MessageReactionEntry("message-2", "bob", "👎")
            ),
            currentUserId = "me"
        )

        assertEquals(null, summary["message-1"])
        assertEquals(1, summary["message-2"]?.single()?.count)
    }
}
