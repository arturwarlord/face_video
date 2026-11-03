package com.facevideo.app.data

import java.time.Instant
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Supabase Realtime snapshots are not guaranteed to arrive in chronological
 * order. Sort by the persisted created_at timestamp, keeping source order for
 * messages that share the same timestamp.
 */
fun sortChatMessagesChronologically(messages: List<FaceVideoMessage>): List<FaceVideoMessage> =
    messages.withIndex()
        .sortedWith(
            compareBy<IndexedValue<FaceVideoMessage>>(
                { parseMessageInstant(it.value.createdAt) ?: Instant.MIN },
                { it.index }
            )
        )
        .map { it.value }

/** Formats a Postgres/Supabase ISO timestamp in the phone's local time zone. */
fun formatChatMessageTime(
    createdAt: String,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val instant = parseMessageInstant(createdAt)
        ?: return createdAt.substringAfter('T', createdAt).take(5)

    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(zoneId)
        .format(instant)
}

private fun parseMessageInstant(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(value) }.getOrNull()


/**
 * Show a timestamp only at the end of a same-sender burst.
 * Consecutive messages from one sender within ten minutes share one timestamp.
 */
fun shouldShowChatMessageTime(
    messages: List<FaceVideoMessage>,
    index: Int
): Boolean {
    val current = messages.getOrNull(index) ?: return false
    val next = messages.getOrNull(index + 1) ?: return true
    if (current.senderId != next.senderId) return true

    val currentInstant = parseMessageInstant(current.createdAt) ?: return true
    val nextInstant = parseMessageInstant(next.createdAt) ?: return true
    val gap = Duration.between(currentInstant, nextInstant)
    return gap.isNegative || gap > Duration.ofMinutes(10)
}

/** Compact badge text for unread counts; null means no badge should be shown. */
fun formatUnreadBadge(count: Int): String? = when {
    count <= 0 -> null
    count > 99 -> "99+"
    else -> count.toString()
}

/** Preview data shown in the chat list; when unread messages exist, use the newest unread incoming message. */
data class ChatInboxPreview(
    val text: String,
    val timestamp: String?
)

fun buildChatInboxPreview(
    latestMessagePreview: String,
    latestMessageAt: String?,
    unreadSummary: FaceVideoChatUnreadSummary?
): ChatInboxPreview {
    if (
        unreadSummary != null &&
        unreadSummary.unreadCount > 0 &&
        !unreadSummary.lastUnreadBody.isNullOrBlank()
    ) {
        return ChatInboxPreview(
            text = unreadSummary.lastUnreadBody,
            timestamp = unreadSummary.lastUnreadAt ?: latestMessageAt
        )
    }

    return ChatInboxPreview(
        text = latestMessagePreview,
        timestamp = latestMessageAt
    )
}

/** For a group message, show delivered/read only after every recipient has reached that state. */
fun resolveMessageDeliveryStatus(
    messageId: String,
    receipts: List<FaceVideoMessageReceipt>
): com.facevideo.app.model.MessageDeliveryStatus {
    val messageReceipts = receipts.filter { it.messageId == messageId }
    if (messageReceipts.isEmpty()) return com.facevideo.app.model.MessageDeliveryStatus.SENT

    return when {
        messageReceipts.all { it.readAt != null } ->
            com.facevideo.app.model.MessageDeliveryStatus.READ
        messageReceipts.all { it.deliveredAt != null } ->
            com.facevideo.app.model.MessageDeliveryStatus.DELIVERED
        else ->
            com.facevideo.app.model.MessageDeliveryStatus.SENT
    }
}

/**
 * Merges the latest Realtime snapshot with messages returned directly by the
 * INSERT request. Realtime may echo the same row later; id-based de-duplication
 * keeps it visible exactly once.
 */
fun mergeChatMessageSnapshots(
    serverMessages: List<FaceVideoMessage>,
    locallySentMessages: List<FaceVideoMessage>
): List<FaceVideoMessage> =
    sortChatMessagesChronologically(
        (serverMessages + locallySentMessages).distinctBy { it.id }
    )

