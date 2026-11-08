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
    fun rawRecorderPeaksProduceDistinctTelegramStyleBars() {
        val waveform = buildVoiceWaveform(
            listOf(0, 900, 3_500, 12_000, 26_000, 5_000, 18_000, 1_500),
            barCount = 16
        )

        assertEquals(16, waveform.size)
        assertEquals(8, waveform.first())
        assertEquals(100, waveform.maxOrNull())
        assertTrue(waveform.all { it in 8..100 })
        assertTrue(waveform.toSet().size >= 6)
        assertTrue((waveform.maxOrNull() ?: 0) - (waveform.minOrNull() ?: 0) >= 55)
    }

    @Test
    fun emptyOrSilentRecordingUsesQuietFallbackBars() {
        val empty = buildVoiceWaveform(emptyList())
        val silent = buildVoiceWaveform(List(30) { 0 })

        assertEquals(VOICE_WAVEFORM_BAR_COUNT, empty.size)
        assertTrue(empty.all { it == 8 })
        assertEquals(VOICE_WAVEFORM_BAR_COUNT, silent.size)
        assertTrue(silent.all { it == 8 })
    }

    @Test
    fun waveformKeepsAnIsolatedLoudPeakInsteadOfFlatteningTheWholeRecording() {
        val waveform = buildVoiceWaveform(List(63) { 1_500 } + 32_767)

        assertEquals(VOICE_WAVEFORM_BAR_COUNT, waveform.size)
        assertEquals(100, waveform.last())
        assertTrue(waveform.first() < 60)
        assertTrue(waveform.toSet().size > 1)
    }

    @Test
    fun longRecordingsKeepThePeakInsideEachTimeSlice() {
        val samples = List(256) { index ->
            val groupLevel = when ((index / 4) % 4) {
                0 -> 1_000
                1 -> 4_000
                2 -> 18_000
                else -> 7_000
            }
            if (index % 4 == 2) groupLevel else groupLevel / 3
        }
        val waveform = buildVoiceWaveform(samples)

        assertEquals(VOICE_WAVEFORM_BAR_COUNT, waveform.size)
        assertTrue(waveform.toSet().size >= 3)
    }
}
