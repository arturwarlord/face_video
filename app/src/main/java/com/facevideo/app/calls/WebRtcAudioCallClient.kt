package com.facevideo.app.calls

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.FaceVideoCallSignal
import com.facevideo.app.data.SupabaseCallRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
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
    private val scope: CoroutineScope,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val isCaller = call.callerId == currentUserId
    private val peerUserId = if (isCaller) call.calleeId else call.callerId
    private val closed = AtomicBoolean(false)
    @Volatile private var iceGatheringCompleted = CompletableDeferred<Unit>()
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
    private var recoveryJob: Job? = null
    private val restartAttempted = AtomicBoolean(false)
    private val restartRequestSent = AtomicBoolean(false)
    @Volatile private var lastIceConnectionState = PeerConnection.IceConnectionState.NEW
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
        recoveryJob?.cancel()
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
                            recoveryJob?.cancel()
                            setStatus(
                                if (restartAttempted.get()) {
                                    "Соединение восстановлено"
                                } else {
                                    "Звонок активен"
                                }
                            )
                        }
                        PeerConnection.IceConnectionState.CHECKING -> {
                            setStatus("Проверяем прямое соединение…")
                            scheduleDirectConnectionTimeout()
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            setStatus("Соединение прервано. Запускаем восстановление…")
                            scheduleIceRecovery("ICE DISCONNECTED", ICE_RECOVERY_GRACE_MS)
                            scheduleDirectConnectionTimeout()
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            connectionTimeoutJob?.cancel()
                            if (restartAttempted.get()) {
                                reportDirectConnectionFailure("ICE перешёл в FAILED после попытки восстановления")
                            } else {
                                setStatus("Связь потеряна. Перезапускаем сетевое согласование…")
                                scheduleIceRecovery("ICE FAILED", 1_000L)
                            }
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
                    Log.i(TAG, "P2P ICE gathering state=$state, candidates=${gatheredCandidateCount.get()}")
                    if (state == PeerConnection.IceGatheringState.COMPLETE) {
                        iceGatheringCompleted.complete(Unit)
                    }
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

    /**
     * Supabase Realtime is the fast path. A small PostgREST polling loop is a
     * recovery path for dropped realtime notifications; rows are deduplicated
     * by signal id so the same SDP/ICE message is never applied twice.
     */
    private fun observeSignals() {
        signalJob?.cancel()
        signalJob = scope.launch {
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
                val recoveryRequest = signal.payload["recoveryRequest"]?.jsonPrimitive?.contentOrNull
                if (recoveryRequest == "ice-restart" && isCaller) {
                    initiateIceRestart("Собеседник запросил восстановление ICE")
                    return
                }
                val mid = signal.payload["sdpMid"]?.jsonPrimitive?.contentOrNull
                val lineIndex = signal.payload["sdpMLineIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val candidateText = signal.payload["candidate"]?.jsonPrimitive?.contentOrNull
                if (mid != null && lineIndex != null && candidateText != null) {
                    val candidate = IceCandidate(mid, lineIndex, candidateText)
                    receiveRemoteCandidate(candidate)
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
            setStatus(
                if (restartAttempted.get()) {
                    "Согласуем новый сетевой маршрут…"
                } else {
                    "Отправляем параметры прямого соединения…"
                }
            )
            // Trickle ICE sends candidates independently through Supabase, so
            // the offer/answer does not wait for gathering to finish.
            val gatheringFinished = withTimeoutOrNull(LOCAL_ICE_GATHER_WAIT_MS) {
                iceGatheringCompleted.await()
                true
            } ?: false
            val localSdp = connection.localDescription?.description
                ?.takeIf { it.isNotBlank() }
                ?: fallbackDescription.description
            val candidatesInSdp = Regex("(?m)^a=candidate:").findAll(localSdp).count()
            Log.i(
                TAG,
                "Local ICE gather finished=$gatheringFinished; SDP candidate count=$candidatesInSdp"
            )
            Log.i(
                TAG,
                "Sending P2P $signalType SDP: candidatesInSdp=$candidatesInSdp, " +
                    "candidateCallbacks=${gatheredCandidateCount.get()}; remaining candidates use trickle ICE"
            )
            if (candidatesInSdp == 0 && gatheredCandidateCount.get() == 0) {
                Log.w(TAG, "No ICE candidates found in SDP or callbacks yet.")
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
        val embeddedCandidateCount = synchronized(candidateLock) {
            remoteDescriptionSet.set(false)
            remoteSdpCandidateKeys.clear()
            sdp.lineSequence()
                .filter { it.startsWith("a=candidate:") }
                .map { it.removePrefix("a=") }
                .forEach { remoteSdpCandidateKeys.add(it) }
            remoteSdpCandidateKeys.size
        }
        Log.i(TAG, "Remote SDP ${type.canonicalForm()}: embedded candidates=$embeddedCandidateCount")
        connection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                remoteDescriptionSet.set(true)
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
        return "Диагностика: ICE=$lastIceConnectionState; receiving=$lastReceiving; " +
            "remote SDP=${remoteDescriptionSet.get()}; signal=$lastHandledSignal; " +
            "restartAttempted=${restartAttempted.get()}; selectedPair=$selectedPairSummary; " +
            "локальные host/srflx/other=" +
            "${localHostCandidates.get()}/${localSrflxCandidates.get()}/${localOtherCandidates.get()}; " +
            "удалённые получено/в очереди/принято/отклонено/уже в SDP=" +
            "${remoteCandidatesReceived.get()}/${pending}/${remoteCandidatesApplied.get()}/" +
            "${remoteCandidatesRejected.get()}/${remoteCandidatesAlreadyInSdp.get()}"
    }

    private fun reportDirectConnectionFailure(reason: String) {
        Log.w(TAG, "$reason. ${diagnosticSummary()}")
        reportError("$DIRECT_P2P_FAILED_MESSAGE\n$reason\n${diagnosticSummary()}")
    }

    /**
     * Only the original caller creates ICE restart offers. The callee asks for
     * recovery using an ICE signal with a control payload, so both peers do not
     * race to create offers on the same PeerConnection.
     */
    private fun scheduleIceRecovery(reason: String, delayMs: Long) {
        recoveryJob?.cancel()
        recoveryJob = scope.launch {
            delay(delayMs)
            val state = lastIceConnectionState
            if (state != PeerConnection.IceConnectionState.DISCONNECTED &&
                state != PeerConnection.IceConnectionState.FAILED
            ) {
                return@launch
            }
            if (isCaller) {
                initiateIceRestart(reason)
            } else {
                sendIceRestartRequest(reason)
            }
        }
    }

    private fun sendIceRestartRequest(reason: String) {
        if (!restartRequestSent.compareAndSet(false, true)) return
        scope.launch {
            runCatching {
                repository.sendSignal(
                    callId = call.id,
                    senderId = currentUserId,
                    recipientId = peerUserId,
                    signalType = "ice",
                    payload = buildJsonObject {
                        put("recoveryRequest", "ice-restart")
                    }
                )
            }.onSuccess {
                Log.i(TAG, "ICE restart requested from caller: $reason")
                setStatus("Запросили у собеседника восстановление соединения…")
            }.onFailure {
                reportError("Не удалось запросить восстановление звонка: ${it.message}")
            }
        }
    }

    private fun initiateIceRestart(reason: String) {
        if (!isCaller) return
        if (!restartAttempted.compareAndSet(false, true)) {
            if (lastIceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                reportDirectConnectionFailure("Повторная попытка ICE restart недоступна")
            }
            return
        }

        recoveryJob?.cancel()
        connectionTimeoutJob?.cancel()
        scope.launch {
            val connection = peerConnection
            if (connection == null || closed.get()) {
                reportDirectConnectionFailure("Нет активного PeerConnection для ICE restart")
                return@launch
            }

            try {
                // A new completion signal is required for the restarted ICE
                // generation; the first gathering cycle has already completed.
                iceGatheringCompleted = CompletableDeferred()
                Log.w(TAG, "Starting one ICE restart: $reason")
                setStatus("Пробуем восстановить прямое соединение…")
                connection.restartIce()
                createOffer()
                scheduleDirectConnectionTimeout()
            } catch (error: Exception) {
                reportDirectConnectionFailure("Не удалось запустить ICE restart: ${error.message}")
            }
        }
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
        connectionTimeoutJob = scope.launch {
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
        private const val ICE_RECOVERY_GRACE_MS = 6_000L
        private const val LOCAL_ICE_GATHER_WAIT_MS = 5_000L
        private const val SIGNAL_RECOVERY_POLL_MS = 900L
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
