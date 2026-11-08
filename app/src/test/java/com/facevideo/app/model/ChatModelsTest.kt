package com.facevideo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun waveformUsesFixedBarCountAndPreservesSignalEnvelope() {
        val waveform = buildVoiceWaveform(listOf(8, 20, 88, 100), 16)

        assertEquals(16, waveform.size)
        assertEquals(8, waveform.first())
        assertEquals(100, waveform.last())
        assertTrue(waveform.any { it in 80..100 })
        assertTrue(waveform.all { it in 8..100 })
    }

    @Test
    fun emptyWaveformUsesSafeFallbackBars() {
        val waveform = buildVoiceWaveform(emptyList())

        assertEquals(VOICE_WAVEFORM_BAR_COUNT, waveform.size)
        assertTrue(waveform.all { it in 8..100 })
    }

    @Test
    fun recorderAmplitudeIsScaledIntoVisibleBarRange() {
        assertEquals(8, normalizeVoiceAmplitude(0))
        assertEquals(100, normalizeVoiceAmplitude(32_767))
        assertTrue(normalizeVoiceAmplitude(8_000) in 8..100)
    }
}
