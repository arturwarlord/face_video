package com.facevideo.app.calls

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.facevideo.app.BuildConfig
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.FaceVideoCallSignal
import com.facevideo.app.data.SupabaseCallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    private val processingLock = Mutex()
    private val processedSignals = mutableSetOf<String>()
    private val pendingCandidatesLock = Any()
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
     * Fetches short-lived TURN credentials through an authenticated Supabase
     * Edge Function. The Metered API key stays server-side and is never baked
     * into the APK. Public STUN remains available as a fallback.
     */
    private suspend fun loadIceServers(): List<PeerConnection.IceServer> {
        val fallback = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
        )
        if (!BuildConfig.SUPABASE_CONFIGURED) return fallback

        val accessToken = runCatching { repository.currentAccessToken() }
            .getOrElse {
                Log.w(TAG, "No authenticated session for TURN credentials; falling back to STUN.", it)
                return fallback
            }

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val functionUrl = BuildConfig.SUPABASE_URL.trimEnd('/') +
                    "/functions/v1/get-arturchat-turn-credentials"
                connection = (URL(functionUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    // TURN is optional; fail fast and continue with public STUN
                    // instead of making call setup wait on a slow/unavailable broker.
                    connectTimeout = 2_500
                    readTimeout = 2_500
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                connection.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                check(responseCode in 200..299) {
                    "TURN credential function returned HTTP $responseCode."
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(response)
                val json = root.optJSONArray("iceServers")
                    ?: error("TURN credential function returned no iceServers array.")
                val turnServers = buildList {
                    for (index in 0 until json.length()) {
                        val item = json.optJSONObject(index) ?: continue
                        val urls = when (val rawUrls = item.opt("urls")) {
                            is String -> listOf(rawUrls)
                            is JSONArray -> (0 until rawUrls.length()).mapNotNull { urlIndex ->
                                rawUrls.optString(urlIndex).takeIf { it.isNotBlank() && it != "null" }
                            }
                            else -> emptyList()
                        }
                        val username = item.optString("username").takeIf { it.isNotBlank() && it != "null" }
                        val password = item.optString("credential").takeIf { it.isNotBlank() && it != "null" }
                        urls.filter { it.startsWith("turn:", true) || it.startsWith("turns:", true) }
                            .forEach { turnUrl ->
                                val builder = PeerConnection.IceServer.builder(turnUrl)
                                if (username != null) builder.setUsername(username)
                                if (password != null) builder.setPassword(password)
                                add(builder.createIceServer())
                            }
                    }
                }
                check(turnServers.isNotEmpty()) {
                    "TURN credentials response did not contain a TURN server."
                }
                Log.i(TAG, "Loaded ${turnServers.size} TURN relay URLs from the authenticated backend.")
                fallback + turnServers
            } catch (error: Exception) {
                Log.w(TAG, "Could not load TURN credentials; falling back to STUN.", error)
                fallback
            } finally {
                connection?.disconnect()
            }
        }
    }

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
            // Pre-gather a small pool so the first ICE checks can start sooner.
            iceCandidatePoolSize = 2
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
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
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.i(TAG, "ICE gathering state=$state")
                }

                override fun onIceCandidate(candidate: IceCandidate) {
                    if (closed.get()) return
                    gatheredCandidateCount.incrementAndGet()
                    Log.i(TAG, "ICE candidate gathered: mid=${candidate.sdpMid}, line=${candidate.sdpMLineIndex}")
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
                    acceptRemoteCandidate(IceCandidate(mid, lineIndex, candidateText))
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
        // Send the offer/answer as soon as setLocalDescription completes.
        // ICE candidates are sent independently by onIceCandidate (trickle ICE),
        // so we do not hold the call for the full ICE gathering cycle.
        scope.launch {
            val localSdp = connection.localDescription?.description
                ?.takeIf { it.isNotBlank() }
                ?: fallbackDescription.description
            val candidatesInSdp = Regex("(?m)^a=candidate:").findAll(localSdp).count()
            Log.i(
                TAG,
                "Sending $signalType SDP immediately: candidatesInSdp=$candidatesInSdp, " +
                    "candidateCallbacks=${gatheredCandidateCount.get()}"
            )
            setStatus("Отправляем параметры соединения…")

            runCatching {
                repository.sendSignal(
                    callId = call.id,
                    senderId = currentUserId,
                    recipientId = peerUserId,
                    signalType = signalType,
                    payload = buildJsonObject { put("description", localSdp) }
                )
            }.onSuccess {
                Log.i(TAG, "$signalType SDP sent; trickle ICE continues in background")
            }.onFailure {
                reportError(it.message ?: "Не удалось отправить параметры соединения.")
            }
        }
    }

    private fun acceptRemoteCandidate(candidate: IceCandidate) {
        val connection = peerConnection ?: return
        synchronized(pendingCandidatesLock) {
            if (connection.remoteDescription == null) {
                pendingRemoteCandidates += candidate
            } else if (!connection.addIceCandidate(candidate)) {
                Log.w(TAG, "WebRTC rejected a remote ICE candidate")
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
        val connection = peerConnection ?: return
        val candidates = synchronized(pendingCandidatesLock) {
            pendingRemoteCandidates.toList().also { pendingRemoteCandidates.clear() }
        }
        candidates.forEach { candidate ->
            if (!connection.addIceCandidate(candidate)) {
                Log.w(TAG, "WebRTC rejected a queued remote ICE candidate")
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
