package com.facevideo.app.calls

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.FaceVideoCallSignal
import com.facevideo.app.data.SupabaseCallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A one-to-one audio call using direct WebRTC peer-to-peer media and Supabase
 * only for signaling. STUN helps discover public-facing addresses; no TURN
 * relay is configured, so strict NAT/firewall combinations may block the call.
 */
class WebRtcAudioCallClient(
    context: Context,
    private val repository: SupabaseCallRepository,
    private val call: FaceVideoCall,
    private val currentUserId: String,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val callScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = context.applicationContext
    private val isCaller = call.callerId == currentUserId
    private val peerUserId = if (isCaller) call.calleeId else call.callerId
    // Video is strictly opt-in per call row. Audio calls retain the old media path.
    private val videoEnabled = call.mediaType == "video"
    private val closed = AtomicBoolean(false)
    private val gatheredCandidateCount = AtomicInteger(0)
    private val localHostCandidates = AtomicInteger(0)
    private val localSrflxCandidates = AtomicInteger(0)
    private val localOtherCandidates = AtomicInteger(0)
    private val remoteCandidatesReceived = AtomicInteger(0)
    private val remoteCandidatesQueued = AtomicInteger(0)
    private val remoteCandidatesApplied = AtomicInteger(0)
    private val remoteCandidatesRejected = AtomicInteger(0)
    private val remoteCandidatesAlreadyInSdp = AtomicInteger(0)
    private val remoteDescriptionSet = AtomicBoolean(false)
    private val candidateLock = Any()
    private val remoteSdpCandidateKeys = mutableSetOf<String>()
    private var connectionTimeoutJob: Job? = null
    @Volatile private var lastIceConnectionState = PeerConnection.IceConnectionState.NEW
    @Volatile private var lastIceGatheringState = PeerConnection.IceGatheringState.NEW.name
    @Volatile private var lastRemoteDescriptionType = "none"
    @Volatile private var lastHandledSignal = "none"
    @Volatile private var lastReceiving = false
    @Volatile private var selectedPairSummary = "ещё не выбрана"
    private val processingLock = Mutex()
    private val processedSignals = mutableSetOf<String>()
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

    private var signalJob: Job? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteVideoSinkAttached = false
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val remoteVideoTrackLock = Any()

    var localVideoRenderer: SurfaceViewRenderer? = null
        private set
    var remoteVideoRenderer: SurfaceViewRenderer? = null
        private set
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerState: Boolean = false

    suspend fun startOutgoing() {
        if (!isCaller) {
            onError("Только инициатор может начать исходящий звонок.")
            return
        }
        try {
            prepareConnection()
            if (closed.get()) return
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
            if (closed.get()) return
            observeSignals()
            setStatus(if (videoEnabled) "Подключаем видео…" else "Подключаем аудио…")
        } catch (error: Exception) {
            fail(error)
        }
    }

    fun setMuted(muted: Boolean) {
        if (!closed.get()) runCatching { localAudioTrack?.setEnabled(!muted) }
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        if (closed.get()) return
        runCatching {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = enabled
        }.onFailure { Log.w(TAG, "Could not change speaker state", it) }
    }

    fun setCameraEnabled(enabled: Boolean) {
        if (!closed.get() && videoEnabled) {
            runCatching { localVideoTrack?.setEnabled(enabled) }
                .onFailure { Log.w(TAG, "Could not change camera state", it) }
        }
    }

    fun switchCamera() {
        if (closed.get() || !videoEnabled) return
        val capturer = videoCapturer ?: return
        runCatching {
            capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    localVideoRenderer?.setMirror(isFrontCamera)
                    Log.i(TAG, "Camera switched; front=$isFrontCamera")
                }

                override fun onCameraSwitchError(errorDescription: String) {
                    reportError("Не удалось переключить камеру: $errorDescription")
                }
            })
        }.onFailure { reportError("Не удалось переключить камеру: ${it.message}") }
    }

    fun end() {
        if (!closed.compareAndSet(false, true)) return
        callScope.cancel()
        signalJob?.cancel()
        connectionTimeoutJob?.cancel()
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }

        // Video resources exist only for calls whose media_type is "video".
        // Stop capture before releasing renderers and EGL to avoid camera leaks.
        runCatching { videoCapturer?.stopCapture() }
            .onFailure { Log.w(TAG, "Could not stop camera capture", it) }
        runCatching { videoCapturer?.dispose() }
        runCatching { localVideoRenderer?.let { renderer -> localVideoTrack?.removeSink(renderer) } }
        runCatching { remoteVideoRenderer?.let { renderer -> remoteVideoTrack?.removeSink(renderer) } }
        runCatching { localVideoTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { surfaceTextureHelper?.dispose() }
        runCatching { localVideoRenderer?.release() }
        runCatching { remoteVideoRenderer?.release() }

        remoteVideoSinkAttached = false
        peerConnection = null
        localAudioTrack = null
        audioSource = null
        localVideoTrack = null
        remoteVideoTrack = null
        videoSource = null
        videoCapturer = null
        surfaceTextureHelper = null
        localVideoRenderer = null
        remoteVideoRenderer = null

        runCatching {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = previousSpeakerState
            audioManager.mode = previousAudioMode
        }.onFailure {
            Log.w(TAG, "Could not restore audio mode after call", it)
        }
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
        initializeFactory(appContext, videoEnabled)
        val factory = if (videoEnabled) {
            videoPeerConnectionFactory
        } else {
            peerConnectionFactory
        } ?: error("Не удалось инициализировать WebRTC для выбранного типа звонка.")

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
                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.i(TAG, "P2P signaling state=$state")
                }

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
                            setStatus("Соединение прервано. Проверяем прямой маршрут…")
                            scheduleDirectConnectionTimeout()
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            connectionTimeoutJob?.cancel()
                            reportDirectConnectionFailure("ICE перешёл в FAILED")
                        }
                        PeerConnection.IceConnectionState.CLOSED -> connectionTimeoutJob?.cancel()
                        else -> Unit
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    lastReceiving = receiving
                    Log.i(TAG, "P2P ICE receiving=$receiving")
                }

                override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
                    val local = describeCandidate(event.local.sdp)
                    val remote = describeCandidate(event.remote.sdp)
                    selectedPairSummary =
                        "$local → $remote; reason=${event.reason}; " +
                            "lastData=${event.lastDataReceivedMs}ms; " +
                            "disconnected=${event.estimatedDisconnectedTimeMs}ms"
                    Log.i(TAG, "Selected ICE candidate pair: $selectedPairSummary")
                }
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    lastIceGatheringState = state.name
                    Log.i(TAG, "P2P ICE gathering state=$state, candidates=${gatheredCandidateCount.get()}")

                }

                override fun onIceCandidate(candidate: IceCandidate) {
                    if (closed.get()) return
                    gatheredCandidateCount.incrementAndGet()
                    val candidateType = Regex("""\styp\s+(host|srflx|prflx|relay)""")
                        .find(candidate.sdp)?.groupValues?.getOrNull(1) ?: "unknown"
                    when (candidateType) {
                        "host" -> localHostCandidates.incrementAndGet()
                        "srflx" -> localSrflxCandidates.incrementAndGet()
                        else -> localOtherCandidates.incrementAndGet()
                    }
                    Log.i(
                        TAG,
                        "P2P ICE candidate gathered: type=$candidateType, " +
                            "mid=${candidate.sdpMid}, line=${candidate.sdpMLineIndex}"
                    )
                    callScope.launch {
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

                // Unified Plan delivers remote media through onTrack. Keep
                // onAddTrack as a compatibility path for WebRTC implementations
                // that still emit the legacy callback.
                override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                    if (!videoEnabled) return
                    val videoTrack = transceiver.receiver.track() as? VideoTrack ?: return
                    attachRemoteVideoTrack(videoTrack, "onTrack")
                }

                override fun onAddTrack(
                    receiver: org.webrtc.RtpReceiver,
                    mediaStreams: Array<org.webrtc.MediaStream>
                ) {
                    if (!videoEnabled) return
                    val videoTrack = receiver.track() as? VideoTrack ?: return
                    attachRemoteVideoTrack(videoTrack, "onAddTrack")
                }
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

        // Keep the existing audio track and signaling flow; add a camera track
        // only for video calls. Audio-only calls never initialize camera APIs.
        if (videoEnabled) prepareVideo(factory)
    }

    private fun prepareVideo(factory: PeerConnectionFactory) {
        check(
            appContext.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        ) { "Для видеозвонка разреши ArturChat доступ к камере." }

        // The video PeerConnectionFactory owns a long-lived EGL context
        // shared with its encoder/decoder factories. Renderers and capture
        // must use that same context; do not release it when one call ends.
        val eglContext = videoFactoryEglBase?.eglBaseContext
            ?: error("Не удалось инициализировать EGL для видеокодеков.")
        val localRenderer = SurfaceViewRenderer(appContext).apply {
            init(eglContext, null)
            setEnableHardwareScaler(true)
            setMirror(true)
        }
        val remoteRenderer = SurfaceViewRenderer(appContext).apply {
            init(
                eglContext,
                object : RendererCommon.RendererEvents {
                    override fun onFirstFrameRendered() {
                        Log.i(TAG, "Remote video first frame rendered")
                        setStatus("Видео собеседника получено")
                    }

                    override fun onFrameResolutionChanged(width: Int, height: Int, rotation: Int) {
                        Log.i(TAG, "Remote video frame: ${width}x${height}, rotation=$rotation")
                    }
                }
            )
            setEnableHardwareScaler(true)
            setMirror(false)
        }
        localVideoRenderer = localRenderer
        remoteVideoRenderer = remoteRenderer

        // A remote track can arrive before the renderer is available; attach
        // it now that the SurfaceViewRenderer has been initialized.
        remoteVideoTrack?.let { attachRemoteVideoTrack(it, "renderer-initialized") }

        val source = factory.createVideoSource(false)
        videoSource = source
        val textureHelper = SurfaceTextureHelper.create(
            "ArturChatCamera-${call.id}",
            eglContext
        ) ?: error("Не удалось подготовить видеопоток.")
        surfaceTextureHelper = textureHelper

        val capturer = createCameraCapturer(appContext)
            ?: error("Не удалось найти доступную камеру на устройстве.")
        videoCapturer = capturer
        capturer.initialize(textureHelper, appContext, source.capturerObserver)

        val track = factory.createVideoTrack("arturchat-video-${call.id}", source)
        localVideoTrack = track
        track.setEnabled(true)
        track.addSink(localRenderer)
        check(peerConnection?.addTrack(track, listOf("arturchat-${call.id}")) != null) {
            "Не удалось добавить видеопоток в WebRTC."
        }

        // Conservative starting profile for mobile networks and mid-range phones.
        capturer.startCapture(640, 480, 24)
        Log.i(TAG, "Local camera started: 640x480@24fps")
    }

    /**
     * Attach the peer's video track exactly once. Retain an early track until
     * the renderer exists so a callback race cannot leave the remote screen black.
     */
    private fun attachRemoteVideoTrack(track: VideoTrack, callback: String) {
        if (!videoEnabled || closed.get()) return
        synchronized(remoteVideoTrackLock) {
            if (remoteVideoTrack !== track) {
                if (remoteVideoSinkAttached) {
                    remoteVideoTrack?.let { previous ->
                        remoteVideoRenderer?.let(previous::removeSink)
                    }
                }
                remoteVideoTrack = track
                remoteVideoSinkAttached = false
            }

            val renderer = remoteVideoRenderer
            if (renderer == null) {
                Log.w(TAG, "Remote video track received before renderer initialization ($callback)")
                return
            }
            if (!remoteVideoSinkAttached) {
                track.setEnabled(true)
                track.addSink(renderer)
                remoteVideoSinkAttached = true
                Log.i(TAG, "Remote video track attached via $callback; enabled=true")
                setStatus("Удалённая видеодорожка получена, ждём первый кадр…")
            }
        }
    }

    private fun createCameraCapturer(context: Context): CameraVideoCapturer? {
        val enumerator: CameraEnumerator =
            if (Camera2Enumerator.isSupported(context)) {
                Camera2Enumerator(context)
            } else {
                Camera1Enumerator(true)
            }
        val names = enumerator.deviceNames
        val selectedName = names.firstOrNull { enumerator.isFrontFacing(it) }
            ?: names.firstOrNull()
            ?: return null
        return enumerator.createCapturer(selectedName, null)
    }

    /**
     * Supabase Realtime is the fast path. A small PostgREST polling loop is a
     * recovery path for dropped realtime notifications; rows are deduplicated
     * by signal id so the same SDP/ICE message is never applied twice.
     */
    private fun observeSignals() {
        signalJob?.cancel()
        signalJob = callScope.launch {
            launch {
                runCatching {
                    repository.observeSignals(call.id, currentUserId).collect { signals ->
                        processIncomingSignals(signals, "realtime")
                    }
                }.onFailure {
                    Log.w(TAG, "Realtime signal stream stopped; polling will continue.", it)
                }
            }
            launch {
                while (isActive && !closed.get()) {
                    runCatching {
                        repository.listSignals(call.id, currentUserId)
                    }.onSuccess { signals ->
                        processIncomingSignals(signals, "poll")
                    }.onFailure {
                        Log.w(TAG, "Signal recovery poll failed.", it)
                    }
                    delay(SIGNAL_RECOVERY_POLL_MS)
                }
            }
        }
    }

    private suspend fun processIncomingSignals(
        signals: List<FaceVideoCallSignal>,
        source: String
    ) {
        signals.sortedBy { it.createdAt }.forEach { signal ->
            processingLock.withLock {
                if (processedSignals.add(signal.id)) {
                    lastHandledSignal = signal.signalType
                    Log.i(TAG, "Received ${signal.signalType} signal via $source")
                    handleSignal(signal)
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
                Log.i(TAG, "Applying remote SDP offer, chars=${description.length}")
                setRemoteDescription(SessionDescription.Type.OFFER, description) {
                    createAnswer()
                    scheduleDirectConnectionTimeout()
                }
            }

            "answer" -> {
                if (!isCaller) return
                val description = signal.payload["description"]?.jsonPrimitive?.contentOrNull
                    ?: return reportError("В ответе отсутствует SDP answer.")
                Log.i(TAG, "Applying remote SDP answer, chars=${description.length}")
                setRemoteDescription(SessionDescription.Type.ANSWER, description) {
                    setStatus("Завершаем подключение…")
                    scheduleDirectConnectionTimeout()
                }
            }

            "ice" -> {
                // Candidate signals are independent of offer/answer. Ignore old
                // recovery-control rows so they cannot start a second offer.
                val mid = signal.payload["sdpMid"]?.jsonPrimitive?.contentOrNull
                val lineIndex = signal.payload["sdpMLineIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val candidateText = signal.payload["candidate"]?.jsonPrimitive?.contentOrNull
                if (mid != null && lineIndex != null && candidateText != null) {
                    receiveRemoteCandidate(IceCandidate(mid, lineIndex, candidateText))
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
        callScope.launch {
            setStatus("Отправляем параметры прямого соединения…")
            // Restore the proven P2P order: send SDP as soon as local
            // description is set; ICE candidates are trickled independently.
            val localSdp = connection.localDescription?.description
                ?.takeIf { it.isNotBlank() }
                ?: fallbackDescription.description
            val candidatesInSdp = Regex("(?m)^a=candidate:").findAll(localSdp).count()
            Log.i(
                TAG,
                "Sending P2P $signalType SDP immediately: candidatesInSdp=$candidatesInSdp; " +
                    "remaining candidates use trickle ICE"
            )
            if (candidatesInSdp == 0 && gatheredCandidateCount.get() == 0) {
                Log.i(TAG, "No ICE candidates embedded yet; waiting for trickle ICE callbacks.")
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
                Log.i(TAG, "$signalType SDP sent with $candidatesInSdp embedded ICE candidates")
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
        val embeddedCandidateCount = synchronized(candidateLock) {
            remoteDescriptionSet.set(false)
            remoteSdpCandidateKeys.clear()
            sdp.lineSequence()
                .filter { it.startsWith("a=candidate:") }
                .map { it.removePrefix("a=") }
                .forEach { remoteSdpCandidateKeys.add(it) }
            remoteSdpCandidateKeys.size
        }
        lastRemoteDescriptionType = type.canonicalForm()
        Log.i(TAG, "Remote SDP ${type.canonicalForm()}: embedded candidates=$embeddedCandidateCount")
        connection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.i(TAG, "Remote SDP ${type.canonicalForm()} applied successfully")
                flushRemoteCandidates()
                onSuccess()
            }

            override fun onSetFailure(error: String) {
                Log.e(TAG, "Remote SDP ${type.canonicalForm()} rejected: $error")
                reportError("Не удалось применить параметры удалённого соединения: $error")
            }
            override fun onCreateSuccess(description: SessionDescription) = Unit
            override fun onCreateFailure(error: String) = Unit
        }, SessionDescription(type, sdp))
    }

    private fun receiveRemoteCandidate(candidate: IceCandidate) {
        remoteCandidatesReceived.incrementAndGet()
        val queued = synchronized(candidateLock) {
            if (!remoteDescriptionSet.get()) {
                if (pendingRemoteCandidates.none { it.sdp == candidate.sdp }) {
                    pendingRemoteCandidates += candidate
                    remoteCandidatesQueued.incrementAndGet()
                }
                true
            } else {
                false
            }
        }
        if (queued) {
            Log.i(TAG, "Queued remote ICE candidate; remote SDP is not ready yet")
        } else {
            applyRemoteCandidate(candidate)
        }
    }

    private fun flushRemoteCandidates() {
        val candidates = synchronized(candidateLock) {
            remoteDescriptionSet.set(true)
            pendingRemoteCandidates.toList().also { pendingRemoteCandidates.clear() }
        }
        Log.i(TAG, "Applying ${candidates.size} queued remote ICE candidates")
        candidates.forEach { applyRemoteCandidate(it) }
    }

    private fun applyRemoteCandidate(candidate: IceCandidate) {
        val alreadyInSdp = synchronized(candidateLock) {
            candidate.sdp in remoteSdpCandidateKeys
        }
        if (alreadyInSdp) {
            remoteCandidatesAlreadyInSdp.incrementAndGet()
            return
        }
        val added = runCatching { peerConnection?.addIceCandidate(candidate) == true }
            .getOrElse {
                Log.w(TAG, "Could not add remote ICE candidate", it)
                false
            }
        if (added) {
            remoteCandidatesApplied.incrementAndGet()
            Log.i(TAG, "Remote ICE candidate accepted")
        } else {
            remoteCandidatesRejected.incrementAndGet()
            Log.w(TAG, "Remote ICE candidate was rejected by WebRTC")
        }
    }

    private fun diagnosticSummary(): String {
        val pending = synchronized(candidateLock) { pendingRemoteCandidates.size }
        val signalingState = peerConnection?.signalingState()?.name ?: "none"
        return "Диагностика: ICE=$lastIceConnectionState; receiving=$lastReceiving; " +
            "remote SDP=${remoteDescriptionSet.get()}($lastRemoteDescriptionType); " +
            "signaling=$signalingState; gathering=$lastIceGatheringState; signal=$lastHandledSignal; " +
            "selectedPair=$selectedPairSummary; локальные host/srflx/other=" +
            "${localHostCandidates.get()}/${localSrflxCandidates.get()}/${localOtherCandidates.get()}; " +
            "удалённые received/queuedTotal/pendingNow/accepted/rejected/alreadyInSdp=" +
            "${remoteCandidatesReceived.get()}/${remoteCandidatesQueued.get()}/${pending}/" +
            "${remoteCandidatesApplied.get()}/${remoteCandidatesRejected.get()}/" +
            "${remoteCandidatesAlreadyInSdp.get()}"
    }

    private fun reportDirectConnectionFailure(reason: String) {
        Log.w(TAG, "$reason. ${diagnosticSummary()}")
        reportError("$DIRECT_P2P_FAILED_MESSAGE\n$reason\n${diagnosticSummary()}")
    }

    private fun describeCandidate(candidateSdp: String): String {
        val parts = candidateSdp.trim().split(Regex("\\s+"))
        val protocol = parts.getOrNull(2)?.uppercase() ?: "unknown"
        val address = parts.getOrNull(4).orEmpty()
        val addressFamily = when {
            address.contains(':') -> "IPv6"
            address.isNotBlank() -> "IPv4"
            else -> "IP?"
        }
        val type = Regex("""\styp\s+(host|srflx|prflx|relay)""")
            .find(candidateSdp)?.groupValues?.getOrNull(1) ?: "unknown"
        // Do not expose raw IP addresses in the call UI.
        return "$type/$protocol/$addressFamily"
    }

    private fun scheduleDirectConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = callScope.launch {
            delay(DIRECT_P2P_TIMEOUT_MS)
            val state = lastIceConnectionState
            if (state == PeerConnection.IceConnectionState.NEW ||
                state == PeerConnection.IceConnectionState.CHECKING ||
                state == PeerConnection.IceConnectionState.DISCONNECTED
            ) {
                reportDirectConnectionFailure("Не удалось выбрать прямую пару ICE-кандидатов за 30 секунд")
            }
        }
    }

    private fun setStatus(message: String) {
        callScope.launch(Dispatchers.Main) {
            if (!closed.get()) onStatus(message)
        }
    }

    private fun reportError(message: String) {
        callScope.launch(Dispatchers.Main) {
            if (!closed.get()) onError(message)
        }
    }

    private fun fail(error: Throwable) {
        Log.e(TAG, "WebRTC audio call failed", error)
        val message = error.message ?: "Не удалось запустить звонок."
        callScope.launch(Dispatchers.Main) {
            if (!closed.get()) {
                onError(message)
                end()
            }
        }
    }

    companion object {
        private const val TAG = "ArturChatWebRTC"
        private const val DIRECT_P2P_TIMEOUT_MS = 30_000L
        private const val SIGNAL_RECOVERY_POLL_MS = 900L
        private const val DIRECT_P2P_FAILED_MESSAGE =
            "Не удалось установить прямое соединение между телефонами. " +
                "В этой версии нет TURN-реле. Попробуйте подключить оба телефона к одной Wi-Fi сети " +
                "или переключить один из телефонов на другую сеть."
        // Keep the proven audio-only factory completely unchanged. Video
        // calls use a second factory with real encoder/decoder implementations;
        // otherwise SDP can advertise an empty/rejected m=video section.
        @Volatile private var peerConnectionFactory: PeerConnectionFactory? = null
        @Volatile private var videoPeerConnectionFactory: PeerConnectionFactory? = null
        @Volatile private var videoFactoryEglBase: EglBase? = null
        @Volatile private var webRtcRuntimeInitialized = false

        private fun initializeFactory(context: Context, withVideo: Boolean) {
            synchronized(this) {
                if (!webRtcRuntimeInitialized) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(context)
                            .createInitializationOptions()
                    )
                    webRtcRuntimeInitialized = true
                }

                if (withVideo) {
                    if (videoPeerConnectionFactory == null) {
                        val eglBase = EglBase.create()
                        try {
                            val factory = PeerConnectionFactory.builder()
                                .setVideoEncoderFactory(
                                    DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
                                )
                                .setVideoDecoderFactory(
                                    DefaultVideoDecoderFactory(eglBase.eglBaseContext)
                                )
                                .createPeerConnectionFactory()
                            videoFactoryEglBase = eglBase
                            videoPeerConnectionFactory = factory
                            Log.i(TAG, "Video PeerConnectionFactory initialized with video codecs")
                        } catch (error: Throwable) {
                            runCatching { eglBase.release() }
                            throw error
                        }
                    }
                } else if (peerConnectionFactory == null) {
                    // Preserve the original factory used by audio-only calls.
                    peerConnectionFactory = PeerConnectionFactory.builder()
                        .createPeerConnectionFactory()
                }
            }
        }
    }
}
