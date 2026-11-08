package com.facevideo.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatModelsTest {
    private val chats = listOf(
        ChatSummary("1", "Мария", "До встречи", "12:00", initials = "М"),
        ChatSummary("2", "Александр", "Отправил фото", "11:00", initials = "А")
    )

    @Test
    fun emptyQueryReturnsAllChats() {
        assertEquals(2, filterChats(chats, "").size)
    }

    @Test
    fun queryMatchesTitleIgnoringCase() {
        assertEquals("Александр", filterChats(chats, "АЛЕКС").single().title)
    }

    @Test
    fun queryMatchesMessagePreview() {
        assertEquals("Мария", filterChats(chats, "встречи").single().title)
    }

    @Test
    fun voiceDurationFormatsMinutesAndSeconds() {
        assertEquals("0:00", formatVoiceDuration(0L))
        assertEquals("1:05", formatVoiceDuration(65_000L))
        assertEquals("10:00", formatVoiceDuration(600_000L))
    }

    @Test
    fun negativeVoiceDurationIsClampedToZero() {
        assertEquals("0:00", formatVoiceDuration(-500L))
    }
}
