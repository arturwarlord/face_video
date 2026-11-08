package com.facevideo.app.model

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ChatSummary(
    val id: String,
    val title: String,
    val preview: String,
    val time: String,
    val unreadCount: Int = 0,
    val online: Boolean = false,
    val initials: String,
    val avatarStyle: Int = 0
)

enum class MessageDeliveryStatus {
    SENT,
    DELIVERED,
    READ
}

data class ChatMessage(
    val id: String,
    val text: String,
    val time: String,
    val isMine: Boolean,
    val deliveryStatus: MessageDeliveryStatus? = null,
    val forwardedFromName: String? = null,
    val isDeleted: Boolean = false,
    val attachmentMimeType: String? = null,
    val attachmentDurationMs: Long? = null,
    val attachmentWaveform: List<Int>? = null
)

fun filterChats(chats: List<ChatSummary>, query: String): List<ChatSummary> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return chats
    return chats.filter {
        it.title.contains(normalized, ignoreCase = true) ||
            it.preview.contains(normalized, ignoreCase = true)
    }
}

fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
}


const val VOICE_WAVEFORM_BAR_COUNT = 64

/** Converts MediaRecorder peak amplitude into a compact, perceptually scaled bar height. */
fun normalizeVoiceAmplitude(amplitude: Int): Int {
    val safeAmplitude = amplitude.coerceIn(0, 32_767)
    if (safeAmplitude == 0) return 8
    return (sqrt(safeAmplitude / 32_767.0) * 100.0)
        .roundToInt()
        .coerceIn(8, 100)
}

/** Resamples recorded amplitude snapshots to a fixed-width waveform for message storage and playback. */
fun buildVoiceWaveform(
    samples: List<Int>,
    barCount: Int = VOICE_WAVEFORM_BAR_COUNT
): List<Int> {
    val safeBarCount = barCount.coerceIn(16, 96)
    if (samples.isEmpty()) return List(safeBarCount) { 14 }
    val normalized = samples.map { it.coerceIn(8, 100) }
    if (normalized.size == 1) return List(safeBarCount) { normalized.first() }

    return List(safeBarCount) { index ->
        val sourcePosition = index.toDouble() * (normalized.size - 1).toDouble() / (safeBarCount - 1).toDouble()
        val lowerIndex = sourcePosition.toInt().coerceIn(0, normalized.lastIndex)
        val upperIndex = (lowerIndex + 1).coerceAtMost(normalized.lastIndex)
        val fraction = sourcePosition - lowerIndex
        (normalized[lowerIndex] * (1.0 - fraction) + normalized[upperIndex] * fraction)
            .roundToInt()
            .coerceIn(8, 100)
    }
}
