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
import kotlinx.coroutines.delay
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
import java.util.concurrent.atomic.AtomicInteger

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
    private val gatheredCandidateCount = AtomicInteger(0)
    private var connectionTimeoutJob: Job? = null
    @Volatile private var lastIceConnectionState = PeerConnection.IceConnectionState.NEW
    private val processingLock = Mutex()
    private val processedSignals = mutableSetOf<String>()
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

    private var signalJob: Job? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerState: Boolean = false

    suspend fun startOutgoing() {
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

    suspend fun startIncoming() {
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
        connectionTimeoutJob?.cancel()
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

    /**
     * Direct peer-to-peer mode. STUN is used only to discover public-facing
     * network addresses; audio is never relayed by these STUN services.
     * No TURN server or third-party media relay is configured in this build.
     */
    private fun loadIceServers(): List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
    )

    private suspend fun prepareConnection() {
        check(!closed.get()) { "Звонок уже завершён." }
        initializeFactory(appContext)
        val factory = peerConnectionFactory ?: error("Не удалось инициализировать WebRTC.")

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousAudioMode = audioManager.mode
        previousSpeakerState = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        val iceServers = loadIceServers()
        val configuration = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(
            configuration,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    lastIceConnectionState = state
                    Log.i(TAG, "P2P ICE connection state=$state")
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            connectionTimeoutJob?.cancel()
                            setStatus("Звонок активен")
                        }
                        PeerConnection.IceConnectionState.CHECKING -> {
                            setStatus("Проверяем прямое соединение…")
                            scheduleDirectConnectionTimeout()
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            setStatus("Соединение прервано. Пытаемся восстановить…")
                            scheduleDirectConnectionTimeout()
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            connectionTimeoutJob?.cancel()
                            reportError(DIRECT_P2P_FAILED_MESSAGE)
                        }
                        PeerConnection.IceConnectionState.CLOSED -> connectionTimeoutJob?.cancel()
                        else -> Unit
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.i(TAG, "P2P ICE gathering state=$state, candidates=${gatheredCandidateCount.get()}")
                }

                override fun onIceCandidate(candidate: IceCandidate) {
                    if (closed.get()) return
                    gatheredCandidateCount.incrementAndGet()
                    val candidateType = Regex("""\styp\s+(host|srflx|relay)""")
                        .find(candidate.sdp)?.groupValues?.getOrNull(1) ?: "unknown"
                    Log.i(
                        TAG,
                        "P2P ICE candidate gathered: type=$candidateType, " +
                            "mid=${candidate.sdpMid}, line=${candidate.sdpMLineIndex}"
                    )
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
                        sendLocalDescription(connection, "offer", description)
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
                        sendLocalDescription(connection, "answer", description)
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

    private fun sendLocalDescription(
        connection: PeerConnection,
        signalType: String,
        fallbackDescription: SessionDescription
    ) {
        scope.launch {
            setStatus("Отправляем параметры прямого соединения…")
            // Trickle ICE sends candidates independently through Supabase, so
            // the offer/answer does not wait for gathering to finish.
            val localSdp = connection.localDescription?.description
                ?.takeIf { it.isNotBlank() }
                ?: fallbackDescription.description
            val candidatesInSdp = Regex("(?m)^a=candidate:").findAll(localSdp).count()
            Log.i(
                TAG,
                "Sending P2P $signalType SDP: candidatesInSdp=$candidatesInSdp, " +
                    "candidateCallbacks=${gatheredCandidateCount.get()}; remaining candidates use trickle ICE"
            )
            if (candidatesInSdp == 0 && gatheredCandidateCount.get() == 0) {
                Log.i(TAG, "No ICE candidates embedded in SDP yet; waiting for trickle ICE callbacks.")
            }

            runCatching {
                repository.sendSignal(
                    callId = call.id,
                    senderId = currentUserId,
                    recipientId = peerUserId,
                    signalType = signalType,
                    payload = buildJsonObject { put("description", localSdp) }
                )
            }.onSuccess {
                Log.i(TAG, "$signalType SDP sent with $candidatesInSdp ICE candidates")
            }.onFailure {
                reportError(it.message ?: "Не удалось отправить параметры соединения.")
            }
        }
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

    private fun scheduleDirectConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(DIRECT_P2P_TIMEOUT_MS)
            val state = lastIceConnectionState
            if (state == PeerConnection.IceConnectionState.CHECKING ||
                state == PeerConnection.IceConnectionState.DISCONNECTED
            ) {
                reportError(DIRECT_P2P_FAILED_MESSAGE)
            }
        }
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
        private const val DIRECT_P2P_TIMEOUT_MS = 30_000L
        private const val DIRECT_P2P_FAILED_MESSAGE =
            "Не удалось установить прямое соединение между телефонами. " +
                "В этой версии нет TURN-реле. Попробуйте подключить оба телефона к одной Wi-Fi сети " +
                "или переключить один из телефонов на другую сеть."
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
