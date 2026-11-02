package com.facevideo.app.data

import java.time.Instant
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


/** Compact badge text for unread counts; null means no badge should be shown. */
fun formatUnreadBadge(count: Int): String? = when {
    count <= 0 -> null
    count > 99 -> "99+"
    else -> count.toString()
}
