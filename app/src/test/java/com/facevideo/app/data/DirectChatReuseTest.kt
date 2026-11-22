package com.facevideo.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectChatReuseTest {
    private fun chat(
        id: String,
        peerId: String = "peer",
        isGroup: Boolean = false,
        createdAt: String,
        lastMessageAt: String? = null
    ) = FaceVideoChatSummary(
        id = id,
        title = id,
        isGroup = isGroup,
        createdAt = createdAt,
        lastMessageAt = lastMessageAt,
        peerUserId = peerId
    )

    @Test
    fun selectsExistingDirectChatForTheRequestedPeer() {
        val existing = chat(
            id = "existing",
            createdAt = "2026-11-01T10:00:00Z",
            lastMessageAt = "2026-11-20T10:00:00Z"
        )
        assertEquals(existing, findExistingDirectChat(listOf(existing), "peer"))
    }

    @Test
    fun prefersConversationWithHistoryOverNewEmptyDuplicate() {
        val established = chat(
            id = "established",
            createdAt = "2026-11-01T10:00:00Z",
            lastMessageAt = "2026-11-20T10:00:00Z"
        )
        val emptyDuplicate = chat(
            id = "empty",
            createdAt = "2026-11-22T10:00:00Z"
        )
        assertEquals(
            established,
            findExistingDirectChat(listOf(emptyDuplicate, established), "peer")
        )
    }

    @Test
    fun choosesMostRecentlyActiveChatWhenSeveralHaveMessages() {
        val older = chat(
            id = "older",
            createdAt = "2026-11-01T10:00:00Z",
            lastMessageAt = "2026-11-19T10:00:00Z"
        )
        val newer = chat(
            id = "newer",
            createdAt = "2026-11-02T10:00:00Z",
            lastMessageAt = "2026-11-21T10:00:00Z"
        )
        assertEquals(newer, findExistingDirectChat(listOf(older, newer), "peer"))
    }

    @Test
    fun ignoresGroupsAndChatsWithAnotherPeer() {
        val group = chat(
            id = "group",
            isGroup = true,
            createdAt = "2026-11-22T10:00:00Z"
        )
        val otherPeer = chat(
            id = "other",
            peerId = "someone-else",
            createdAt = "2026-11-22T10:00:00Z"
        )
        assertNull(findExistingDirectChat(listOf(group, otherPeer), "peer"))
    }

    @Test
    fun choosesNewestEmptyChatWhenNoConversationHasMessages() {
        val older = chat(id = "older", createdAt = "2026-11-20T10:00:00Z")
        val newer = chat(id = "newer", createdAt = "2026-11-22T10:00:00Z")
        assertEquals(newer, findExistingDirectChat(listOf(older, newer), "peer"))
    }
}
