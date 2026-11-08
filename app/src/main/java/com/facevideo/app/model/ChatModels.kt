package com.facevideo.app.model

import kotlin.math.pow
import kotlin.math.roundToInt

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

/**
 * Converts raw MediaRecorder peak amplitudes into Telegram-style waveform bars.
 * Each recording is normalized against its own peak so pauses, quiet syllables,
 * and loud accents remain visually distinct instead of being flattened.
 */
fun buildVoiceWaveform(
    samples: List<Int>,
    barCount: Int = VOICE_WAVEFORM_BAR_COUNT
): List<Int> {
    val safeBarCount = barCount.coerceIn(16, 96)
    val raw = samples.map { it.coerceIn(0, 32_767) }
    val peak = raw.maxOrNull() ?: 0
    if (peak <= 0) return List(safeBarCount) { 8 }

    // A sub-linear curve keeps quiet speech visible without flattening stronger peaks.
    val normalized = raw.map { amplitude ->
        val ratio = (amplitude.toDouble() / peak.toDouble()).coerceIn(0.0, 1.0)
        val shaped = if (ratio < 0.025) 0.0 else ratio.pow(0.78)
        (8 + (shaped * 92.0).roundToInt()).coerceIn(8, 100)
    }
    if (normalized.size == 1) return List(safeBarCount) { normalized.first() }

    // If there are more samples than bars, keep each time slice's local peak.
    // If the recording is short, interpolate between real samples without inventing a pattern.
    if (normalized.size >= safeBarCount) {
        return List(safeBarCount) { index ->
            val from = (index.toLong() * normalized.size / safeBarCount).toInt()
            val until = (((index + 1L) * normalized.size / safeBarCount).toInt())
                .coerceAtLeast(from + 1)
                .coerceAtMost(normalized.size)
            normalized.subList(from, until).maxOrNull() ?: 8
        }
    }

    return List(safeBarCount) { index ->
        val position = index.toDouble() * (normalized.size - 1).toDouble() / (safeBarCount - 1).toDouble()
        val lower = position.toInt().coerceIn(0, normalized.lastIndex)
        val upper = (lower + 1).coerceAtMost(normalized.lastIndex)
        val fraction = position - lower
        (normalized[lower] * (1.0 - fraction) + normalized[upper] * fraction)
            .roundToInt()
            .coerceIn(8, 100)
    }
}
