package com.facevideo.app.calls

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.FaceVideoCallSignal
import com.facevideo.app.data.SupabaseCallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A one-to-one audio call using WebRTC media and Supabase only for signaling.
 * STUN-only connectivity is best-effort; networks that require TURN relay need
 * a separately hosted TURN service.
 */
class WebRtcAudioCallClient(
    context: Context,
    private val repository: SupabaseCallRepository,
    private val call: FaceVideoCall,
    private val currentUserId: String,
    private val scope: CoroutineScope,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val isCaller = call.callerId == currentUserId
    private val peerUserId = if (isCaller) call.calleeId else call.callerId
    private val closed = AtomicBoolean(false)
    private val processingLock = Mutex()
    private val processedSignals = mutableSetOf<String>()
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

    private var signalJob: Job? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerState: Boolean = false

    fun startOutgoing() {
        if (!isCaller) {
            onError("Только инициатор может начать исходящий звонок.")
            return
        }
        try {
            prepareConnection()
            observeSignals()
            createOffer()
            setStatus("Вызов собеседника…")
        } catch (error: Exception) {
            fail(error)
        }
    }

    fun startIncoming() {
        if (isCaller) {
            onError("Для этого звонка ожидался входящий вызов.")
            return
        }
        try {
            prepareConnection()
            observeSignals()
            setStatus("Подключаем аудио…")
        } catch (error: Exception) {
            fail(error)
        }
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = enabled
    }

    fun end() {
        if (!closed.compareAndSet(false, true)) return
        signalJob?.cancel()
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        peerConnection = null
        localAudioTrack = null
        audioSource = null

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = previousSpeakerState
        audioManager.mode = previousAudioMode
    }

    private fun prepareConnection() {
        check(!closed.get()) { "Звонок уже завершён." }
        initializeFactory(appContext)
        val factory = peerConnectionFactory ?: error("Не удалось инициализировать WebRTC.")

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousAudioMode = audioManager.mode
        previousSpeakerState = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val configuration = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(
            configuration,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> setStatus("Звонок активен")
                        PeerConnection.IceConnectionState.CHECKING -> setStatus("Устанавливаем соединение…")
                        PeerConnection.IceConnectionState.DISCONNECTED -> setStatus("Соединение прервано. Пытаемся восстановить…")
                        PeerConnection.IceConnectionState.FAILED -> reportError("Не удалось соединить устройства. Возможно, этой сети нужен TURN-сервер.")
                        PeerConnection.IceConnectionState.CLOSED -> Unit
                        else -> Unit
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit

                override fun onIceCandidate(candidate: IceCandidate) {
                    if (closed.get()) return
                    scope.launch {
                        runCatching {
                            repository.sendSignal(
                                callId = call.id,
                                senderId = currentUserId,
                                recipientId = peerUserId,
                                signalType = "ice",
                                payload = buildJsonObject {
                                    put("sdpMid", candidate.sdpMid)
                                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                                    put("candidate", candidate.sdp)
                                }
                            )
                        }.onFailure { reportError(it.message ?: "Не удалось отправить сигнальное сообщение.") }
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
                override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
                override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
                override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
                override fun onRenegotiationNeeded() = Unit

                override fun onAddTrack(
                    receiver: org.webrtc.RtpReceiver,
                    mediaStreams: Array<org.webrtc.MediaStream>
                ) = Unit
            }
        ) ?: error("WebRTC не смог создать соединение.")

        val source = factory.createAudioSource(MediaConstraints())
        audioSource = source
        val track = factory.createAudioTrack(
            "arturchat-audio-${call.id}",
            source
        )
        localAudioTrack = track
        check(peerConnection?.addTrack(track, listOf("arturchat-${call.id}")) != null) {
            "Не удалось подключить микрофон."
        }
    }

    private fun observeSignals() {
        signalJob?.cancel()
        signalJob = scope.launch {
            repository.observeSignals(call.id, currentUserId).collect { signals ->
                signals.sortedBy { it.createdAt }.forEach { signal ->
                    if (processedSignals.add(signal.id)) {
                        processingLock.withLock { handleSignal(signal) }
                    }
                }
            }
        }
    }

    private suspend fun handleSignal(signal: FaceVideoCallSignal) {
        when (signal.signalType) {
            "offer" -> {
                if (isCaller) return
                val description = signal.payload["description"]?.jsonPrimitive?.contentOrNull
                    ?: return reportError("В приглашении отсутствует SDP offer.")
                setRemoteDescription(SessionDescription.Type.OFFER, description) {
                    createAnswer()
                }
            }

            "answer" -> {
                if (!isCaller) return
                val description = signal.payload["description"]?.jsonPrimitive?.contentOrNull
                    ?: return reportError("В ответе отсутствует SDP answer.")
                setRemoteDescription(SessionDescription.Type.ANSWER, description) {
                    flushRemoteCandidates()
                    setStatus("Завершаем подключение…")
                }
            }

            "ice" -> {
                val mid = signal.payload["sdpMid"]?.jsonPrimitive?.contentOrNull
                val lineIndex = signal.payload["sdpMLineIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val candidateText = signal.payload["candidate"]?.jsonPrimitive?.contentOrNull
                if (mid != null && lineIndex != null && candidateText != null) {
                    val candidate = IceCandidate(mid, lineIndex, candidateText)
                    if (peerConnection?.remoteDescription == null) {
                        pendingRemoteCandidates += candidate
                    } else {
                        peerConnection?.addIceCandidate(candidate)
                    }
                }
            }
        }
    }

    private fun createOffer() {
        val connection = peerConnection ?: error("WebRTC соединение не создано.")
        connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                connection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        scope.launch {
                            runCatching {
                                repository.sendSignal(
                                    call.id,
                                    currentUserId,
                                    peerUserId,
                                    "offer",
                                    buildJsonObject { put("description", description.description) }
                                )
                            }.onFailure { reportError(it.message ?: "Не удалось отправить сигнальное сообщение.") }
                        }
                    }

                    override fun onSetFailure(error: String) = reportError(error)
                    override fun onCreateSuccess(description: SessionDescription) = Unit
                    override fun onCreateFailure(error: String) = reportError(error)
                }, description)
            }

            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String) = reportError(error)
            override fun onSetFailure(error: String) = reportError(error)
        }, MediaConstraints())
    }

    private fun createAnswer() {
        val connection = peerConnection ?: return
        connection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                connection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        scope.launch {
                            runCatching {
                                repository.sendSignal(
                                    call.id,
                                    currentUserId,
                                    peerUserId,
                                    "answer",
                                    buildJsonObject { put("description", description.description) }
                                )
                            }.onFailure { reportError(it.message ?: "Не удалось отправить сигнальное сообщение.") }
                        }
                    }

                    override fun onSetFailure(error: String) = reportError(error)
                    override fun onCreateSuccess(description: SessionDescription) = Unit
                    override fun onCreateFailure(error: String) = reportError(error)
                }, description)
            }

            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String) = reportError(error)
            override fun onSetFailure(error: String) = reportError(error)
        }, MediaConstraints())
    }

    private fun setRemoteDescription(
        type: SessionDescription.Type,
        sdp: String,
        onSuccess: () -> Unit
    ) {
        val connection = peerConnection ?: return
        connection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                flushRemoteCandidates()
                onSuccess()
            }

            override fun onSetFailure(error: String) = reportError(error)
            override fun onCreateSuccess(description: SessionDescription) = Unit
            override fun onCreateFailure(error: String) = reportError(error)
        }, SessionDescription(type, sdp))
    }

    private fun flushRemoteCandidates() {
        val candidates = pendingRemoteCandidates.toList()
        pendingRemoteCandidates.clear()
        candidates.forEach { peerConnection?.addIceCandidate(it) }
    }

    private fun setStatus(message: String) {
        scope.launch(Dispatchers.Main) {
            if (!closed.get()) onStatus(message)
        }
    }

    private fun reportError(message: String) {
        scope.launch(Dispatchers.Main) {
            if (!closed.get()) onError(message)
        }
    }

    private fun fail(error: Throwable) {
        Log.e(TAG, "WebRTC audio call failed", error)
        val message = error.message ?: "Не удалось запустить звонок."
        scope.launch(Dispatchers.Main) {
            if (!closed.get()) {
                onError(message)
                end()
            }
        }
    }

    companion object {
        private const val TAG = "ArturChatWebRTC"
        @Volatile private var peerConnectionFactory: PeerConnectionFactory? = null

        private fun initializeFactory(context: Context) {
            if (peerConnectionFactory != null) return
            synchronized(this) {
                if (peerConnectionFactory == null) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(context)
                            .createInitializationOptions()
                    )
                    peerConnectionFactory = PeerConnectionFactory.builder()
                        .createPeerConnectionFactory()
                }
            }
        }
    }
}
