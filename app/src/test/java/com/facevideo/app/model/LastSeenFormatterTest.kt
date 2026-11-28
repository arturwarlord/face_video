package com.facevideo.app.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LastSeenFormatterTest {
    private val now = Instant.parse("2026-11-28T18:00:00Z")
    private val utc = ZoneId.of("UTC")

    @Test
    fun recentHeartbeatUsesJustNowLabel() {
        assertEquals(
            "был(а) только что",
            formatLastSeenAt("2026-11-28T17:59:30Z", now, utc)
        )
    }

    @Test
    fun sameDayHeartbeatShowsLocalClockTime() {
        assertEquals(
            "был(а) сегодня в 17:36",
            formatLastSeenAt("2026-11-28T17:36:00Z", now, utc)
        )
    }

    @Test
    fun previousDayHeartbeatUsesYesterdayLabel() {
        assertEquals(
            "был(а) вчера в 22:10",
            formatLastSeenAt("2026-11-27T22:10:00Z", now, utc)
        )
    }

    @Test
    fun olderHeartbeatIncludesCalendarDateAndTime() {
        assertEquals(
            "был(а) 22 ноября в 14:05",
            formatLastSeenAt("2026-11-22T14:05:00Z", now, utc)
        )
    }

    @Test
    fun absentOrInvalidHeartbeatFallsBackToOffline() {
        assertEquals("Не в сети", formatLastSeenAt(null, now, utc))
        assertEquals("Не в сети", formatLastSeenAt("not-a-timestamp", now, utc))
    }

    @Test
    fun timestampUsesDeviceTimezoneForDayBoundaries() {
        val berlin = ZoneId.of("Europe/Berlin")
        assertEquals(
            "был(а) сегодня в 00:30",
            formatLastSeenAt("2026-11-27T23:30:00Z", now, berlin)
        )
    }
}
