package com.facevideo.app.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats the last online heartbeat for a Russian-language messenger status.
 * The supplied instant is converted to the device's local timezone.
 */
internal fun formatLastSeenAt(
    value: String?,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val lastSeen = runCatching { Instant.parse(value) }.getOrNull()
        ?: return "Не в сети"
    val localLastSeen = lastSeen.atZone(zoneId)
    val today = now.atZone(zoneId).toLocalDate()
    val lastSeenDate = localLastSeen.toLocalDate()
    val time = DateTimeFormatter.ofPattern("HH:mm", Locale("ru")).format(localLastSeen)
    val ageSeconds = if (lastSeen.isAfter(now)) Long.MAX_VALUE
        else Duration.between(lastSeen, now).seconds

    return when {
        ageSeconds in 0..59 -> "был(а) только что"
        lastSeenDate == today -> "был(а) сегодня в $time"
        lastSeenDate == today.minusDays(1) -> "был(а) вчера в $time"
        lastSeenDate.year == today.year -> {
            val date = DateTimeFormatter.ofPattern("d MMMM", Locale("ru")).format(localLastSeen)
            "был(а) $date в $time"
        }
        else -> {
            val date = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru")).format(localLastSeen)
            "был(а) $date в $time"
        }
    }
}
