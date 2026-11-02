package com.facevideo.app.data

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageFormattingTest {
    private fun message(id: String, body: String, createdAt: String) = FaceVideoMessage(
        id = id,
        chatId = "chat-1",
        senderId = "user-1",
        body = body,
        createdAt = createdAt
    )

    @Test
    fun messagesAreSortedByTimestampNotPrimaryKeyOrBody() {
        val newerNumeric = message(
            id = "0001",
            body = "123456",
            createdAt = "2026-11-02T10:02:00.000Z"
        )
        val older = message(
            id = "ffff",
            body = "Привет",
            createdAt = "2026-11-02T10:01:00.000Z"
        )
        val middle = message(
            id = "0002",
            body = "Hello",
            createdAt = "2026-11-02T10:01:30.000Z"
        )

        val sorted = sortChatMessagesChronologically(listOf(newerNumeric, older, middle))

        assertEquals(listOf("ffff", "0002", "0001"), sorted.map { it.id })
    }

    @Test
    fun messagesWithSameTimestampKeepTheirIncomingOrder() {
        val first = message("b", "First", "2026-11-02T10:01:00Z")
        val second = message("a", "Second", "2026-11-02T10:01:00Z")

        assertEquals(
            listOf("b", "a"),
            sortChatMessagesChronologically(listOf(first, second)).map { it.id }
        )
    }

    @Test
    fun messageTimeIsFormattedInRequestedLocalZone() {
        assertEquals(
            "12:45",
            formatChatMessageTime("2026-11-02T12:45:33.123Z", ZoneOffset.UTC)
        )
        assertEquals(
            "13:45",
            formatChatMessageTime("2026-11-02T12:45:33.123Z", java.time.ZoneId.of("Europe/Berlin"))
        )
    }
}
