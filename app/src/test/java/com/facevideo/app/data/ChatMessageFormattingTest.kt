package com.facevideo.app.data

import java.time.ZoneOffset
import com.facevideo.app.model.MessageDeliveryStatus
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
    fun unreadBadgeIsHiddenForZeroAndNegativeCounts() {
        assertEquals(null, formatUnreadBadge(0))
        assertEquals(null, formatUnreadBadge(-2))
    }

    @Test
    fun unreadBadgeShowsCountsAndCapsAtNinetyNinePlus() {
        assertEquals("1", formatUnreadBadge(1))
        assertEquals("12", formatUnreadBadge(12))
        assertEquals("99", formatUnreadBadge(99))
        assertEquals("99+", formatUnreadBadge(100))
    }

    private fun receipt(
        id: String,
        messageId: String,
        deliveredAt: String? = null,
        readAt: String? = null,
        recipientId: String = "recipient-$id"
    ) = FaceVideoMessageReceipt(
        id = id,
        messageId = messageId,
        chatId = "chat-1",
        senderId = "sender-1",
        recipientId = recipientId,
        deliveredAt = deliveredAt,
        readAt = readAt
    )

    @Test
    fun outgoingMessageStartsAsSentUntilRecipientReceiptArrives() {
        assertEquals(
            MessageDeliveryStatus.SENT,
            resolveMessageDeliveryStatus("message-1", emptyList())
        )
    }

    @Test
    fun outgoingMessageBecomesDeliveredAfterRecipientDeliveryReceipt() {
        val receipts = listOf(
            receipt(
                id = "r1",
                messageId = "message-1",
                deliveredAt = "2026-11-03T09:00:00Z"
            )
        )

        assertEquals(
            MessageDeliveryStatus.DELIVERED,
            resolveMessageDeliveryStatus("message-1", receipts)
        )
    }

    @Test
    fun outgoingMessageBecomesReadAfterRecipientReadReceipt() {
        val receipts = listOf(
            receipt(
                id = "r1",
                messageId = "message-1",
                deliveredAt = "2026-11-03T09:00:00Z",
                readAt = "2026-11-03T09:02:00Z"
            )
        )

        assertEquals(
            MessageDeliveryStatus.READ,
            resolveMessageDeliveryStatus("message-1", receipts)
        )
    }

    @Test
    fun groupMessageRequiresEveryRecipientBeforeShowingDeliveredOrRead() {
        val receipts = listOf(
            receipt("r1", "message-1", deliveredAt = "2026-11-03T09:00:00Z", readAt = "2026-11-03T09:02:00Z"),
            receipt("r2", "message-1", deliveredAt = "2026-11-03T09:01:00Z")
        )

        assertEquals(
            MessageDeliveryStatus.DELIVERED,
            resolveMessageDeliveryStatus("message-1", receipts)
        )
        assertEquals(
            MessageDeliveryStatus.SENT,
            resolveMessageDeliveryStatus("message-1", receipts.map { it.copy(deliveredAt = null) })
        )
    }

    @Test
    fun inboxPreviewPrefersNewestUnreadIncomingMessageOverLaterOwnMessage() {
        val preview = buildChatInboxPreview(
            latestMessagePreview = "My later outgoing message",
            latestMessageAt = "2026-11-03T10:05:00Z",
            unreadSummary = FaceVideoChatUnreadSummary(
                chatId = "chat-1",
                unreadCount = 2,
                lastUnreadBody = "Newest unread incoming message",
                lastUnreadAt = "2026-11-03T10:03:00Z"
            )
        )

        assertEquals("Newest unread incoming message", preview.text)
        assertEquals("2026-11-03T10:03:00Z", preview.timestamp)
    }

    @Test
    fun inboxPreviewUsesLatestMessageWhenNoUnreadMessagesExist() {
        val preview = buildChatInboxPreview(
            latestMessagePreview = "My latest outgoing message",
            latestMessageAt = "2026-11-03T10:05:00Z",
            unreadSummary = FaceVideoChatUnreadSummary(
                chatId = "chat-1",
                unreadCount = 0,
                lastUnreadBody = null,
                lastUnreadAt = null
            )
        )

        assertEquals("My latest outgoing message", preview.text)
        assertEquals("2026-11-03T10:05:00Z", preview.timestamp)
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
