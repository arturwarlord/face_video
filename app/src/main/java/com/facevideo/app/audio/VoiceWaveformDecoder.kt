package com.facevideo.app.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.facevideo.app.model.buildVoiceWaveform
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Builds a Telegram-style waveform from decoded audio, not from sparse recorder polling.
 * PCM is measured in 20 ms windows using RMS amplitude, which captures speech dynamics
 * and silence much more reliably than a single peak sample.
 */
fun decodeVoiceWaveformFromFile(
    file: File,
    barCount: Int = 64
): List<Int>? {
    if (!file.isFile || file.length() <= 0L) return null

    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var decoderStarted = false

    return try {
        extractor.setDataSource(file.absolutePath)
        val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        } ?: return null

        val inputFormat = extractor.getTrackFormat(audioTrack)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8_000)
        val channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        val samplesPerWindow = (sampleRate * channels / 50).coerceAtLeast(channels)

        extractor.selectTrack(audioTrack)
        decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()
        decoderStarted = true

        val codec = decoder
        val bufferInfo = MediaCodec.BufferInfo()
        val rmsWindows = ArrayList<Int>((sampleRate / 50 * 600L).toInt().coerceAtMost(30_000))
        var inputEnded = false
        var outputEnded = false
        var windowPower = 0.0
        var windowSampleCount = 0

        fun addSample(sample: Int) {
            val safe = sample.coerceIn(0, 32_767).toDouble()
            windowPower += safe * safe
            windowSampleCount++
            if (windowSampleCount >= samplesPerWindow) {
                rmsWindows += sqrt(windowPower / windowSampleCount).roundToInt()
                windowPower = 0.0
                windowSampleCount = 0
            }
        }

        while (!outputEnded) {
            if (!inputEnded) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                        ?: throw IllegalStateException("Audio decoder input buffer is unavailable.")
                    inputBuffer.clear()
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputEnded = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val pcm = outputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                    pcm.position(bufferInfo.offset)
                    pcm.limit(bufferInfo.offset + bufferInfo.size)
                    val outputFormat = codec.outputFormat
                    val pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    } else {
                        AudioFormat.ENCODING_PCM_16BIT
                    }

                    if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                        while (pcm.remaining() >= 4) {
                            val value = (pcm.float.coerceIn(-1f, 1f) * 32_767f).roundToInt()
                            addSample(abs(value).coerceAtMost(32_767))
                        }
                    } else {
                        while (pcm.remaining() >= 2) {
                            val value = pcm.short.toInt()
                            addSample(abs(value.toLong()).coerceAtMost(32_767L).toInt())
                        }
                    }
                }

                val reachedEnd = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outputIndex, false)
                if (reachedEnd) outputEnded = true
            }
        }

        if (windowSampleCount > 0) {
            rmsWindows += sqrt(windowPower / windowSampleCount).roundToInt()
        }
        if (rmsWindows.isEmpty()) null else buildVoiceWaveform(rmsWindows, barCount)
    } catch (_: Exception) {
        null
    } finally {
        if (decoderStarted) runCatching { decoder?.stop() }
        runCatching { decoder?.release() }
        runCatching { extractor.release() }
    }
}
