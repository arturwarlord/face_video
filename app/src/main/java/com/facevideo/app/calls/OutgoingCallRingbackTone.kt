package com.facevideo.app.calls

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Local ringback heard by the caller while an outgoing ArturChat call is
 * waiting for the callee to answer. The platform tone uses the regional
 * telephone ring cadence and is rendered locally, never sent to the peer.
 */
internal class OutgoingCallRingbackTone {
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    fun start() {
        if (toneGenerator != null) return

        val generator = runCatching {
            ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_VOLUME)
        }.onFailure {
            Log.w(TAG, "Could not create outgoing ringback tone", it)
        }.getOrNull() ?: return

        val started = runCatching {
            generator.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        }.onFailure {
            Log.w(TAG, "Could not start outgoing ringback tone", it)
        }.getOrDefault(false)

        if (started) {
            toneGenerator = generator
        } else {
            runCatching { generator.release() }
            Log.w(TAG, "Outgoing ringback tone was not started")
        }
    }

    @Synchronized
    fun stop() {
        val generator = toneGenerator ?: return
        toneGenerator = null
        runCatching { generator.stopTone() }
            .onFailure { Log.w(TAG, "Could not stop outgoing ringback tone", it) }
        runCatching { generator.release() }
            .onFailure { Log.w(TAG, "Could not release outgoing ringback tone", it) }
    }

    private companion object {
        const val TAG = "ArturChatRingback"
        const val TONE_VOLUME = 60
    }
}
