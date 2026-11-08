package com.facevideo.app

import android.Manifest
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.facevideo.app.calls.WebRtcAudioCallClient
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.SupabaseCallRepository
import com.facevideo.app.data.DemoData
import com.facevideo.app.data.SupabaseClientProvider
import com.facevideo.app.data.SupabaseChatRepository
import com.facevideo.app.data.ArturChatImageCache
import com.facevideo.app.data.ArturChatLocalCache
import com.facevideo.app.data.FaceVideoChatSummary
import com.facevideo.app.data.FaceVideoProfile
import com.facevideo.app.data.FaceVideoStoryCard
import com.facevideo.app.data.FaceVideoPresence
import com.facevideo.app.data.FaceVideoChatUnreadSummary
import com.facevideo.app.data.formatChatMessageTime
import com.facevideo.app.data.formatUnreadBadge
import com.facevideo.app.data.mergeChatMessageSnapshots
import com.facevideo.app.data.buildChatInboxPreview
import com.facevideo.app.data.resolveMessageDeliveryStatus
import com.facevideo.app.data.sortChatMessagesChronologically
import com.facevideo.app.data.shouldShowChatMessageTime
import com.facevideo.app.update.ArturChatUpdateHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.facevideo.app.notifications.AppNotificationManager
import com.facevideo.app.notifications.CallNotificationIntents
import com.facevideo.app.notifications.PushTokenRegistrar
import com.facevideo.app.model.ChatMessage
import com.facevideo.app.model.ChatSummary
import com.facevideo.app.model.MessageDeliveryStatus
import com.facevideo.app.model.formatVoiceDuration
import com.facevideo.app.model.buildVoiceWaveform
import com.facevideo.app.model.normalizeVoiceAmplitude
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.track
import io.github.jan.supabase.realtime.realtime

// ArturChat palette inspired by the supplied pastel sky reference.
private val AppBackground = Color(0xFFF0F3FF)
private val Panel = Color(0xFFFAFBFF)
private val PanelLight = Color(0xFFEDF1FF)
private val IncomingMessage = Color(0xFFE5EAF6)
private val AccentSlate = Color(0xFF5266D8)
private val MainText = Color(0xFF253047)
private val Muted = Color(0xFF77839A)
private val PaletteBlush = Color(0xFFE6DFFF)
private val PaletteSlate = Color(0xFFC8D7F2)
private val PaletteBlueGray = Color(0xFFD2E3F8)
private val PaletteMist = Color(0xFFE0E7F6)
private val AvatarColors = listOf(
    PaletteBlush, PaletteSlate, PaletteBlueGray, PaletteMist
)

// Inbox-only design tokens. Kept separate from the call/chat surfaces so this
// visual pass cannot unintentionally restyle the already-working call screens.
private val InboxBackground = Color(0xFFF2F4FF)
private val InboxSurface = Color(0xFFFFFFFF)
private val InboxSurfaceSoft = Color(0xFFE8EBFF)
private val InboxMessageIncoming = Color(0xFFDDE3EA)
private val InboxAccent = Color(0xFF5267DB)
private val InboxAccentStrong = Color(0xFF3F50C9)
private val InboxPrimary = Color(0xFF202936)
private val InboxSecondary = Color(0xFF485464)
private val InboxMuted = Color(0xFF788493)
private val InboxBorder = Color(0xFFE1E6EC)
private val InboxOnline = Color(0xFF2F9A72)
private val InboxDanger = Color(0xFFBE5E67)

private enum class MainTab(val title: String) {
    CHATS("Чаты"),
    CALLS("Звонки"),
    CONTACTS("Друзья"),
    PROFILE("Профиль")
}

private data class MessageReplyPreview(
    val sender: String,
    val text: String
)

private data class PreparedChatImage(
    val bytes: ByteArray,
    val fileName: String
)

/**
 * Decodes modern formats through ImageDecoder (including HEIF/HEIC on supported
 * Android versions), scales before allocating large bitmaps, then normalizes
 * every selected image to JPEG for the private chat bucket.
 */
private fun prepareChatImage(context: Context, uri: Uri): PreparedChatImage {
    val resolver = context.contentResolver

    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = android.graphics.ImageDecoder.createSource(resolver, uri)
        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            require(width > 0 && height > 0) {
                "Не удалось определить размер выбранного изображения."
            }
            val longestSide = maxOf(width, height)
            if (longestSide > 1920) {
                val scale = 1920f / longestSide
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IllegalArgumentException("Не удалось открыть выбранное фото.")

        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "Выбранный файл не является поддерживаемым изображением."
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 1920) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            inMutable = true
        }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IllegalArgumentException("Не удалось прочитать выбранное фото.")
    } ?: throw IllegalArgumentException("Не удалось декодировать выбранное изображение.")

    try {
        val output = java.io.ByteArrayOutputStream()
        var encoded = false
        for (quality in listOf(85, 75, 65)) {
            output.reset()
            if (!decoded.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, output)) {
                throw IllegalStateException("Не удалось преобразовать фото в JPEG.")
            }
            if (output.size() <= 10 * 1024 * 1024) {
                encoded = true
                break
            }
        }
        require(encoded && output.size() in 1..(10 * 1024 * 1024)) {
            "Фото слишком большое. Выбери изображение поменьше."
        }

        val originalName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        }.getOrNull().orEmpty()
        val baseName = originalName.substringBeforeLast('.', originalName)
            .replace(Regex("[^A-Za-zА-Яа-я0-9 _-]"), "_")
            .trim()
            .take(220)
            .ifBlank { "photo" }

        return PreparedChatImage(
            bytes = output.toByteArray(),
            fileName = "$baseName.jpg"
        )
    } finally {
        decoded.recycle()
    }
}

data class CallNotificationAction(
    val callId: String,
    val action: String,
    val nonce: Long
)

class MainActivity : ComponentActivity() {
    private val pendingCallNotificationAction = MutableStateFlow<CallNotificationAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureCallNotificationAction(intent)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = AccentSlate,
                    background = AppBackground,
                    surface = Panel,
                    onPrimary = Color.White,
                    onBackground = MainText,
                    onSurface = MainText
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackground
                ) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFE2DCFF),
                                    Color(0xFFE2E9FF),
                                    Color(0xFFE8F0FF),
                                    Color(0xFFF2F5FF)
                                )
                            )
                        )
                    ) {
                        ArturChatUpdateHost {
                            SupabaseAuthGate { userEmail, userId, onSignOut ->
                                FaceVideoApp(
                                    userEmail = userEmail,
                                    userId = userId,
                                    onSignOut = onSignOut,
                                    callNotificationActionFlow = pendingCallNotificationAction,
                                    onCallNotificationActionHandled = { nonce ->
                                        if (pendingCallNotificationAction.value?.nonce == nonce) {
                                            pendingCallNotificationAction.value = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureCallNotificationAction(intent)
    }

    private fun captureCallNotificationAction(intent: Intent?) {
        val callId = intent?.getStringExtra(CallNotificationIntents.EXTRA_CALL_ID)
            ?: return
        val action = intent.getStringExtra(CallNotificationIntents.EXTRA_CALL_ACTION)
            ?: return
        if (action !in setOf(
                CallNotificationIntents.ACTION_ANSWER,
                CallNotificationIntents.ACTION_DECLINE
            )
        ) return
        pendingCallNotificationAction.value = CallNotificationAction(
            callId = callId,
            action = action,
            nonce = System.nanoTime()
        )
    }
}

private sealed interface PendingCallAction {
    data class Outgoing(
        val chat: FaceVideoChatSummary,
        val mediaType: String = "audio"
    ) : PendingCallAction
    data class Incoming(val call: FaceVideoCall) : PendingCallAction
}

@Composable
private fun FaceVideoApp(
    userEmail: String?,
    userId: String?,
    onSignOut: () -> Unit,
    callNotificationActionFlow: StateFlow<CallNotificationAction?>,
    onCallNotificationActionHandled: (Long) -> Unit
) {
    val callNotificationAction by callNotificationActionFlow.collectAsState()

    var selectedTab by remember { mutableStateOf(MainTab.CHATS) }
    var openedChat by remember { mutableStateOf<ChatSummary?>(null) }
    var openedLiveChat by remember { mutableStateOf<FaceVideoChatSummary?>(null) }
    var openedProfileUserId by remember { mutableStateOf<String?>(null) }
    var openedStoriesUserId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val localCache = remember(context) { ArturChatLocalCache(context) }
    var unreadSummaries by remember(userId, localCache) {
        mutableStateOf(userId?.let(localCache::readUnreadSummaries) ?: emptyMap())
    }
    var unreadRefreshKey by remember(userId) { mutableStateOf(0) }
    var showCallInfo by remember { mutableStateOf(false) }
    val callInfoMessage = "Открой личную переписку и нажми значок трубки для аудиозвонка или камеры для видеозвонка."

    val supabase = SupabaseClientProvider.client
    val liveRepository = remember(supabase) { supabase?.let { SupabaseChatRepository(it) } }
    val callRepository = remember(supabase) { supabase?.let { SupabaseCallRepository(it) } }
    val scope = rememberCoroutineScope()
    var realtimeOnlineUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var databaseOnlineUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val onlineUserIds = realtimeOnlineUserIds + databaseOnlineUserIds
    var presenceAvailable by remember { mutableStateOf(false) }

    // Share online state only with authenticated ArturChat profiles through a private Realtime channel.
    LaunchedEffect(supabase, userId) {
        val currentClient = supabase ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        val presenceChannel = currentClient.channel("face_video:online") {
            isPrivate = true
        }
        val presenceFlow = presenceChannel.presenceDataFlow<FaceVideoPresence>()
        val presenceCollector = launch {
            presenceFlow.collect { presences ->
                realtimeOnlineUserIds = presences.mapTo(mutableSetOf()) { it.userId }
            }
        }

        try {
            presenceChannel.subscribe(blockUntilSubscribed = true)
            presenceChannel.track(FaceVideoPresence(userId = currentUserId))
            presenceAvailable = true
            android.util.Log.i("ArturChatPresence", "Online presence connected")
            awaitCancellation()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.w("ArturChatPresence", "Could not connect online presence", error)
            realtimeOnlineUserIds = emptySet()
            presenceAvailable = false
        } finally {
            presenceCollector.cancel()
            runCatching { currentClient.realtime.removeChannel(presenceChannel) }
                .onFailure {
                    android.util.Log.w("ArturChatPresence", "Could not remove online presence channel", it)
                }
            realtimeOnlineUserIds = emptySet()
            presenceAvailable = false
        }
    }

    // Database heartbeat is the reliable fallback when Realtime Presence cannot
    // reserve a connection. A user stays online while at least one device pings.
    LaunchedEffect(liveRepository, userId) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            try {
                repository.heartbeatOnlinePresence(currentUserId)
                databaseOnlineUserIds = repository.listOnlineUserIds()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w("ArturChatPresence", "Database online heartbeat failed", error)
            }
            delay(15_000L)
        }
    }

    val appNotifications = remember(context) { AppNotificationManager(context) }
    val incomingCallsFlow = remember(callRepository, userId) {
        if (callRepository != null && userId != null) {
            callRepository.observeIncomingCalls(userId)
        } else {
            flowOf(emptyList())
        }
    }
    val incomingCalls by incomingCallsFlow.collectAsState(initial = emptyList())
    var incomingCallerProfile by remember { mutableStateOf<FaceVideoProfile?>(null) }
    var activeCall by remember { mutableStateOf<FaceVideoCall?>(null) }
    var activeCallTitle by remember { mutableStateOf("") }
    var activeCallIsOutgoing by remember { mutableStateOf(false) }
    var callStatus by remember { mutableStateOf("Подготовка звонка…") }
    var callError by remember { mutableStateOf<String?>(null) }
    var callMuted by remember { mutableStateOf(false) }
    var callCameraEnabled by remember { mutableStateOf(true) }
    var callClient by remember { mutableStateOf<WebRtcAudioCallClient?>(null) }
    var pendingCallAction by remember { mutableStateOf<PendingCallAction?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(userId, liveRepository) {
        if (
            userId != null &&
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val currentUserId = userId ?: return@LaunchedEffect
        val repository = liveRepository ?: return@LaunchedEffect
        runCatching {
            PushTokenRegistrar.register(context, repository, currentUserId)
        }.onFailure {
            android.util.Log.w("ArturChatPush", "FCM token registration failed", it)
        }
    }

    val currentOpenedLiveChatId by rememberUpdatedState(openedLiveChat?.id)

    // Unread summaries refresh independently from the long-lived realtime subscription.
    LaunchedEffect(liveRepository, userId, unreadRefreshKey) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        runCatching { repository.getUnreadSummaries() }
            .onSuccess { summaries ->
                unreadSummaries = summaries
                withContext(Dispatchers.IO) {
                    localCache.saveUnreadSummaries(currentUserId, summaries)
                }
            }
            .onFailure {
                android.util.Log.w("ArturChatUnread", "Could not refresh unread summaries", it)
            }
    }

    // Keep one realtime listener for the whole signed-in session; screen navigation
    // must not tear it down and reload the entire message snapshot.
    LaunchedEffect(liveRepository, userId) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        val chatTitles = localCache.readChats(currentUserId).associate { it.id to it.title }

        suspend fun markIncomingMessagesDelivered(messages: List<com.facevideo.app.data.FaceVideoMessage>) {
            val incomingIds = messages
                .filter { it.senderId != currentUserId }
                .map { it.id }
            if (incomingIds.isNotEmpty()) {
                runCatching { repository.markMessagesDelivered(incomingIds) }
                    .onFailure {
                        android.util.Log.w("ArturChatReceipts", "Could not mark messages delivered", it)
                    }
            }
        }

        val knownMessageIds = mutableSetOf<String>()
        var initialSnapshotReceived = false

        repository.observeMyMessages().collect { messages ->
            if (!initialSnapshotReceived) {
                knownMessageIds.addAll(messages.map { it.id })
                initialSnapshotReceived = true
                // Warm all per-chat caches from this snapshot without additional chat queries.
                launch(Dispatchers.IO) {
                    messages.groupBy { it.chatId }.forEach { (chatId, chatMessages) ->
                        localCache.seedMessagesIfMissing(currentUserId, chatId, chatMessages)
                    }
                }
                markIncomingMessagesDelivered(messages)
                return@collect
            }

            val newMessages = messages.filter { knownMessageIds.add(it.id) }
            newMessages
                .filter { it.senderId != currentUserId && it.chatId != currentOpenedLiveChatId }
                .forEach { message ->
                    appNotifications.showMessage(
                        notificationId = message.id,
                        title = chatTitles[message.chatId] ?: "Новое сообщение",
                        body = message.body
                    )
                }

            if (newMessages.any { it.senderId != currentUserId }) {
                unreadRefreshKey += 1
            }

            if (newMessages.isNotEmpty()) {
                val changedChatIds = newMessages.map { it.chatId }.distinct()
                launch(Dispatchers.IO) {
                    changedChatIds.forEach { chatId ->
                        localCache.saveMessages(
                            currentUserId,
                            chatId,
                            messages.filter { it.chatId == chatId }
                        )
                    }
                }
            }
            markIncomingMessagesDelivered(newMessages)
        }
    }

    suspend fun performCallAction(action: PendingCallAction) {
        val repository = callRepository ?: throw IllegalStateException("Сервер звонков недоступен.")
        val currentUser = userId ?: throw IllegalStateException("Не удалось определить пользователя.")
        when (action) {
            is PendingCallAction.Outgoing -> {
                callError = null
                val call = repository.startDirectCall(action.chat.id, currentUser, action.mediaType)
                activeCall = call
                activeCallTitle = action.chat.title
                activeCallIsOutgoing = true
                callCameraEnabled = true
                callStatus = if (call.mediaType == "video") "Вызов видеосвязи…" else "Вызов собеседника…"
                val client = WebRtcAudioCallClient(
                    context = context,
                    repository = repository,
                    call = call,
                    currentUserId = currentUser,
                                        onStatus = { callStatus = it },
                    onError = { callError = it }
                )
                callClient = client
                client.startOutgoing()
            }
            is PendingCallAction.Incoming -> {
                callError = null
                appNotifications.cancelNotification(action.call.id)
                repository.answerCall(action.call.id)
                val profile = repository.getProfile(action.call.callerId)
                activeCall = action.call.copy(status = "accepted")
                activeCallTitle = profile?.displayName?.ifBlank { profile.username } ?: "Собеседник"
                activeCallIsOutgoing = false
                callCameraEnabled = true
                callStatus = if (action.call.mediaType == "video") "Подключаем видео…" else "Подключаем аудио…"
                val client = WebRtcAudioCallClient(
                    context = context,
                    repository = repository,
                    call = action.call.copy(status = "accepted"),
                    currentUserId = currentUser,
                    onStatus = { callStatus = it },
                    onError = { callError = it }
                )
                callClient = client
                client.startIncoming()
            }
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingCallAction
        pendingCallAction = null
        if (granted && action != null) {
            scope.launch {
                runCatching { performCallAction(action) }
                    .onFailure { callError = it.message ?: "Не удалось начать звонок." }
            }
        } else if (!granted) {
            callError = "Для аудиозвонка разреши ArturChat доступ к микрофону."
        }
    }

    val videoPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val action = pendingCallAction
        pendingCallAction = null
        val microphoneGranted =
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted =
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (action != null && microphoneGranted && cameraGranted) {
            scope.launch {
                runCatching { performCallAction(action) }
                    .onFailure { callError = it.message ?: "Не удалось начать видеозвонок." }
            }
        } else {
            callError = "Для видеозвонка нужны разрешения на микрофон и камеру."
        }
    }

    fun requestCallPermission(action: PendingCallAction) {
        val needsVideo = when (action) {
            is PendingCallAction.Outgoing -> action.mediaType == "video"
            is PendingCallAction.Incoming -> action.call.mediaType == "video"
        }
        val microphoneGranted =
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted =
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        when {
            needsVideo && (!microphoneGranted || !cameraGranted) -> {
                pendingCallAction = action
                val missingPermissions = buildList {
                    if (!microphoneGranted) add(Manifest.permission.RECORD_AUDIO)
                    if (!cameraGranted) add(Manifest.permission.CAMERA)
                }.toTypedArray()
                videoPermissionsLauncher.launch(missingPermissions)
            }
            !microphoneGranted -> {
                pendingCallAction = action
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                scope.launch {
                    runCatching { performCallAction(action) }
                        .onFailure { callError = it.message ?: "Не удалось начать звонок." }
                }
            }
        }
    }

    LaunchedEffect(callNotificationAction?.nonce, callRepository, userId) {
        val request = callNotificationAction ?: return@LaunchedEffect
        val repository = callRepository ?: return@LaunchedEffect
        val currentUser = userId ?: return@LaunchedEffect

        val call = runCatching { repository.getCallById(request.callId) }.getOrNull()
        if (call != null && call.calleeId == currentUser && call.status == "ringing") {
            when (request.action) {
                CallNotificationIntents.ACTION_ANSWER -> {
                    requestCallPermission(PendingCallAction.Incoming(call))
                }
                CallNotificationIntents.ACTION_DECLINE -> {
                    runCatching { repository.declineCall(call.id) }
                        .onFailure { callError = it.message ?: "Не удалось отклонить вызов." }
                }
            }
        }
        appNotifications.cancelNotification(request.callId)
        onCallNotificationActionHandled(request.nonce)
    }

    fun finishCall() {
        val call = activeCall ?: return
        val shouldCancel = activeCallIsOutgoing && call.status == "ringing"
        val clientToClose = callClient
        val repository = callRepository

        // Stop local media and restore the chat screen immediately. Do not keep
        // the call screen waiting for a network status update from Supabase.
        callClient = null
        activeCall = null
        callMuted = false
        callCameraEnabled = true
        runCatching { clientToClose?.end() }
            .onFailure {
                android.util.Log.w("ArturChatCall", "Could not close local call resources", it)
            }

        // Persist the terminal state in the background; a network failure must
        // never close the Activity or prevent the user from returning to chat.
        scope.launch {
            runCatching {
                if (shouldCancel) {
                    repository?.cancelCall(call.id)
                } else {
                    repository?.endCall(call.id)
                }
            }.onFailure {
                android.util.Log.w("ArturChatCall", "Could not update cancelled call status", it)
            }
        }
    }

    // Back navigation follows the visible screen stack. A profile is shown
    // on top of the existing chat, so dismiss it first and keep that chat open.
    BackHandler(
        enabled = activeCall != null ||
            openedProfileUserId != null ||
            openedStoriesUserId != null ||
            openedLiveChat != null ||
            openedChat != null
    ) {
        when {
            activeCall != null -> finishCall()
            openedProfileUserId != null -> openedProfileUserId = null
            openedStoriesUserId != null -> openedStoriesUserId = null
            openedLiveChat != null -> openedLiveChat = null
            openedChat != null -> openedChat = null
        }
    }

    val incomingCall = incomingCalls.firstOrNull { it.id != activeCall?.id }
    LaunchedEffect(incomingCall?.id) {
        if (incomingCall != null && activeCall == null) {
            appNotifications.startIncomingCallAlert()
        } else {
            appNotifications.stopIncomingCallAlert()
        }
    }
    DisposableEffect(appNotifications) {
        onDispose { appNotifications.stopIncomingCallAlert() }
    }
    LaunchedEffect(incomingCall?.id) {
        incomingCallerProfile = if (incomingCall != null) {
            runCatching { callRepository?.getProfile(incomingCall.callerId) }.getOrNull()
        } else null
    }

    LaunchedEffect(activeCall?.id, callRepository) {
        val call = activeCall ?: return@LaunchedEffect
        val repository = callRepository ?: return@LaunchedEffect
        repository.observeCall(call.id).collect { rows ->
            val latest = rows.firstOrNull() ?: return@collect
            if (activeCall?.id == call.id && activeCall?.status != latest.status) {
                activeCall = activeCall?.copy(status = latest.status)
            }
            when (latest.status) {
                "accepted" -> if (activeCallIsOutgoing && callStatus.startsWith("Вызов")) {
                    callStatus = if (call.mediaType == "video") {
                        "Собеседник ответил. Подключаем видео…"
                    } else {
                        "Собеседник ответил. Подключаем аудио…"
                    }
                }
                "declined" -> {
                    callStatus = "Собеседник отклонил вызов."
                    delay(1800)
                    if (activeCall?.id == call.id) {
                        callClient?.end()
                        callClient = null
                        activeCall = null
                    }
                }
                "cancelled" -> {
                    callStatus = "Вызов отменён."
                    delay(1500)
                    if (activeCall?.id == call.id) {
                        callClient?.end()
                        callClient = null
                        activeCall = null
                    }
                }
                "ended" -> {
                    callStatus = "Звонок завершён."
                    delay(1200)
                    if (activeCall?.id == call.id) {
                        callClient?.end()
                        callClient = null
                        activeCall = null
                    }
                }
            }
        }
    }

    LaunchedEffect(activeCall?.id, activeCallIsOutgoing) {
        val call = activeCall ?: return@LaunchedEffect
        if (activeCallIsOutgoing && call.status == "ringing") {
            delay(45_000)
            if (activeCall?.id == call.id && callStatus.startsWith("Вызов")) {
                runCatching { callRepository?.cancelCall(call.id) }
                callStatus = "Нет ответа."
                delay(1500)
                if (activeCall?.id == call.id) {
                    callClient?.end()
                    callClient = null
                    activeCall = null
                }
            }
        }
    }

    val messagesByChat = remember {
        mutableStateMapOf<String, SnapshotStateList<ChatMessage>>().apply {
            DemoData.chats.forEach { chat ->
                put(chat.id, mutableStateListOf<ChatMessage>().apply {
                    addAll(DemoData.messages[chat.id].orEmpty())
                })
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(AppBackground, Color(0xFFE5EAF0), AppBackground))
        )
    ) {
        if (activeCall != null) {
            if (activeCall?.mediaType == "video" && callClient != null) {
                ActiveVideoCallScreen(
                    peerName = activeCallTitle,
                    status = callStatus,
                    error = callError,
                    muted = callMuted,
                    cameraEnabled = callCameraEnabled,
                    client = callClient!!,
                    onToggleMute = {
                        callMuted = !callMuted
                        callClient?.setMuted(callMuted)
                    },
                    onToggleCamera = {
                        callCameraEnabled = !callCameraEnabled
                        callClient?.setCameraEnabled(callCameraEnabled)
                    },
                    onSwitchCamera = { callClient?.switchCamera() },
                    onEnd = { finishCall() }
                )
            } else {
                ActiveAudioCallScreen(
                    peerName = activeCallTitle,
                    status = callStatus,
                    error = callError,
                    muted = callMuted,
                    onToggleMute = {
                        callMuted = !callMuted
                        callClient?.setMuted(callMuted)
                    },
                    onEnd = { finishCall() }
                )
            }
        } else if (openedStoriesUserId != null && liveRepository != null && userId != null) {
            StoryViewerScreen(
                storyOwnerId = openedStoriesUserId!!,
                currentUserId = userId,
                repository = liveRepository,
                cache = localCache,
                onBack = { openedStoriesUserId = null }
            )
        } else if (openedProfileUserId != null && liveRepository != null && userId != null) {
            UserProfileScreen(
                userId = openedProfileUserId!!,
                currentUserId = userId,
                repository = liveRepository,
                cache = localCache,
                onlineUserIds = onlineUserIds,
                onBack = { openedProfileUserId = null },
                onOpenChat = { chat ->
                    openedProfileUserId = null
                    openedLiveChat = chat
                }
            )
        } else if (openedLiveChat != null && liveRepository != null && userId != null) {
            val currentChat = openedLiveChat!!
            LiveChatScreen(
                chat = currentChat,
                repository = liveRepository,
                cache = localCache,
                userId = userId,
                onlineUserIds = onlineUserIds,
                presenceAvailable = presenceAvailable,
                onBack = { openedLiveChat = null },
                onMarkedRead = { unreadRefreshKey += 1 },
                onOpenProfile = { profileUserId -> openedProfileUserId = profileUserId },
                onCall = { isVideo ->
                    requestCallPermission(
                        PendingCallAction.Outgoing(
                            currentChat,
                            if (isVideo) "video" else "audio"
                        )
                    )
                }
            )
        } else if (openedChat != null) {
            val chat = openedChat!!
            ChatScreen(
                chat = chat,
                messages = messagesByChat[chat.id] ?: mutableStateListOf(),
                onBack = { openedChat = null },
                onCall = { showCallInfo = true }
            )
        } else {
            Column(
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFE2DCFF),
                                Color(0xFFE2E9FF),
                                Color(0xFFE8F0FF),
                                Color(0xFFF2F5FF),
                                Color(0xFFE8E9FF)
                            )
                        )
                    )
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val glowTopLeft = Offset(-w * 0.10f, h * 0.10f)
                        val glowTopRight = Offset(w * 1.02f, h * 0.22f)
                        val glowCenter = Offset(w * 0.46f, h * 0.56f)
                        val glowBottomLeft = Offset(-w * 0.08f, h * 0.94f)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFB49BFF).copy(alpha = 0.58f),
                                    Color(0xFFC9BFFF).copy(alpha = 0.22f),
                                    Color.Transparent
                                ),
                                center = glowTopLeft,
                                radius = w * 0.90f
                            ),
                            radius = w * 0.90f,
                            center = glowTopLeft
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9BCBFF).copy(alpha = 0.56f),
                                    Color(0xFFB8D9FF).copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = glowTopRight,
                                radius = w * 0.82f
                            ),
                            radius = w * 0.82f,
                            center = glowTopRight
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFD0B8FF).copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                center = glowCenter,
                                radius = w * 0.78f
                            ),
                            radius = w * 0.78f,
                            center = glowCenter
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFB8D8FF).copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                center = glowBottomLeft,
                                radius = w * 0.70f
                            ),
                            radius = w * 0.70f,
                            center = glowBottomLeft
                        )
                    }
            ) {
                Box(Modifier.weight(1f)) {
                    when (selectedTab) {
                        MainTab.CHATS -> {
                            if (liveRepository != null && userId != null) {
                                LiveChatListScreen(
                                    repository = liveRepository,
                                    cache = localCache,
                                    userId = userId,
                                    unreadSummaries = unreadSummaries,
                                    onOpenChat = { openedLiveChat = it },
                                    onOpenStories = { openedStoriesUserId = it }
                                )
                            } else {
                                ChatListScreen(onOpenChat = { openedChat = it })
                            }
                        }
                        MainTab.CALLS -> {
                            CallsScreen(
                                repository = callRepository,
                                userId = userId,
                                onOpenProfile = { openedProfileUserId = it }
                            )
                        }
                        MainTab.CONTACTS -> {
                            if (liveRepository != null && userId != null) {
                                FriendsScreen(
                                    repository = liveRepository,
                                    userId = userId,
                                    onlineUserIds = onlineUserIds,
                                    onOpenChat = { openedLiveChat = it },
                                    onOpenProfile = { openedProfileUserId = it }
                                )
                            } else {
                                ContactsScreen(onOpenChat = { openedChat = it })
                            }
                        }
                        MainTab.PROFILE -> ProfileScreen(
                            userEmail = userEmail,
                            userId = userId,
                            repository = liveRepository,
                            cache = localCache,
                            onSignOut = {
                                val currentUserId = userId
                                scope.launch {
                                    if (currentUserId != null && liveRepository != null) {
                                        runCatching {
                                            PushTokenRegistrar.unregister(context, liveRepository, currentUserId)
                                        }
                                    }
                                    onSignOut()
                                }
                            }
                        )
                    }
                }
                BottomNavigation(
                    selectedTab = selectedTab,
                    unreadChatsCount = unreadSummaries.values.sumOf { it.unreadCount.coerceAtLeast(0L) }.coerceAtMost(999L).toInt(),
                    onSelect = { selectedTab = it }
                )
            }
        }
    }

    if (incomingCall != null && activeCall == null) {
        AlertDialog(
            onDismissRequest = {
                appNotifications.cancelNotification(incomingCall.id)
                scope.launch { runCatching { callRepository?.declineCall(incomingCall.id) } }
            },
            containerColor = PanelLight,
            title = {
                Text(
                    if (incomingCall.mediaType == "video") "Входящий видеозвонок" else "Входящий аудиозвонок",
                    color = MainText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        incomingCallerProfile?.displayName?.ifBlank { incomingCallerProfile?.username ?: "Пользователь" }
                            ?: "Пользователь звонит тебе",
                        color = MainText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (incomingCall.mediaType == "video") "ArturChat · видеовызов" else "ArturChat · голосовой вызов",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    if (callError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(callError.orEmpty(), color = Color(0xFFA85F6E), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { requestCallPermission(PendingCallAction.Incoming(incomingCall)) }) {
                    Text("Принять", color = AccentSlate, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    appNotifications.cancelNotification(incomingCall.id)
                    scope.launch { runCatching { callRepository?.declineCall(incomingCall.id) } }
                }) {
                    Text("Отклонить", color = Muted)
                }
            }
        )
    }

    if (showCallInfo) {
        AlertDialog(
            onDismissRequest = { showCallInfo = false },
            containerColor = PanelLight,
            title = { Text("Видеозвонки", color = MainText, fontWeight = FontWeight.Bold) },
            text = { Text(callInfoMessage, color = Muted) },
            confirmButton = {
                TextButton(onClick = { showCallInfo = false }) {
                    Text("Понятно", color = AccentSlate)
                }
            }
        )
    }
}

@Composable
private fun LiveChatListScreen(
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    userId: String,
    unreadSummaries: Map<String, FaceVideoChatUnreadSummary>,
    onOpenChat: (FaceVideoChatSummary) -> Unit,
    onOpenStories: (String) -> Unit,
    contactsMode: Boolean = false
) {
    val cachedChats = remember(cache, userId) { cache.readChats(userId) }
    var chats by remember(cache, userId) { mutableStateOf(cachedChats) }
    var isLoading by remember(cache, userId) { mutableStateOf(cachedChats.isEmpty()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNewChat by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, reloadKey, userId) {
        if (chats.isEmpty()) isLoading = true
        loadError = null
        try {
            val refreshedChats = repository.listMyChats(userId)
            chats = refreshedChats
            withContext(Dispatchers.IO) {
                cache.saveChats(userId, refreshedChats)
            }
        } catch (error: Exception) {
            loadError = error.message ?: "Не удалось загрузить переписки."
            android.util.Log.w("ArturChatCache", "Keeping cached chat list after refresh failure", error)
        } finally {
            isLoading = false
        }
    }

    fun openCachedChat(chat: FaceVideoChatSummary) {
        chats = (chats.filterNot { it.id == chat.id } + chat)
        cache.saveChats(userId, chats)
        onOpenChat(chat)
    }

    val visibleChats = remember(chats, query, unreadSummaries) {
        if (query.isBlank()) chats else chats.filter { chat ->
            chat.title.contains(query.trim(), ignoreCase = true) ||
                chat.lastMessagePreview.contains(query.trim(), ignoreCase = true) ||
                unreadSummaries[chat.id]?.lastUnreadBody.orEmpty().contains(query.trim(), ignoreCase = true)
        }
    }

    // The parent main-tab container paints one continuous background behind
    // the list and bottom navigation. Keep this surface transparent to avoid
    // a nested rectangular panel inside the main gradient.
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, top = 7.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ARTURCHAT",
                modifier = Modifier.weight(1f),
                color = InboxAccentStrong.copy(alpha = 0.88f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.55.sp
            )
            // Keep a comfortable touch target while drawing only the icon:
            // no circular fill, gradient rim, shadow or outline.
            Box(
                modifier = Modifier.size(40.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { showSearch = !showSearch },
                contentAlignment = Alignment.Center
            ) {
                InboxSearchGlyph(iconSize = 18.dp)
            }
        }

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(animationSpec = tween(190)) +
                slideInVertically(animationSpec = tween(240)) { fullHeight -> -fullHeight / 3 } +
                expandVertically(animationSpec = tween(240), expandFrom = Alignment.Top),
            exit = fadeOut(animationSpec = tween(130)) +
                slideOutVertically(animationSpec = tween(180)) { fullHeight -> -fullHeight / 4 } +
                shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top)
        ) {
            Column {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    compact = true
                )
                Spacer(Modifier.height(7.dp))
            }
        }

        if (!contactsMode) {
            StoriesStrip(
                repository = repository,
                cache = cache,
                userId = userId,
                onOpenStories = onOpenStories
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading && chats.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = InboxAccent)
                    }
                }
                visibleChats.isEmpty() && loadError != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Не удалось подключиться к перепискам", color = MainText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(loadError.orEmpty(), color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = { reloadKey++ }) { Text("Повторить", color = InboxAccent) }
                    }
                }
                visibleChats.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Avatar("FV", 1, Modifier.size(68.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (query.isBlank()) "Пока нет переписок" else "Ничего не найдено", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (query.isBlank()) "Начни переписку с другом — нажми кнопку создания чата внизу."
                            else "Попробуй изменить поисковый запрос.",
                            color = Muted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        if (loadError != null) {
                            Text(
                                "Нет соединения — показаны сохранённые переписки",
                                color = Muted,
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Недавние",
                                color = InboxPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${visibleChats.size} чатов",
                                color = InboxMuted,
                                fontSize = 10.sp
                            )
                        }
                        LazyColumn(
                            Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 1.dp, bottom = 82.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(visibleChats, key = { it.id }) { chat ->
                                Column(Modifier.fillMaxWidth()) {
                                    LiveChatRow(
                                        chat = chat,
                                        unreadSummary = unreadSummaries[chat.id],
                                        onClick = { openCachedChat(chat) },
                                        onDelete = {
                                            scope.launch {
                                                runCatching { repository.hideChatForMe(chat.id, userId) }
                                                    .onSuccess {
                                                        chats = chats.filterNot { it.id == chat.id }
                                                        withContext(Dispatchers.IO) { cache.saveChats(userId, chats) }
                                                        reloadKey++
                                                    }
                                                    .onFailure { loadError = it.message ?: "Не удалось удалить чат." }
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier.fillMaxWidth().height(1.dp).background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.78f),
                                                    Color(0xFFD6DDF0).copy(alpha = 0.58f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .clickable { showNewChat = true }
            ) {
                Row(
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF28A8FF), Color(0xFF4E63F0), Color(0xFF9562ED))
                            )
                        )
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NewChatGlyph()
                    Spacer(Modifier.width(10.dp))
                    Text("Начать диалог", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showNewChat) {
        NewChatDialog(
            repository = repository,
            currentUserId = userId,
            onDismiss = { showNewChat = false },
            onOpenChat = { chat ->
                showNewChat = false
                openCachedChat(chat)
            }
        )
    }
}

@Composable
private fun StoriesStrip(
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    userId: String,
    onOpenStories: (String) -> Unit
) {
    val cachedStories = remember(cache, userId) { cache.readStories(userId) }
    val cachedMyProfile = remember(cache, userId) { cache.readProfile(userId, userId) }
    var stories by remember(userId, cache) { mutableStateOf(cachedStories) }
    var myProfile by remember(userId, cache) { mutableStateOf(cachedMyProfile) }
    var loadError by remember(userId) { mutableStateOf<String?>(null) }
    var refreshKey by remember(userId) { mutableStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mediaCache = remember(context) { ArturChatImageCache(context) }

    LaunchedEffect(repository, userId, refreshKey) {
        loadError = null
        runCatching {
            val visible = repository.listVisibleStories(includeMediaUrls = false)
            val own = repository.getMyProfile(userId)
            stories = visible
            myProfile = own
            withContext(Dispatchers.IO) {
                cache.saveStories(userId, visible)
                if (own != null) cache.saveProfile(userId, own)
                mediaCache.pruneStories(userId, visible.mapTo(mutableSetOf()) { it.storyId })
            }
            // Warm one story per author in the background. The story list itself
            // no longer waits for a signed URL for every photo.
            val storiesToPreload = visible.groupBy { it.userId }.values
                .mapNotNull { group -> group.minByOrNull { it.createdAt } }
                .take(8)
            scope.launch(Dispatchers.IO) {
                storiesToPreload.forEach { story ->
                    runCatching {
                        val signedUrl = repository.createStoryMediaUrl(story.mediaPath)
                        mediaCache.fetchAndStore(
                            kind = "stories",
                            ownerId = userId,
                            mediaId = story.storyId,
                            sourceUrl = signedUrl,
                            freshnessMillis = 15 * 60 * 1000L,
                            refreshWhenSourceChanges = false
                        )
                    }.onFailure {
                        android.util.Log.w("ArturChatStories", "Story prefetch failed", it)
                    }
                }
            }
        }.onFailure {
            loadError = it.message ?: "Не удалось загрузить истории."
            android.util.Log.w("ArturChatStories", "Could not refresh visible stories; keeping local cache", it)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            caption = ""
            showComposer = true
        }
    }

    val ownStories = stories.filter { it.userId == userId }
    val friendGroups = stories
        .filter { it.userId != userId }
        .groupBy { it.userId }
        .values
        .map { group -> group.sortedByDescending { it.createdAt }.first() }
        .sortedByDescending { it.createdAt }

    Column(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 19.dp, end = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Истории", color = InboxPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                "Обновить ↻",
                color = InboxMuted,
                fontSize = 10.sp,
                modifier = Modifier.clickable { refreshKey++ }.padding(4.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            val latestOwnStory = ownStories.maxByOrNull { it.createdAt }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp).clickable {
                    if (latestOwnStory != null) onOpenStories(userId) else imagePicker.launch("image/*")
                }
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (latestOwnStory != null) {
                        Box(
                            Modifier.size(66.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF28A9FF), Color(0xFF5D68EC), Color(0xFFB06BEF))
                                    ),
                                    CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                        ) {
                            Avatar(
                                initials = (myProfile?.displayName?.ifBlank { myProfile?.username.orEmpty() }
                                    ?: myProfile?.username.orEmpty()).take(1).uppercase().ifBlank { "Я" },
                                style = userId.hashCode().ushr(1) % 4,
                                modifier = Modifier.fillMaxSize(),
                                imageUrl = myProfile?.avatarUrl,
                                imageCacheKey = userId
                            )
                        }
                    } else {
                        Surface(
                            color = Color.White.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.96f)),
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                InboxCameraGlyph()
                            }
                        }
                    }
                    Surface(
                        color = InboxAccent,
                        shape = CircleShape,
                        border = BorderStroke(2.dp, Color(0xFFF2F3FF)),
                        modifier = Modifier.size(22.dp).clickable {
                            imagePicker.launch("image/*")
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (latestOwnStory != null) "Добавить ещё" else "Моя история",
                    color = InboxPrimary,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }

            friendGroups.forEach { story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(68.dp).clickable { onOpenStories(story.userId) }
                ) {
                    Box(
                        Modifier.size(62.dp).background(
                            Brush.linearGradient(listOf(Color(0xFF38A8FF), Color(0xFF766AF0), Color(0xFFB47BEF))),
                            CircleShape
                        ).padding(3.dp)
                    ) {
                        Avatar(
                            story.displayName.ifBlank { story.username }.take(1).uppercase(),
                            story.userId.hashCode().ushr(1) % 4,
                            Modifier.fillMaxSize(),
                            highlighted = true,
                            imageUrl = story.avatarUrl,
                            imageCacheKey = story.userId
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        story.displayName.ifBlank { story.username },
                        color = InboxPrimary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (loadError != null) {
            Text(
                loadError.orEmpty(),
                color = InboxMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 18.dp, top = 4.dp)
            )
        }
    }

    if (showComposer && selectedUri != null) {
        AlertDialog(
            onDismissRequest = { if (!isPublishing) showComposer = false },
            containerColor = PanelLight,
            title = { Text("Новая история", color = MainText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Фото будет видно только вашим друзьям и автоматически исчезнет через 24 часа.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        label = { Text("Подпись к истории") },
                        supportingText = { Text("${caption.length}/500") }
                    )
                    if (isPublishing) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentSlate
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isPublishing,
                    onClick = {
                        val uri = selectedUri ?: return@TextButton
                        scope.launch {
                            isPublishing = true
                            runCatching {
                                val prepared = withContext(Dispatchers.IO) {
                                    prepareChatImage(context, uri)
                                }
                                repository.publishStory(userId, prepared.bytes, caption).also { published ->
                                    withContext(Dispatchers.IO) {
                                        mediaCache.writeBytes("stories", userId, published.storyId, prepared.bytes)
                                    }
                                }
                            }.onSuccess { published ->
                                stories = listOf(published) + stories.filterNot { it.storyId == published.storyId }
                                withContext(Dispatchers.IO) { cache.saveStories(userId, stories) }
                                showComposer = false
                                selectedUri = null
                                refreshKey++
                                Toast.makeText(context, "История опубликована", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "Не удалось опубликовать историю.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isPublishing = false
                        }
                    }
                ) { Text(if (isPublishing) "Публикуем…" else "Опубликовать", color = AccentSlate) }
            },
            dismissButton = {
                TextButton(onClick = { if (!isPublishing) showComposer = false }) {
                    Text("Отмена", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun StoryImage(
    story: FaceVideoStoryCard,
    viewerUserId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fillWithBlurredCopy: Boolean = false
) {
    val context = LocalContext.current
    val mediaCache = remember(context) { ArturChatImageCache(context) }
    var bitmap by remember(viewerUserId, story.storyId) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(viewerUserId, story.storyId, story.mediaUrl) {
        val cachedBytes = withContext(Dispatchers.IO) {
            mediaCache.readBytes("stories", viewerUserId, story.storyId)
        }
        bitmap = cachedBytes?.let { decodeCachedImage(it, 1800) }

        if (story.mediaUrl.isNotBlank()) {
            val refreshedBytes = withContext(Dispatchers.IO) {
                runCatching {
                    mediaCache.fetchAndStore(
                        kind = "stories",
                        ownerId = viewerUserId,
                        mediaId = story.storyId,
                        sourceUrl = story.mediaUrl,
                        freshnessMillis = 15 * 60 * 1000L,
                        refreshWhenSourceChanges = false
                    )
                }.onFailure {
                    android.util.Log.w("ArturChatStories", "Could not refresh story image", it)
                }.getOrNull()
            }
            if (refreshedBytes != null) bitmap = decodeCachedImage(refreshedBytes, 1800)
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Box(modifier.fillMaxSize()) {
            if (fillWithBlurredCopy) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(34.dp).alpha(0.54f),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Color(0x66101928)))
            }
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "История",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    } else {
        Box(modifier.background(Color(0xFF293444)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = Color.White)
        }
    }
}

private fun decodeCachedImage(
    bytes: ByteArray,
    maxDimension: Int
): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxDimension) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

@Composable
private fun StoryViewerScreen(
    storyOwnerId: String,
    currentUserId: String,
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    onBack: () -> Unit
) {
    val cachedStories = remember(cache, currentUserId, storyOwnerId) {
        cache.readStories(currentUserId).filter { it.userId == storyOwnerId }.sortedBy { it.createdAt }
    }
    var stories by remember(storyOwnerId, currentUserId, cache) { mutableStateOf(cachedStories) }
    var index by remember(storyOwnerId) { mutableStateOf(0) }
    var isLoading by remember(storyOwnerId) { mutableStateOf(cachedStories.isEmpty()) }
    var errorMessage by remember(storyOwnerId) { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mediaCache = remember(context) { ArturChatImageCache(context) }

    LaunchedEffect(repository, storyOwnerId, currentUserId) {
        isLoading = stories.isEmpty()
        runCatching {
            val ownerStories = repository.listVisibleStories(storyOwnerId = storyOwnerId)
            stories = ownerStories.sortedBy { it.createdAt }
            index = 0
            errorMessage = null
            withContext(Dispatchers.IO) {
                val otherCachedStories = cache.readStories(currentUserId)
                    .filterNot { it.userId == storyOwnerId }
                cache.saveStories(currentUserId, otherCachedStories + ownerStories)
            }
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить историю."
            android.util.Log.w("ArturChatStories", "Could not refresh story viewer; keeping local cache", it)
        }
        isLoading = false
    }

    val current = stories.getOrNull(index)
    val canDelete = current?.userId == currentUserId
    val ownerName = current?.let { it.displayName.ifBlank { it.username } } ?: "История"
    val ownerInitial = ownerName.trim().take(1).uppercase().ifBlank { "?" }

    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111B2A), Color(0xFF202E42), Color(0xFF111A29))
                )
            )
    ) {
        // One decoded story image renders both the fitted foreground and the blurred ambient backdrop.
        if (current != null) {
            Crossfade(
                targetState = current.storyId,
                animationSpec = tween(240),
                label = "story-media-transition"
            ) { storyId ->
                val visibleStory = stories.firstOrNull { it.storyId == storyId }
                if (visibleStory != null) {
                    StoryImage(
                        story = visibleStory,
                        viewerUserId = currentUserId,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        fillWithBlurredCopy = true
                    )
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            // Top progress and creator controls use a restrained glass surface.
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE8101928), Color(0xAA101928), Color.Transparent)
                        )
                    )
                    .padding(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 18.dp)
            ) {
                if (stories.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { storyIndex, _ ->
                            Box(
                                Modifier.weight(1f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            storyIndex < index -> Color.White
                                            storyIndex == index -> Color(0xFFBFD7FF)
                                            else -> Color.White.copy(alpha = 0.32f)
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "‹",
                                color = Color.White,
                                fontSize = 29.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF39B4FF), Color(0xFF626FEF), Color(0xFFB276F0))
                                ),
                                CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        Avatar(
                            initials = ownerInitial,
                            style = storyOwnerId.hashCode().ushr(1) % 4,
                            modifier = Modifier.fillMaxSize(),
                            imageUrl = current?.avatarUrl,
                            imageCacheKey = storyOwnerId
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ownerName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            current?.let { storyTimeLabel(it.createdAt) }.orEmpty(),
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    if (canDelete) {
                        Surface(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !deleting,
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                        ) {
                            Text(
                                if (deleting) "Удаляем…" else "Удалить",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Загружаем историю…",
                                color = Color.White.copy(alpha = 0.76f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    current != null -> {
                        // Tap left/right on the media to navigate, as in modern story viewers.
                        Row(
                            Modifier.fillMaxSize()
                                .pointerInput(index, stories.size) {
                                    detectTapGestures { tap ->
                                        when {
                                            tap.x < size.width * 0.34f ->
                                                index = (index - 1).coerceAtLeast(0)
                                            tap.x > size.width * 0.66f -> {
                                                if (index < stories.lastIndex) index++ else onBack()
                                            }
                                        }
                                    }
                                }
                                .pointerInput(storyOwnerId) {
                                    var downwardDrag = 0f
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (downwardDrag > 110f) onBack()
                                            downwardDrag = 0f
                                        },
                                        onVerticalDrag = { _, dragAmount ->
                                            downwardDrag += dragAmount
                                        }
                                    )
                                }
                        ) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    else -> {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                errorMessage ?: "Истории пока нет или срок её действия истёк.",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = onBack,
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text("Вернуться", color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x99101928), Color(0xE8101928))
                        )
                    )
                    .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp)
            ) {
                if (current?.caption?.isNotBlank() == true) {
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            current.caption,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { index = (index - 1).coerceAtLeast(0) },
                        enabled = current != null && index > 0,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = if (index > 0) 0.15f else 0.07f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Text(
                            "Назад",
                            color = Color.White.copy(alpha = if (index > 0) 1f else 0.38f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Text(
                            if (stories.isEmpty()) "— / —" else "${index + 1} / ${stories.size}",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = {
                            if (index < stories.lastIndex) index++ else onBack()
                        },
                        enabled = current != null,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (index < stories.lastIndex) "Следующая" else "Готово",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("›", color = Color.White, fontSize = 17.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && current != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить историю?") },
            text = { Text("История исчезнет у всех друзей.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    scope.launch {
                        deleting = true
                        runCatching {
                            repository.deleteStory(
                                current.storyId,
                                current.mediaPath.ifBlank { "${current.userId}/${current.storyId}.jpg" }
                            )
                        }.onSuccess {
                            stories = stories.filterNot { it.storyId == current.storyId }
                            withContext(Dispatchers.IO) {
                                cache.saveStories(currentUserId, cache.readStories(currentUserId)
                                    .filterNot { it.storyId == current.storyId })
                                mediaCache.remove("stories", currentUserId, current.storyId)
                            }
                            index = (index - 1).coerceAtLeast(0)
                            if (stories.isEmpty()) onBack()
                        }
                            .onFailure { errorMessage = it.message ?: "Не удалось удалить историю." }
                        deleting = false
                    }
                }) { Text("Удалить", color = Color(0xFFA85F6E)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Отмена") } }
        )
    }
}

private fun storyTimeLabel(value: String): String = runCatching {
    val created = java.time.Instant.parse(value)
    val minutes = java.time.Duration.between(created, java.time.Instant.now()).toMinutes().coerceAtLeast(0)
    when {
        minutes < 1 -> "только что"
        minutes < 60 -> "${minutes} мин. назад"
        minutes < 24 * 60 -> "${minutes / 60} ч. назад"
        else -> "более суток назад"
    }
}.getOrDefault("История")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveChatRow(
    chat: FaceVideoChatSummary,
    unreadSummary: FaceVideoChatUnreadSummary?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(chat.id) { mutableStateOf(false) }
    var confirmDelete by remember(chat.id) { mutableStateOf(false) }
    val unreadCount = unreadSummary?.unreadCount?.coerceAtLeast(0)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
    val preview = buildChatInboxPreview(chat.lastMessagePreview, chat.lastMessageAt, unreadSummary)
    val initials = chat.title.trim().take(2).uppercase().ifBlank { "FV" }
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp)
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier.fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Avatar(
            initials,
            chat.id.hashCode().ushr(1) % 4,
            Modifier.size(52.dp),
            imageUrl = chat.peerAvatarUrl,
            imageCacheKey = chat.peerUserId
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, color = InboxPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(
                preview.text.ifBlank { "Начните переписку" },
                color = if (unreadCount > 0) InboxSecondary else InboxMuted,
                fontSize = 12.sp,
                fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                preview.timestamp?.let { formatChatMessageTime(it) }.orEmpty(),
                color = if (unreadCount > 0) InboxAccent else InboxMuted,
                fontSize = 10.sp
            )
            formatUnreadBadge(unreadCount)?.let { badgeText ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = InboxAccent,
                    shape = CircleShape,
                    modifier = Modifier.height(21.dp).widthIn(min = 21.dp)
                ) {
                    Box(
                        modifier = Modifier.height(21.dp).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
            Text("⋮", color = InboxMuted, fontSize = 18.sp, modifier = Modifier.clickable { showMenu = true }.padding(start = 8.dp))
        }
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(text = { Text("Удалить чат") }, onClick = { showMenu = false; confirmDelete = true })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить чат?") },
            text = { Text("Переписка исчезнет только из вашего списка. У второго участника чат останется.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Удалить", color = Color(0xFFA85F6E)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun NewChatDialog(
    repository: SupabaseChatRepository,
    currentUserId: String,
    onDismiss: () -> Unit,
    onOpenChat: (FaceVideoChatSummary) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<FaceVideoProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelLight,
        title = { Text("Новый чат", color = MainText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Введи псевдоним человека, с которым хочешь начать переписку.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(24)
                        result = null
                        message = null
                    },
                    singleLine = true,
                    label = { Text("Например, alex_24") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (result != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("@${result!!.username} · ${result!!.displayName}", color = AccentSlate, fontWeight = FontWeight.SemiBold)
                }
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(message.orEmpty(), color = Color(0xFFA85F6E), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && username.length >= 3,
                onClick = {
                    scope.launch {
                        isBusy = true
                        message = null
                        try {
                            val profile = result
                            if (profile == null) {
                                val found = repository.findUser(username)
                                if (found == null) {
                                    message = "Пользователь не найден. Проверь псевдоним."
                                } else if (found.id == currentUserId) {
                                    message = "Это твой собственный аккаунт."
                                } else {
                                    result = found
                                }
                            } else {
                                val chatId = repository.createDirectChat(profile.id)
                                val chat = repository.getChat(chatId)
                                if (chat == null) {
                                    message = "Чат создан, но пока не удалось загрузить его данные."
                                } else {
                                    onOpenChat(chat)
                                }
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Не удалось создать переписку."
                        } finally {
                            isBusy = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        isBusy -> "Подключение..."
                        result != null -> "Начать переписку"
                        else -> "Найти"
                    },
                    color = AccentSlate
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Muted) }
        }
    )
}

@Composable
private fun LiveChatScreen(
    chat: FaceVideoChatSummary,
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    userId: String,
    onlineUserIds: Set<String>,
    presenceAvailable: Boolean,
    onBack: () -> Unit,
    onMarkedRead: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onCall: (Boolean) -> Unit
) {
    val cachedMessages = remember(cache, userId, chat.id) {
        cache.readMessages(userId, chat.id)
    }
    val messagesFlow = remember(repository, chat.id) { repository.observeMessages(chat.id) }
    val messages by messagesFlow.collectAsState(initial = cachedMessages)
    var peerUserId by remember(repository, chat.id, userId) { mutableStateOf(chat.peerUserId) }
    var peerAvatarUrl by remember(repository, chat.id, userId) { mutableStateOf(chat.peerAvatarUrl) }
    LaunchedEffect(repository, chat.id, userId, chat.isGroup, chat.peerUserId, chat.peerAvatarUrl) {
        if (chat.isGroup) {
            peerUserId = null
            peerAvatarUrl = null
        } else {
            val resolvedPeerId = chat.peerUserId ?: runCatching {
                repository.getDirectChatPeerId(chat.id, userId)
            }.onFailure {
                android.util.Log.w("ArturChatPresence", "Could not resolve chat peer", it)
            }.getOrNull()
            peerUserId = resolvedPeerId
            peerAvatarUrl = chat.peerAvatarUrl ?: resolvedPeerId?.let { peerId ->
                runCatching { repository.getProfile(peerId)?.avatarUrl }
                    .onFailure {
                        android.util.Log.w("ArturChatAvatar", "Could not load peer avatar", it)
                    }
                    .getOrNull()
            }
        }
    }
    var locallySentMessages by remember(chat.id) { mutableStateOf<List<com.facevideo.app.data.FaceVideoMessage>>(emptyList()) }
    LaunchedEffect(messages) {
        if (locallySentMessages.isNotEmpty()) {
            val serverIds = messages.mapTo(mutableSetOf()) { it.id }
            locallySentMessages = locallySentMessages.filterNot { it.id in serverIds }
        }
    }
    val receiptsFlow = remember(repository, chat.id) { repository.observeMessageReceipts(chat.id) }
    val receipts by receiptsFlow.collectAsState(initial = emptyList())
    var hiddenMessageIds by remember(chat.id) { mutableStateOf<Set<String>>(emptySet()) }
    // Realtime rows can arrive in primary-key order rather than message time order.
    // Always render the timeline chronologically so every new message stays at the bottom.
    val orderedMessages = remember(messages, locallySentMessages, hiddenMessageIds) {
        mergeChatMessageSnapshots(messages, locallySentMessages).filterNot { it.id in hiddenMessageIds }
    }
    LaunchedEffect(userId, chat.id, orderedMessages) {
        withContext(Dispatchers.IO) {
            cache.saveMessages(userId, chat.id, orderedMessages)
        }
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var replyingTo by remember(chat.id) { mutableStateOf<com.facevideo.app.data.FaceVideoMessage?>(null) }
    var selectedMessageIds by remember(chat.id) { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteSelection by remember { mutableStateOf(false) }
    var showForwardPicker by remember { mutableStateOf(false) }
    var forwardTargets by remember { mutableStateOf<List<com.facevideo.app.data.FaceVideoChatSummary>>(emptyList()) }
    var actionBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val voiceRecorder = remember(chat.id) { ChatVoiceRecorder(context.cacheDir) }
    var isVoiceRecording by remember(chat.id) { mutableStateOf(false) }
    var voiceRecordingStartedAt by remember(chat.id) { mutableStateOf(0L) }
    var voiceRecordingDurationMs by remember(chat.id) { mutableStateOf(0L) }
    val voiceAmplitudeSamples = remember(chat.id) { mutableStateListOf<Int>() }

    DisposableEffect(chat.id, voiceRecorder) {
        onDispose { voiceRecorder.cancel() }
    }

    LaunchedEffect(chat.id, orderedMessages.lastOrNull()?.id) {
        runCatching { repository.markChatRead(chat.id) }
            .onSuccess { onMarkedRead() }
            .onFailure {
                android.util.Log.w("ArturChatUnread", "Could not mark chat as read", it)
            }
    }

    LaunchedEffect(orderedMessages.lastOrNull()?.id, orderedMessages.size) {
        if (orderedMessages.isNotEmpty()) {
            listState.animateScrollToItem(orderedMessages.lastIndex)
        }
    }

    fun sendMessage() {
        val text = draft.trim()
        if (text.isBlank() || isSending) return
        scope.launch {
            isSending = true
            errorMessage = null
            try {
                val persistedMessage = repository.sendText(
                    chatId = chat.id,
                    text = text,
                    replyToMessageId = replyingTo?.id
                )
                locallySentMessages = mergeChatMessageSnapshots(
                    locallySentMessages,
                    listOf(persistedMessage)
                )
                draft = ""
                replyingTo = null
            } catch (error: Exception) {
                errorMessage = error.message ?: "Не удалось отправить сообщение."
            } finally {
                isSending = false
            }
        }
    }

    fun sendImage(uri: Uri) {
        if (isSending) return
        scope.launch {
            isSending = true
            errorMessage = null
            var stage = "Подготовка выбранного изображения"
            try {
                val prepared = withContext(Dispatchers.IO) {
                    prepareChatImage(context, uri)
                }
                stage = "Загрузка изображения в защищённое хранилище"
                val path = repository.uploadChatImage(
                    chatId = chat.id,
                    senderId = userId,
                    bytes = prepared.bytes
                )
                stage = "Сохранение сообщения в переписке"
                val persistedMessage = repository.sendImage(
                    chatId = chat.id,
                    imagePath = path,
                    fileName = prepared.fileName,
                    fileSize = prepared.bytes.size.toLong(),
                    replyToMessageId = replyingTo?.id
                )
                locallySentMessages = mergeChatMessageSnapshots(
                    locallySentMessages,
                    listOf(persistedMessage)
                )
                replyingTo = null
                Toast.makeText(context, "Фото отправлено", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                android.util.Log.e("ArturChatMedia", "Image send failed at stage: $stage", error)
                errorMessage = "$stage: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                isSending = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) sendImage(uri)
    }

    fun startVoiceRecording() {
        if (isSending || isVoiceRecording) return
        try {
            voiceRecorder.start()
            voiceRecordingStartedAt = SystemClock.elapsedRealtime()
            voiceRecordingDurationMs = 0L
            voiceAmplitudeSamples.clear()
            isVoiceRecording = true
            errorMessage = null
        } catch (error: Exception) {
            android.util.Log.e("ArturChatVoice", "Could not start recording", error)
            errorMessage = error.message ?: "Не удалось начать запись. Проверьте доступ к микрофону."
        }
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecording()
        else errorMessage = "Для записи голосовых сообщений нужен доступ к микрофону."
    }

    fun cancelVoiceRecording() {
        if (!isVoiceRecording) return
        isVoiceRecording = false
        voiceRecorder.cancel()
        voiceRecordingDurationMs = 0L
        voiceAmplitudeSamples.clear()
    }

    fun sendVoiceRecording() {
        if (!isVoiceRecording || isSending) return
        val durationMs = (SystemClock.elapsedRealtime() - voiceRecordingStartedAt).coerceAtLeast(0L)
        val waveform = buildVoiceWaveform(voiceAmplitudeSamples.toList())
        isVoiceRecording = false
        voiceRecordingDurationMs = durationMs
        isSending = true
        errorMessage = null
        scope.launch {
            var recordedFile: File? = null
            try {
                val completedFile = withContext(Dispatchers.IO) { voiceRecorder.stop() }
                recordedFile = completedFile
                require(durationMs >= 1_000L) { "Запишите голосовое сообщение хотя бы одну секунду." }
                val audioBytes = withContext(Dispatchers.IO) { completedFile.readBytes() }
                val audioPath = repository.uploadChatVoice(chat.id, userId, audioBytes)
                val persistedMessage = repository.sendVoiceMessage(
                    chatId = chat.id,
                    audioPath = audioPath,
                    fileName = "voice-message.m4a",
                    fileSize = audioBytes.size.toLong(),
                    durationMs = durationMs,
                    waveform = waveform,
                    replyToMessageId = replyingTo?.id
                )
                locallySentMessages = mergeChatMessageSnapshots(locallySentMessages, listOf(persistedMessage))
                replyingTo = null
                Toast.makeText(context, "Голосовое сообщение отправлено", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                android.util.Log.e("ArturChatVoice", "Voice send failed", error)
                errorMessage = error.message ?: "Не удалось отправить голосовое сообщение."
            } finally {
                withContext(Dispatchers.IO) { recordedFile?.delete() }
                isSending = false
                voiceRecordingDurationMs = 0L
                voiceAmplitudeSamples.clear()
            }
        }
    }

    LaunchedEffect(isVoiceRecording, voiceRecordingStartedAt) {
        if (isVoiceRecording) {
            while (isVoiceRecording) {
                val elapsed = (SystemClock.elapsedRealtime() - voiceRecordingStartedAt).coerceAtLeast(0L)
                voiceRecordingDurationMs = elapsed
                voiceAmplitudeSamples.add(voiceRecorder.readAmplitudeLevel())
                if (elapsed >= MAX_CHAT_VOICE_DURATION_MS) {
                    sendVoiceRecording()
                    break
                }
                delay(100L)
            }
        }
    }


    Column(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE2DCFF),
                        Color(0xFFE2E9FF),
                        Color(0xFFE8F0FF),
                        Color(0xFFF2F5FF),
                        Color(0xFFE8E9FF)
                    )
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val topLeft = Offset(-w * 0.12f, h * 0.08f)
                val topRight = Offset(w * 1.04f, h * 0.22f)
                val center = Offset(w * 0.48f, h * 0.62f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFB49BFF).copy(alpha = 0.42f), Color.Transparent),
                        center = topLeft,
                        radius = w * 0.86f
                    ),
                    radius = w * 0.86f,
                    center = topLeft
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF9BCBFF).copy(alpha = 0.40f), Color.Transparent),
                        center = topRight,
                        radius = w * 0.78f
                    ),
                    radius = w * 0.78f,
                    center = topRight
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFD0B8FF).copy(alpha = 0.24f), Color.Transparent),
                        center = center,
                        radius = w * 0.78f
                    ),
                    radius = w * 0.78f,
                    center = center
                )
            }
            .imePadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
            color = Color.White.copy(alpha = 0.34f),
            shape = RoundedCornerShape(23.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.34f),
                                Color(0xFFE5ECFF).copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.38f)
                            )
                        )
                    )
                    .padding(horizontal = 7.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedMessageIds.isNotEmpty()) {
                    ChatHeaderIconButton(onClick = { selectedMessageIds = emptySet() }) {
                        Text("×", color = AccentSlate, fontSize = 29.sp, fontWeight = FontWeight.Light)
                    }
                    Text(
                        "${selectedMessageIds.size} выбрано",
                        color = MainText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp
                    )
                } else {
                    ChatHeaderIconButton(onClick = onBack) {
                        InboxBackGlyph()
                    }
                    Spacer(Modifier.width(5.dp))
                    Avatar(
                        chat.title.take(2).uppercase(),
                        chat.id.hashCode().ushr(1) % 4,
                        Modifier.size(45.dp).clickable(enabled = peerUserId != null && !chat.isGroup) {
                            peerUserId?.let(onOpenProfile)
                        },
                        imageUrl = peerAvatarUrl,
                        imageCacheKey = peerUserId
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        Modifier.weight(1f).clickable(enabled = peerUserId != null && !chat.isGroup) {
                            peerUserId?.let(onOpenProfile)
                        }
                    ) {
                        Text(
                            chat.title,
                            color = InboxPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        val peerIsOnline = peerUserId != null && peerUserId in onlineUserIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(6.dp).background(
                                    if (peerIsOnline) InboxOnline else InboxMuted.copy(alpha = 0.65f),
                                    CircleShape
                                )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                when {
                                    chat.isGroup -> "Групповой чат"
                                    peerUserId == null -> "Проверяем статус…"
                                    peerIsOnline -> "В сети"
                                    !presenceAvailable -> "Статус обновляется…"
                                    else -> "Не в сети"
                                },
                                color = if (peerIsOnline) InboxOnline else InboxMuted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    InboxHeaderActionButton(onClick = { onCall(false) }) {
                        ChatCallGlyph(video = false, tint = InboxAccentStrong)
                    }
                    Spacer(Modifier.width(5.dp))
                    InboxHeaderActionButton(onClick = { onCall(true) }) {
                        ChatCallGlyph(video = true, tint = InboxAccentStrong)
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.94f), Color(0xFFD6DDF0), Color.Transparent)
                )
            )
        )

        if (orderedMessages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Это начало вашей переписки. Напиши первое сообщение.", color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(orderedMessages, key = { _, message -> message.id }) { index, message ->
                    val startsNewSenderGroup = index > 0 &&
                        orderedMessages[index - 1].senderId != message.senderId
                    val followsSameSender = index > 0 && !startsNewSenderGroup
                    val repliedMessage = message.replyToMessageId
                        ?.let { replyId -> orderedMessages.firstOrNull { it.id == replyId } }
                    val replyPreview = repliedMessage?.let { original ->
                        MessageReplyPreview(
                            sender = if (original.senderId == userId) "Вы"
                                else if (chat.isGroup) "Участник" else chat.title,
                            text = original.body
                        )
                    }
                    MessageBubble(
                        message = ChatMessage(
                            id = message.id,
                            text = if (message.deletedAt != null) "Это сообщение удалено" else message.body,
                            time = formatChatMessageTime(message.createdAt),
                            isMine = message.senderId == userId,
                            deliveryStatus = if (message.senderId == userId && message.deletedAt == null) resolveMessageDeliveryStatus(message.id, receipts) else null,
                            forwardedFromName = message.forwardedFromName,
                            isDeleted = message.deletedAt != null,
                            attachmentMimeType = if (message.deletedAt == null) message.attachmentMimeType else null,
                            attachmentDurationMs = if (message.deletedAt == null) message.attachmentDurationMs else null,
                            attachmentWaveform = if (message.deletedAt == null) message.attachmentWaveform else null
                        ),
                        selected = message.id in selectedMessageIds,
                        onSelect = if (message.deletedAt == null) ({ selectedMessageIds = selectedMessageIds + message.id }) else null,
                        onToggleSelection = if (message.deletedAt == null && selectedMessageIds.isNotEmpty()) ({ selectedMessageIds = if (message.id in selectedMessageIds) selectedMessageIds - message.id else selectedMessageIds + message.id }) else null,
                        showTime = shouldShowChatMessageTime(orderedMessages, index),
                        separateFromPrevious = startsNewSenderGroup,
                        sameSenderPrevious = followsSameSender,
                        replyPreview = replyPreview,
                        attachmentPath = if (message.deletedAt == null) message.attachmentPath else null,
                        attachmentRepository = repository,
                        onReply = if (message.deletedAt == null) ({ replyingTo = message }) else null,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ArturChat", message.body))
                            Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                        },
                        onReplyPreviewClick = repliedMessage?.let { original ->
                            {
                                val targetIndex = orderedMessages.indexOfFirst { it.id == original.id }
                                if (targetIndex >= 0) {
                                    scope.launch { listState.animateScrollToItem(targetIndex) }
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showDeleteSelection) {
            val selectedRows = orderedMessages.filter { it.id in selectedMessageIds }
            val allMine = selectedRows.isNotEmpty() && selectedRows.all { it.senderId == userId }
            AlertDialog(
                onDismissRequest = { showDeleteSelection = false },
                title = { Text("Удалить сообщения?") },
                text = { Text("Выбрано: ${selectedRows.size}. Можно скрыть сообщения только у себя.") },
                confirmButton = {
                    Column {
                        TextButton(onClick = {
                            scope.launch {
                                actionBusy = true
                                runCatching { repository.hideMessagesForMe(selectedMessageIds.toList(), userId) }
                                    .onSuccess { hiddenMessageIds = hiddenMessageIds + selectedMessageIds; selectedMessageIds = emptySet(); showDeleteSelection = false }
                                    .onFailure { errorMessage = it.message ?: "Не удалось удалить сообщения." }
                                actionBusy = false
                            }
                        }, enabled = !actionBusy) { Text("Удалить у меня", color = AccentSlate) }
                        if (allMine) TextButton(onClick = {
                            scope.launch {
                                actionBusy = true
                                runCatching { selectedRows.forEach { repository.deleteMessageForEveryone(it.id, userId) } }
                                    .onSuccess { selectedMessageIds = emptySet(); showDeleteSelection = false }
                                    .onFailure { errorMessage = it.message ?: "Не удалось удалить у всех." }
                                actionBusy = false
                            }
                        }, enabled = !actionBusy) { Text("Удалить у всех", color = Color(0xFFA85F6E)) }
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteSelection = false }) { Text("Отмена") } }
            )
        }
        if (showForwardPicker) {
            AlertDialog(
                onDismissRequest = { showForwardPicker = false },
                title = { Text("Переслать в чат") },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        if (forwardTargets.isEmpty()) Text("Нет других чатов", color = Muted)
                        forwardTargets.forEach { target ->
                            TextButton(onClick = {
                                scope.launch {
                                    actionBusy = true
                                    runCatching {
                                        orderedMessages.filter { it.id in selectedMessageIds }.forEach { source ->
                                            repository.forwardMessage(
                                                source, target.id, userId,
                                                if (source.senderId == userId) "Вы" else chat.title
                                            )
                                        }
                                    }.onSuccess {
                                        Toast.makeText(context, "Сообщения пересланы", Toast.LENGTH_SHORT).show()
                                        selectedMessageIds = emptySet()
                                        showForwardPicker = false
                                    }.onFailure { errorMessage = it.message ?: "Не удалось переслать сообщения." }
                                    actionBusy = false
                                }
                            }, enabled = !actionBusy) { Text(target.title, color = MainText) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showForwardPicker = false }) { Text("Закрыть") } }
            )
        }

        if (errorMessage != null) {
            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (selectedMessageIds.isNotEmpty()) {
            val selectedRows = orderedMessages.filter { it.id in selectedMessageIds }
            Surface(
                color = Color.White.copy(alpha = 0.90f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f)),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedRows.size == 1) {
                        SelectionActionButton(
                            icon = "↩",
                            label = "Ответить",
                            color = AccentSlate,
                            modifier = Modifier.weight(1f)
                        ) {
                            replyingTo = selectedRows.single()
                            selectedMessageIds = emptySet()
                        }
                    }
                    SelectionActionButton(
                        icon = "▢",
                        label = "Копировать",
                        color = AccentSlate,
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedText = selectedRows.joinToString("\n") { it.body }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ArturChat", selectedText))
                        Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                        selectedMessageIds = emptySet()
                    }
                    SelectionActionButton(
                        icon = "➦",
                        label = "Переслать",
                        color = AccentSlate,
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch {
                            actionBusy = true
                            forwardTargets = runCatching {
                                repository.listMyChats(userId).filter { it.id != chat.id }
                            }.getOrDefault(emptyList())
                            actionBusy = false
                            showForwardPicker = true
                        }
                    }
                    SelectionActionButton(
                        icon = "⌫",
                        label = "Удалить",
                        color = Color(0xFFA85F6E),
                        modifier = Modifier.weight(1f)
                    ) {
                        showDeleteSelection = true
                    }
                }
            }
        }

        if (replyingTo != null) {
            val selectedReply = replyingTo!!
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(12.dp)).background(PanelLight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(AccentSlate))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedReply.senderId == userId) "Ответ себе"
                        else "Ответ: " + if (chat.isGroup) "участнику" else chat.title,
                        color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(selectedReply.body, color = MainText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "×",
                    color = Muted,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { replyingTo = null }.padding(start = 10.dp)
                )
            }
        }

        if (isSending) {
            Text(
                "Отправляем сообщение или аудио…",
                color = InboxMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        if (isVoiceRecording) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                color = Color.White.copy(alpha = 0.34f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
                shadowElevation = 3.dp
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.34f), Color(0xFFE8EDFF).copy(alpha = 0.30f), Color.White.copy(alpha = 0.40f))))
                        .padding(horizontal = 7.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.66f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f))
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFDF718D)))
                                Spacer(Modifier.width(8.dp))
                                Text("Запись голоса", color = InboxPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text(formatVoiceDuration(voiceRecordingDurationMs), color = InboxAccentStrong, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            VoiceWaveformBars(
                                waveform = buildVoiceWaveform(voiceAmplitudeSamples.toList()),
                                progress = 1f,
                                modifier = Modifier.fillMaxWidth().height(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    InboxHeaderActionButton(onClick = { cancelVoiceRecording() }) {
                        Text("×", color = InboxAccentStrong, fontSize = 25.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.width(7.dp))
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))))
                            .border(1.dp, Color.White.copy(alpha = 0.62f), CircleShape)
                            .clickable(enabled = !isSending) { sendVoiceRecording() },
                        contentAlignment = Alignment.Center
                    ) { ChatSendGlyph() }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                color = Color.White.copy(alpha = 0.34f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
                shadowElevation = 3.dp
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.34f), Color(0xFFE8EDFF).copy(alpha = 0.30f), Color.White.copy(alpha = 0.40f))))
                        .padding(horizontal = 7.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InboxHeaderActionButton(onClick = { if (!isSending) imagePicker.launch("image/*") }) {
                        InboxCameraGlyph()
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.66f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f))
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                            textStyle = TextStyle(color = InboxPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (draft.isEmpty()) Text("Сообщение…", color = InboxMuted, fontSize = 13.sp)
                                    inner()
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    InboxHeaderActionButton(onClick = {
                        if (!isSending) {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startVoiceRecording()
                            } else {
                                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }) { ChatMicrophoneGlyph() }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(
                                if (isSending || draft.isBlank()) Brush.horizontalGradient(listOf(Color(0xFF9AA6C8), Color(0xFFB0B9D4)))
                                else Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8)))
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.62f), CircleShape)
                            .clickable(enabled = !isSending && draft.isNotBlank()) { sendMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp
                            )
                        } else ChatSendGlyph()
                    }
                }
            }
        }
    }
}


@Composable
private fun ChatListScreen(onOpenChat: (ChatSummary) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        com.facevideo.app.model.filterChats(DemoData.chats, query)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ARTURCHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text("Сообщения", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = Color(0xFFE5EAF0),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFF9EAFC5))
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccentSlate))
                    Spacer(Modifier.width(7.dp))
                    Text("DEMO", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        SearchField(value = query, onValueChange = { query = it })

        Text(
            "БЫСТРЫЙ ДОСТУП",
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 12.dp)
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            DemoData.chats.filter { it.online }.forEach { chat ->
                Column(
                    Modifier.width(62.dp).clickable { onOpenChat(chat) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(chat.initials, chat.avatarStyle, Modifier.size(54.dp), highlighted = true)
                    Spacer(Modifier.height(7.dp))
                    Text(chat.title.substringBefore(" "), color = MainText, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Недавние", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(filtered.size.toString() + " диалогов", color = Muted, fontSize = 11.sp)
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                ChatRow(chat = chat, onClick = { onOpenChat(chat) })
            }
            if (filtered.isEmpty()) {
                item {
                    Text("Ничего не найдено", color = Muted, modifier = Modifier.fillMaxWidth().padding(28.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, compact: Boolean = false, placeholder: String = "Поиск по чатам") {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 18.dp else 20.dp, vertical = if (compact) 2.dp else 0.dp).heightIn(min = if (compact) 46.dp else 0.dp),
        color = if (compact) InboxSurface else Panel,
        shape = RoundedCornerShape(if (compact) 13.dp else 16.dp),
        border = BorderStroke(1.dp, if (compact) InboxBorder else Color(0xFFC9D3DF))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = if (compact) InboxPrimary else MainText, fontSize = if (compact) 13.sp else 14.sp),
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (compact) 13.dp else 15.dp,
                vertical = if (compact) 12.dp else 14.dp
            ),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", color = if (compact) InboxAccent else AccentSlate, fontSize = if (compact) 19.sp else 22.sp)
                    Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, color = if (compact) InboxMuted else Muted, fontSize = if (compact) 12.sp else 14.sp)
                        inner()
                    }
                }
            }
        )
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .background(Color(0xFFF4F6FF)).padding(horizontal = 11.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.initials, chat.avatarStyle, Modifier.size(53.dp), highlighted = chat.online)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (chat.online) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AccentSlate))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(chat.preview, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.time, color = Muted, fontSize = 10.sp)
            if (chat.unreadCount > 0) {
                Spacer(Modifier.height(7.dp))
                Box(Modifier.size(21.dp).clip(CircleShape).background(AccentSlate), contentAlignment = Alignment.Center) {
                    Text(chat.unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Avatar(
    initials: String,
    style: Int,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    imageUrl: String? = null,
    imageCacheKey: String? = null
) {
    val context = LocalContext.current
    val imageCache = remember(context) { ArturChatImageCache(context) }
    val stableCacheKey = imageCacheKey?.takeIf { it.isNotBlank() }
        ?: imageUrl?.takeIf { it.isNotBlank() }
    var avatarBitmap by remember(stableCacheKey) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(imageUrl, stableCacheKey) {
        if (imageUrl.isNullOrBlank() || stableCacheKey.isNullOrBlank()) {
            avatarBitmap = null
            return@LaunchedEffect
        }

        val cachedBytes = withContext(Dispatchers.IO) {
            imageCache.readBytes("avatars", "public", stableCacheKey)
        }
        if (cachedBytes != null) avatarBitmap = decodeCachedImage(cachedBytes, 512)

        val freshBytes = withContext(Dispatchers.IO) {
            runCatching {
                imageCache.fetchAndStore(
                    kind = "avatars",
                    ownerId = "public",
                    mediaId = stableCacheKey,
                    sourceUrl = imageUrl,
                    freshnessMillis = 24 * 60 * 60 * 1000L,
                    refreshWhenSourceChanges = true
                )
            }.onFailure {
                android.util.Log.w("ArturChatAvatar", "Avatar cache refresh failed", it)
            }.getOrNull()
        }
        if (freshBytes != null) avatarBitmap = decodeCachedImage(freshBytes, 512)
    }
    Box(
        modifier.clip(CircleShape).background(
            Brush.linearGradient(listOf(AvatarColors[style % AvatarColors.size], PanelLight))
        ),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = avatarBitmap
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "Фото профиля", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(initials, color = MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (highlighted) {
            Box(Modifier.align(Alignment.BottomEnd).padding(1.dp).size(10.dp).clip(CircleShape).background(AccentSlate))
        }
    }
}

@Composable
private fun ChatScreen(
    chat: ChatSummary,
    messages: SnapshotStateList<ChatMessage>,
    onBack: () -> Unit,
    onCall: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatHeaderIconButton(onClick = onBack) {
                InboxBackGlyph()
            }
            Spacer(Modifier.width(8.dp))
            Avatar(chat.initials, chat.avatarStyle, Modifier.size(43.dp), highlighted = chat.online)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = MainText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (chat.online) "в сети" else "был(а) недавно", color = if (chat.online) AccentSlate else Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(7.dp))
            ChatHeaderIconButton(onClick = onCall) {
                ChatCallGlyph(video = false)
            }
            Spacer(Modifier.width(5.dp))
            ChatHeaderIconButton(onClick = onCall) {
                ChatCallGlyph(video = true)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.94f), Color(0xFFD6DDF0), Color.Transparent)
                )
            )
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                Text(
                    "СЕГОДНЯ",
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InboxHeaderActionButton(onClick = {}) {
                Text("+", color = InboxAccentStrong, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                Modifier.weight(1f),
                color = Panel,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFD6DDF0))
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (draft.isNotBlank()) {
                            messages.add(ChatMessage(UUID.randomUUID().toString(), draft.trim(), "сейчас", true))
                            draft = ""
                        }
                    }),
                    textStyle = TextStyle(color = MainText, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("Сообщение...", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(9.dp))
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))))
                    .clickable {
                        if (draft.isNotBlank()) {
                            messages.add(ChatMessage(UUID.randomUUID().toString(), draft.trim(), "сейчас", true))
                            draft = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatImagePreview(
    path: String,
    repository: SupabaseChatRepository
) {
    var bitmap by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    var showFullImage by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, repository) {
        bitmap = null
        failed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.downloadChatAttachment(path)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sampleSize = 1
                while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 1400) {
                    sampleSize *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                    ?: throw IllegalStateException("Не удалось декодировать фото.")
            }
        }.onSuccess {
            bitmap = it
        }.onFailure {
            android.util.Log.w("ArturChatAttachments", "Image download failed", it)
            failed = true
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        val aspectRatio = (loaded.width.toFloat() / loaded.height.toFloat()).coerceIn(0.6f, 1.8f)
        Image(
            bitmap = loaded,
            contentDescription = "Фотография",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 230.dp)
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(11.dp))
                .clickable { showFullImage = true }
        )
        if (showFullImage) {
            Dialog(onDismissRequest = { showFullImage = false }) {
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = loaded,
                        contentDescription = "Фотография",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 680.dp)
                            .clickable { showFullImage = false }
                    )
                }
            }
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (failed) Color(0xFFF5D2D0) else Color(0xFFE5EAF0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (failed) "Фото недоступно" else "Загружаем фото…",
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VoiceWaveformBars(
    waveform: List<Int>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val bars = remember(waveform) {
        if (waveform.size in 16..96) waveform.map { it.coerceIn(8, 100) }
        else List(64) { 14 }
    }
    val safeProgress = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        if (bars.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas
        val step = size.width / bars.size
        val strokeWidth = (step * 0.54f).coerceIn(1.6.dp.toPx(), 3.4.dp.toPx())
        val playedColor = Color(0xFF596FE3)
        val futureColor = Color(0xFFC8D3EC)
        bars.forEachIndexed { index, level ->
            val fraction = level / 100f
            val barHeight = (size.height * (0.18f + fraction * 0.82f))
                .coerceIn(3.dp.toPx(), size.height * 0.96f)
            val x = step * (index + 0.5f)
            val top = (size.height - barHeight) / 2f
            drawLine(
                color = if ((index + 0.5f) / bars.size <= safeProgress) playedColor else futureColor,
                start = Offset(x, top),
                end = Offset(x, top + barHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        if (safeProgress > 0f && safeProgress < 1f) {
            drawCircle(
                color = Color(0xFF5267D8),
                radius = 2.6.dp.toPx(),
                center = Offset(size.width * safeProgress, size.height / 2f)
            )
        }
    }
}

@Composable
private fun ChatVoiceMessagePlayer(
    path: String,
    repository: SupabaseChatRepository,
    durationMs: Long,
    waveform: List<Int>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerState = remember(path) { mutableStateOf<MediaPlayer?>(null) }
    val audioFileState = remember(path) { mutableStateOf<File?>(null) }
    var isLoading by remember(path) { mutableStateOf(false) }
    var isPrepared by remember(path) { mutableStateOf(false) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var playbackFailed by remember(path) { mutableStateOf(false) }
    var playbackPositionMs by remember(path) { mutableStateOf(0L) }
    val safeDurationMs = durationMs.coerceAtLeast(0L)

    fun togglePlayback() {
        val currentPlayer = playerState.value
        if (currentPlayer != null && isPrepared) {
            runCatching {
                if (isPlaying) {
                    currentPlayer.pause()
                    isPlaying = false
                } else {
                    if (safeDurationMs > 0L && playbackPositionMs >= safeDurationMs) {
                        currentPlayer.seekTo(0)
                        playbackPositionMs = 0L
                    }
                    currentPlayer.start()
                    isPlaying = true
                }
            }.onFailure {
                playbackFailed = true
                isPlaying = false
            }
            return
        }
        if (isLoading) return
        playbackFailed = false
        isLoading = true
        scope.launch {
            var createdPlayer: MediaPlayer? = null
            try {
                val audioBytes = withContext(Dispatchers.IO) { repository.downloadChatAttachment(path) }
                val audioFile = withContext(Dispatchers.IO) {
                    File.createTempFile("arturchat-voice-", ".m4a", context.cacheDir).apply { writeBytes(audioBytes) }
                }
                audioFileState.value?.delete()
                audioFileState.value = audioFile
                val mediaPlayer = MediaPlayer()
                createdPlayer = mediaPlayer
                playerState.value = mediaPlayer
                mediaPlayer.setOnPreparedListener { preparedPlayer ->
                    isPrepared = true
                    isLoading = false
                    playbackPositionMs = 0L
                    runCatching { preparedPlayer.start(); isPlaying = true }.onFailure {
                        playbackFailed = true
                        isPlaying = false
                    }
                }
                mediaPlayer.setOnCompletionListener { completedPlayer ->
                    isPlaying = false
                    playbackPositionMs = 0L
                    runCatching { completedPlayer.seekTo(0) }
                }
                mediaPlayer.setOnErrorListener { failedPlayer, _, _ ->
                    isLoading = false
                    isPlaying = false
                    isPrepared = false
                    playbackFailed = true
                    runCatching { failedPlayer.release() }
                    if (playerState.value === failedPlayer) playerState.value = null
                    true
                }
                mediaPlayer.setDataSource(audioFile.absolutePath)
                mediaPlayer.prepareAsync()
            } catch (error: Exception) {
                runCatching { createdPlayer?.release() }
                playerState.value = null
                audioFileState.value?.delete()
                audioFileState.value = null
                isLoading = false
                playbackFailed = true
                android.util.Log.w("ArturChatVoice", "Voice playback failed", error)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playbackPositionMs = runCatching { playerState.value?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
            delay(120L)
        }
    }
    DisposableEffect(path) {
        onDispose {
            runCatching { playerState.value?.release() }
            audioFileState.value?.delete()
        }
    }

    val progress = if (safeDurationMs > 0L) {
        (playbackPositionMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))))
                    .border(1.dp, Color.White.copy(alpha = 0.90f), CircleShape)
                    .clickable(enabled = !isLoading) { togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else ChatPlaybackGlyph(isPlaying)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Голосовое сообщение", color = InboxPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(formatVoiceDuration(safeDurationMs), color = InboxAccentStrong, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                VoiceWaveformBars(
                    waveform = waveform,
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(31.dp)
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    formatVoiceDuration(playbackPositionMs) + " / " + formatVoiceDuration(safeDurationMs),
                    color = InboxMuted,
                    fontSize = 9.sp
                )
            }
        }
        if (playbackFailed) {
            Text("Не удалось воспроизвести аудио", color = Color(0xFFB85E79), fontSize = 9.sp)
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectionActionButton(
    icon: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, color = color, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = color,
            fontSize = 9.sp,
            maxLines = 1,
            softWrap = false,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    showTime: Boolean = true,
    separateFromPrevious: Boolean = false,
    sameSenderPrevious: Boolean = false,
    replyPreview: MessageReplyPreview? = null,
    attachmentPath: String? = null,
    attachmentRepository: SupabaseChatRepository? = null,
    onReply: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onReplyPreviewClick: (() -> Unit)? = null,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    onToggleSelection: (() -> Unit)? = null
) {
    var showActions by remember(message.id) { mutableStateOf(false) }
    val deliveryTicks = if (message.isMine && showTime) {
        when (message.deliveryStatus) {
            MessageDeliveryStatus.SENT -> 1 to Muted
            MessageDeliveryStatus.DELIVERED -> 2 to Muted
            MessageDeliveryStatus.READ -> 2 to Color(0xFF5D6C84)
            null -> null
        }
    } else null
    val messageText = message.text
    val isVoiceAttachment = message.attachmentMimeType?.startsWith("audio/") == true && attachmentPath != null

    Row(
        Modifier.fillMaxWidth().padding(
            top = when {
                separateFromPrevious -> 8.dp
                sameSenderPrevious -> 3.dp
                else -> 0.dp
            }
        ),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box {
            Surface(
                // Delivery ticks are inline with the final text line, so they cannot create a separate row.
                modifier = Modifier
                    .then(if (isVoiceAttachment) Modifier.widthIn(min = 250.dp, max = 310.dp) else Modifier.widthIn(max = 310.dp))
                    .combinedClickable(
                        enabled = onSelect != null || onToggleSelection != null || onReply != null || onCopy != null,
                        onClick = { if (selected || onToggleSelection != null) onToggleSelection?.invoke() },
                        onLongClick = { if (onSelect != null) onSelect() else showActions = true }
                    ),
                color = when {
                    selected -> Color(0xFF879BCB)
                    message.isMine -> Color(0xE9E4EBFF)
                    else -> Color(0xEAFBFCFF)
                },
                border = BorderStroke(
                    if (selected) 1.4.dp else 1.dp,
                    when {
                        selected -> Color(0xFF607BB9)
                        message.isMine -> Color(0xFFBFCBFA).copy(alpha = 0.82f)
                        else -> Color.White.copy(alpha = 0.92f)
                    }
                ),
                shadowElevation = if (selected) 3.dp else 1.dp,
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (message.isMine) 18.dp else 5.dp,
                    bottomEnd = if (message.isMine) 5.dp else 18.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = if (isVoiceAttachment) 12.dp else 14.dp,
                        end = if (isVoiceAttachment) 12.dp else 14.dp,
                        top = if (isVoiceAttachment) 10.dp else 7.dp,
                        bottom = if (showTime) 5.dp else 8.dp
                    )
                ) {
                    if (message.forwardedFromName != null) {
                        Text("Переслано от: ${message.forwardedFromName}", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                    }
                    if (replyPreview != null && !message.isDeleted) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (message.isMine) Color(0xFFD9DFFF) else Color(0xFFE0E8F8))
                                .clickable(enabled = onReplyPreviewClick != null) {
                                    onReplyPreviewClick?.invoke()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(AccentSlate))
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(replyPreview.sender, color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    replyPreview.text,
                                    color = MainText,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    val isVoiceMessage = message.attachmentMimeType?.startsWith("audio/") == true
                    if (attachmentPath != null && attachmentRepository != null) {
                        if (isVoiceMessage) {
                            ChatVoiceMessagePlayer(
                                path = attachmentPath,
                                repository = attachmentRepository,
                                durationMs = message.attachmentDurationMs ?: 0L,
                                waveform = message.attachmentWaveform.orEmpty()
                            )
                        } else {
                            ChatImagePreview(
                                path = attachmentPath,
                                repository = attachmentRepository
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (!isVoiceMessage || attachmentPath == null) {
                        Text(
                            text = messageText,
                            color = MainText,
                            fontSize = 14.sp
                        )
                    }
                    if (showTime) {
                        Row(
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                message.time,
                                color = Muted,
                                fontSize = 9.sp
                            )
                            deliveryTicks?.let { (count, tickColor) ->
                                Canvas(
                                    modifier = Modifier
                                        .padding(start = 1.dp, end = 1.dp)
                                        .size(width = if (count == 1) 8.dp else 13.dp, height = 9.dp)
                                ) {
                                    val unit = 4.dp.toPx()
                                    val arm = 2.2.dp.toPx()
                                    val rise = 6.2.dp.toPx()
                                    val stroke = 1.25.dp.toPx()
                                    repeat(count) { index ->
                                        val x = index * unit
                                        val joint = Offset(x + arm, size.height * 0.72f)
                                        drawLine(
                                            color = tickColor,
                                            start = Offset(x, size.height * 0.48f),
                                            end = joint,
                                            strokeWidth = stroke,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = tickColor,
                                            start = joint,
                                            end = Offset(x + rise, size.height * 0.2f),
                                            strokeWidth = stroke,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            DropdownMenu(
                expanded = showActions,
                onDismissRequest = { showActions = false },
                containerColor = PanelLight
            ) {
                if (onReply != null) {
                    DropdownMenuItem(
                        text = { Text("Ответить", color = MainText) },
                        onClick = {
                            showActions = false
                            onReply()
                        }
                    )
                }
                if (onCopy != null) {
                    DropdownMenuItem(
                        text = { Text("Копировать", color = MainText) },
                        onClick = {
                            showActions = false
                            onCopy()
                        }
                    )
                }
            }
        }
    }
}

private const val MAX_CHAT_VOICE_BYTES = 10 * 1024 * 1024
private const val MAX_CHAT_VOICE_DURATION_MS = 10 * 60 * 1000L

private class ChatVoiceRecorder(private val cacheDirectory: File) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start() {
        check(recorder == null) { "Запись уже запущена." }
        val file = File(cacheDirectory, "voice-" + UUID.randomUUID().toString() + ".m4a")
        val activeRecorder = MediaRecorder()
        try {
            activeRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            activeRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            activeRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            activeRecorder.setAudioEncodingBitRate(64_000)
            activeRecorder.setAudioSamplingRate(44_100)
            activeRecorder.setMaxFileSize(MAX_CHAT_VOICE_BYTES.toLong())
            activeRecorder.setOutputFile(file.absolutePath)
            activeRecorder.prepare()
            activeRecorder.start()
            recorder = activeRecorder
            outputFile = file
        } catch (error: Exception) {
            runCatching { activeRecorder.reset() }
            activeRecorder.release()
            file.delete()
            throw error
        }
    }

    fun readAmplitudeLevel(): Int {
        val activeRecorder = recorder ?: return 8
        val amplitude = runCatching { activeRecorder.maxAmplitude }.getOrDefault(0)
        return normalizeVoiceAmplitude(amplitude)
    }

    fun stop(): File {
        val activeRecorder = recorder ?: throw IllegalStateException("Запись не запущена.")
        val completedFile = outputFile ?: throw IllegalStateException("Файл записи не найден.")
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
            require(completedFile.isFile && completedFile.length() > 0L) {
                "Запись не содержит аудиоданных."
            }
            return completedFile
        } catch (error: Exception) {
            completedFile.delete()
            throw error
        } finally {
            runCatching { activeRecorder.reset() }
            activeRecorder.release()
        }
    }

    fun cancel() {
        val activeRecorder = recorder
        val partialFile = outputFile
        recorder = null
        outputFile = null
        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.reset() }
            activeRecorder.release()
        }
        partialFile?.delete()
    }
}

@Composable
private fun ActiveAudioCallScreen(
    peerName: String,
    status: String,
    error: String?,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE5E0FF), Color(0xFFE6F0FF), Color(0xFFF1F4FF))
                )
            )
            .padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ARTURCHAT",
            color = AccentSlate,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.2.sp
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(164.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF9EBBFF).copy(alpha = 0.58f), Color(0xFFC7BFFF).copy(alpha = 0.18f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(136.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF39A7FF), Color(0xFF5868E8), Color(0xFF9564E9))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Avatar(
                    peerName.take(2).uppercase(),
                    peerName.hashCode().ushr(1) % 4,
                    Modifier.size(122.dp)
                )
            }
        }
        Spacer(Modifier.height(23.dp))
        Text(peerName, color = MainText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Surface(
            color = Color(0xCFFFFFFF),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (error == null) Color(0xFF39A982) else Color(0xFFD77782)))
                Spacer(Modifier.width(8.dp))
                Text(status, color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFFEFF1),
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, Color(0xFFF5D5DA))
            ) {
                Text(error, color = Color(0xFFA85F6E), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color(0xEFFFFFFF),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White),
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(66.dp).clickable(onClick = onToggleMute)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (muted) "×" else "⌁", color = AccentSlate, fontSize = 27.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(if (muted) "Микрофон выкл." else "Микрофон", color = MainText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(74.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFEF778D), Color(0xFFD94D71))))
                        .clickable(onClick = onEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.height(8.dp))
                Text("Завершить", color = MainText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Звонок один на один · WebRTC", color = Muted, fontSize = 10.sp)
    }
}
@Composable
private fun ActiveVideoCallScreen(
    peerName: String,
    status: String,
    error: String?,
    muted: Boolean,
    cameraEnabled: Boolean,
    client: WebRtcAudioCallClient,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEnd: () -> Unit
) {
    val remoteRenderer = client.remoteVideoRenderer
    val localRenderer = client.localVideoRenderer

    Box(
        Modifier.fillMaxSize().background(Color(0xFF171B22))
    ) {
        if (remoteRenderer != null) {
            AndroidView(
                factory = { remoteRenderer },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Подготавливаем видеосвязь…", color = Color.White, fontSize = 14.sp)
            }
        }

        if (localRenderer != null) {
            AndroidView(
                factory = { localRenderer },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = 16.dp)
                    .size(width = 112.dp, height = 156.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        }

        Column(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, end = 142.dp)
        ) {
            Text(peerName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error, color = Color(0xFFFFB8C1), fontSize = 11.sp)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoCallControlButton(
                label = if (muted) "Микрофон выкл." else "Микрофон",
                active = !muted,
                onClick = onToggleMute
            )
            VideoCallControlButton(
                label = if (cameraEnabled) "Камера" else "Камера выкл.",
                active = cameraEnabled,
                onClick = onToggleCamera
            )
            VideoCallControlButton(
                label = "Сменить",
                active = true,
                onClick = onSwitchCamera
            )
            VideoCallControlButton(
                label = "Завершить",
                active = false,
                destructive = true,
                onClick = onEnd
            )
        }

        Text(
            "ARTURCHAT · ПРЯМОЕ СОЕДИНЕНИЕ",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun VideoCallControlButton(
    label: String,
    active: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = when {
            destructive -> Color(0xFFA85F6E)
            active -> Color(0xFF394451).copy(alpha = 0.94f)
            else -> Color(0xFF69717C).copy(alpha = 0.94f)
        },
        shape = CircleShape,
        modifier = Modifier.size(72.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
        }
    }
}

private fun parseCallInstant(value: String?): java.time.Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { java.time.Instant.parse(value) }.getOrNull()
}

private fun formatCallDateTime(value: String): String {
    val instant = parseCallInstant(value) ?: return value.substringAfter('T', value).take(5)
    val zone = java.time.ZoneId.systemDefault()
    val local = instant.atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val dateLabel = when (local.toLocalDate()) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("ru"))
            .format(local)
    }
    return "$dateLabel · " + java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(local)
}

private fun formatCallDuration(call: FaceVideoCall): String? {
    val start = parseCallInstant(call.answeredAt) ?: return null
    val end = parseCallInstant(call.endedAt)
        ?: if (call.status == "accepted") java.time.Instant.now() else return null
    val seconds = java.time.Duration.between(start, end).seconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainder)
    } else {
        "%02d:%02d".format(minutes, remainder)
    }
}

private fun callStatusLabel(call: FaceVideoCall, currentUserId: String): String {
    return when (call.status) {
        "ringing" -> {
            val age = parseCallInstant(call.createdAt)?.let {
                java.time.Duration.between(it, java.time.Instant.now()).seconds
            } ?: 0
            if (call.calleeId == currentUserId && age > 60) "Пропущен" else "Вызов"
        }
        "accepted" -> "На связи"
        "declined" -> "Отклонён"
        "cancelled" -> if (call.calleeId == currentUserId) "Пропущен" else "Отменён"
        "ended" -> "Завершён"
        else -> "Звонок"
    }
}

@Composable
private fun CallsScreen(
    repository: SupabaseCallRepository?,
    userId: String?,
    onOpenProfile: (String) -> Unit
) {
    var calls by remember(userId) { mutableStateOf<List<FaceVideoCall>>(emptyList()) }
    var profiles by remember(userId) { mutableStateOf<Map<String, FaceVideoProfile>>(emptyMap()) }
    var isLoading by remember(userId) { mutableStateOf(repository != null && userId != null) }
    var errorMessage by remember(userId) { mutableStateOf<String?>(null) }
    var refreshKey by remember(userId) { mutableStateOf(0) }

    LaunchedEffect(repository, userId, refreshKey) {
        val currentRepository = repository
        val currentUserId = userId
        if (currentRepository == null || currentUserId == null) {
            calls = emptyList()
            profiles = emptyMap()
            isLoading = false
            errorMessage = "История звонков недоступна."
            return@LaunchedEffect
        }

        isLoading = calls.isEmpty()
        errorMessage = null
        runCatching {
            val loadedCalls = currentRepository.listMyCalls(currentUserId)
            val peerIds = loadedCalls.map { call ->
                if (call.callerId == currentUserId) call.calleeId else call.callerId
            }.toSet()
            val loadedProfiles = currentRepository.getProfiles(peerIds)
            calls = loadedCalls
            profiles = loadedProfiles
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить историю звонков."
            android.util.Log.w("ArturChatCalls", "Could not load call history", it)
        }
        isLoading = false
    }

    val missedCount = calls.count { callStatusLabel(it, userId.orEmpty()) == "Пропущен" }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ARTURCHAT", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("Звонки", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Text("История аудио- и видеовызовов", color = Muted, fontSize = 11.sp)
            }
            ChatHeaderIconButton(onClick = { refreshKey++ }) {
                Text("↻", color = InboxAccentStrong, fontSize = 23.sp, fontWeight = FontWeight.Light)
            }
        }

        Spacer(Modifier.height(17.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ВСЕ ЗВОНКИ", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.05.sp)
                Spacer(Modifier.height(2.dp))
                Text(calls.size.toString(), color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.width(1.dp).height(38.dp).background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent))
                )
            )
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Text("ПРОПУЩЕНО", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.05.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    missedCount.toString(),
                    color = if (missedCount > 0) Color(0xFFB65E78) else MainText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("НЕДАВНИЕ", color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            Text("${calls.size} записей", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.80f), Color(0xFFD6DDF0).copy(alpha = 0.65f), Color.Transparent)
                )
            )
        )
        Spacer(Modifier.height(5.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
                        Spacer(Modifier.height(12.dp))
                        Text("Загружаем историю…", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            calls.isEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(76.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ChatCallGlyph(video = false, tint = Color.White)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Пока нет звонков", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "Исходящие и входящие аудио- и видеовызовы появятся здесь.",
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (errorMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            TextButton(onClick = { refreshKey++ }) { Text("Повторить", color = AccentSlate) }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(calls, key = { it.id }) { call ->
                        val currentUserId = userId.orEmpty()
                        val peerId = if (call.callerId == currentUserId) call.calleeId else call.callerId
                        val profile = profiles[peerId]
                        val title = profile?.let { it.displayName.ifBlank { it.username } } ?: "Собеседник"
                        val callIsVideo = call.mediaType == "video"
                        val isMissed = callStatusLabel(call, currentUserId) == "Пропущен"
                        val duration = formatCallDuration(call)
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenProfile(peerId) }
                                .padding(top = 10.dp, bottom = 9.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Avatar(
                                        title.trim().take(2).uppercase().ifBlank { "?" },
                                        peerId.hashCode().ushr(1) % 4,
                                        Modifier.size(48.dp),
                                        imageUrl = profile?.avatarUrl,
                                        imageCacheKey = peerId
                                    )
                                    Box(
                                        modifier = Modifier.size(20.dp).clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF3CA8F4), Color(0xFF586BE4), Color(0xFF9364E6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ChatCallGlyph(video = callIsVideo, tint = Color.White)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        (if (callIsVideo) "Видеозвонок" else "Аудиозвонок") +
                                            " · " + if (call.callerId == currentUserId) "Исходящий" else "Входящий",
                                        color = Muted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(6.dp).clip(CircleShape).background(
                                                when {
                                                    isMissed || call.status == "declined" -> Color(0xFFD66C7D)
                                                    call.status == "accepted" -> Color(0xFF39A982)
                                                    else -> AccentSlate
                                                }
                                            )
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            callStatusLabel(call, currentUserId),
                                            color = if (isMissed || call.status == "declined") Color(0xFFB85C6D) else AccentSlate,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (duration != null) {
                                            Text("  ·  $duration", color = Muted, fontSize = 10.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatCallDateTime(call.createdAt), color = Muted, fontSize = 9.sp)
                                    Spacer(Modifier.height(5.dp))
                                    Text(if (call.callerId == currentUserId) "↗" else "↙", color = AccentSlate, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().height(1.dp).background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.68f), Color(0xFFD6DDF0).copy(alpha = 0.52f), Color.Transparent)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsScreen(onOpenChat: (ChatSummary) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ARTURCHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text("Друзья", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Войдите в аккаунт ArturChat, чтобы отправлять и принимать заявки в друзья.",
            color = Muted, fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private enum class FriendsSection { FRIENDS, REQUESTS }

@Composable
private fun FriendsScreen(
    repository: SupabaseChatRepository,
    userId: String,
    onlineUserIds: Set<String>,
    onOpenChat: (FaceVideoChatSummary) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var relationships by remember(userId) { mutableStateOf<List<com.facevideo.app.data.FaceVideoFriendship>>(emptyList()) }
    var people by remember(userId) { mutableStateOf<Map<String, FaceVideoProfile>>(emptyMap()) }
    var isLoading by remember(userId) { mutableStateOf(true) }
    var refreshKey by remember(userId) { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf(FriendsSection.FRIENDS) }
    var showAddFriend by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busyUserId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId, refreshKey) {
        isLoading = true
        errorMessage = null
        runCatching {
            val loaded = repository.listMyFriendships(userId)
            val relatedIds = loaded.map { row ->
                if (row.userLow == userId) row.userHigh else row.userLow
            }.distinct()
            val loadedProfiles = mutableMapOf<String, FaceVideoProfile>()
            relatedIds.forEach { id ->
                repository.getProfile(id)?.let { loadedProfiles[id] = it }
            }
            relationships = loaded
            people = loadedProfiles
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить друзей."
        }
        isLoading = false
    }

    fun relatedUserId(row: com.facevideo.app.data.FaceVideoFriendship): String =
        if (row.userLow == userId) row.userHigh else row.userLow

    val friends = relationships.filter { it.status == "accepted" }
        .mapNotNull { row -> people[relatedUserId(row)] }
        .filter { profile ->
            query.isBlank() ||
                profile.displayName.contains(query.trim(), ignoreCase = true) ||
                profile.username.contains(query.trim().removePrefix("@"), ignoreCase = true)
        }
        .sortedBy { it.displayName.ifBlank { it.username }.lowercase() }

    val incoming = relationships.filter { it.status == "pending" && it.requestedBy != userId }
    val outgoing = relationships.filter { it.status == "pending" && it.requestedBy == userId }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ARTURCHAT", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("Друзья", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Text("Люди, с которыми вы взаимно подружились", color = Muted, fontSize = 11.sp)
            }
            Surface(
                color = Color(0xDFFFFFFF),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier.clickable(interactionSource = null, indication = null) { refreshKey++ }
            ) {
                Text("↻", color = AccentSlate, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(19.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))))
                .clickable(onClick = { showAddFriend = true }),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("Добавить друга", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(15.dp))
        Surface(
            color = Color(0xAFFFFFFF),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    FriendsSection.FRIENDS to "Друзья · ${friends.size}",
                    FriendsSection.REQUESTS to "Заявки · ${incoming.size + outgoing.size}"
                ).forEach { (section, title) ->
                    val selected = selectedSection == section
                    Box(
                        Modifier.weight(1f).height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (selected) Modifier.background(
                                    Brush.horizontalGradient(listOf(Color(0xFF5B6FE4), Color(0xFF8266E9)))
                                ) else Modifier
                            )
                            .clickable(interactionSource = null, indication = null) { selectedSection = section },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (selected) Color.White else MainText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (isLoading) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
                Spacer(Modifier.height(12.dp))
                Text("Обновляем список друзей…", color = Muted, fontSize = 11.sp)
            }
        } else if (errorMessage != null) {
            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 12.sp)
            TextButton(onClick = { refreshKey++ }) { Text("Повторить", color = AccentSlate) }
        } else if (selectedSection == FriendsSection.FRIENDS) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                compact = true,
                placeholder = "Поиск среди друзей"
            )
            Spacer(Modifier.height(12.dp))
            if (friends.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar("♡", 2, Modifier.size(64.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(if (query.isBlank()) "Пока нет друзей" else "Ничего не найдено", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (query.isBlank()) "Найди человека по псевдониму и отправь заявку. Он появится здесь после принятия."
                        else "Попробуй изменить поисковый запрос.",
                        color = Muted, fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                friends.forEach { friend ->
                    FriendDirectoryRow(
                        profile = friend,
                        status = if (friend.id in onlineUserIds) "В сети" else friend.statusText.ifBlank { "Не в сети" },
                        actionLabel = "Написать",
                        actionEnabled = busyUserId != friend.id,
                        onProfile = { onOpenProfile(friend.id) },
                        onAction = {
                            scope.launch {
                                busyUserId = friend.id
                                runCatching { repository.getOrCreateDirectChat(friend.id, userId) }
                                    .onSuccess { chat ->
                                        if (chat != null) onOpenChat(chat)
                                        else errorMessage = "Не удалось открыть переписку."
                                    }
                                    .onFailure { errorMessage = it.message ?: "Не удалось открыть переписку." }
                                busyUserId = null
                            }
                        }
                    )
                }
            }
        } else {
            Text("ВХОДЯЩИЕ ЗАЯВКИ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            if (incoming.isEmpty()) {
                Text("Новых заявок пока нет.", color = Muted, fontSize = 12.sp)
            } else {
                incoming.forEach { row ->
                    val personId = relatedUserId(row)
                    val person = people[personId]
                    if (person != null) {
                        FriendDirectoryRow(
                            profile = person,
                            status = "Хочет добавить вас в друзья",
                            actionLabel = "Принять",
                            actionEnabled = busyUserId != personId,
                            onProfile = { onOpenProfile(personId) },
                            onAction = {
                                scope.launch {
                                    busyUserId = personId
                                    runCatching { repository.respondToFriendRequest(personId, true) }
                                        .onSuccess { refreshKey++ }
                                        .onFailure { errorMessage = it.message ?: "Не удалось принять заявку." }
                                    busyUserId = null
                                }
                            },
                            secondaryActionLabel = "Отклонить",
                            onSecondaryAction = {
                                scope.launch {
                                    busyUserId = personId
                                    runCatching { repository.respondToFriendRequest(personId, false) }
                                        .onSuccess { refreshKey++ }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отклонить заявку." }
                                    busyUserId = null
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("ОТПРАВЛЕННЫЕ ЗАЯВКИ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            if (outgoing.isEmpty()) {
                Text("Вы пока никому не отправляли заявки.", color = Muted, fontSize = 12.sp)
            } else {
                outgoing.forEach { row ->
                    val personId = relatedUserId(row)
                    val person = people[personId]
                    if (person != null) {
                        FriendDirectoryRow(
                            profile = person,
                            status = "Ожидает подтверждения",
                            actionLabel = "Отменить",
                            actionEnabled = busyUserId != personId,
                            onProfile = { onOpenProfile(personId) },
                            onAction = {
                                scope.launch {
                                    busyUserId = personId
                                    runCatching { repository.cancelFriendRequest(personId) }
                                        .onSuccess { refreshKey++ }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отменить заявку." }
                                    busyUserId = null
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAddFriend) {
        AddFriendDialog(
            repository = repository,
            currentUserId = userId,
            onDismiss = { showAddFriend = false },
            onChanged = {
                showAddFriend = false
                refreshKey++
            }
        )
    }
}

@Composable
private fun FriendDirectoryRow(
    profile: FaceVideoProfile,
    status: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onProfile: () -> Unit,
    onAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Surface(
        color = Color(0xDFFFFFFF),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                profile.displayName.ifBlank { profile.username }.take(1).uppercase(),
                profile.id.hashCode().ushr(1) % 4,
                Modifier.size(49.dp).clickable(onClick = onProfile),
                imageUrl = profile.avatarUrl,
                imageCacheKey = profile.id
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f).clickable(onClick = onProfile)) {
                Text(profile.displayName.ifBlank { profile.username }, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("@${profile.username}", color = Muted, fontSize = 10.sp, maxLines = 1)
                Text(status, color = AccentSlate, fontSize = 10.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                    color = Color(0xFFE9EEFF),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.clickable(enabled = actionEnabled, onClick = onAction)
                ) {
                    Text(
                        actionLabel,
                        color = AccentSlate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Surface(
                        color = Color(0xFFFFF0F2),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier.clickable(enabled = actionEnabled, onClick = onSecondaryAction)
                    ) {
                        Text(
                            secondaryActionLabel,
                            color = Color(0xFFA85F6E),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendDialog(
    repository: SupabaseChatRepository,
    currentUserId: String,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var foundProfile by remember { mutableStateOf<FaceVideoProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelLight,
        title = { Text("Добавить друга", color = MainText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Найди человека по уникальному псевдониму ArturChat.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(24)
                        foundProfile = null
                        message = null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("@", color = AccentSlate) },
                    label = { Text("Псевдоним пользователя") }
                )
                if (foundProfile != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            foundProfile!!.displayName.ifBlank { foundProfile!!.username }.take(1).uppercase(),
                            2, Modifier.size(42.dp), imageUrl = foundProfile!!.avatarUrl,
                            imageCacheKey = foundProfile!!.id
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(foundProfile!!.displayName.ifBlank { foundProfile!!.username }, color = MainText, fontWeight = FontWeight.SemiBold)
                            Text("@${foundProfile!!.username}", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(message.orEmpty(), color = if (message.orEmpty().contains("отправлена") || message.orEmpty().contains("друзья")) AccentSlate else Color(0xFFA85F6E), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && username.length >= 3,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val candidate = foundProfile
                            if (candidate == null) {
                                val found = repository.findUser(username)
                                when {
                                    found == null -> message = "Пользователь не найден."
                                    found.id == currentUserId -> message = "Нельзя добавить самого себя."
                                    else -> foundProfile = found
                                }
                            } else {
                                val result = repository.sendFriendRequest(candidate.id)
                                message = when (result) {
                                    "accepted" -> "Теперь вы друзья!"
                                    else -> "Заявка отправлена."
                                }
                                onChanged()
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Не удалось отправить заявку."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        busy -> "Подождите…"
                        foundProfile != null -> "Отправить заявку"
                        else -> "Найти"
                    },
                    color = AccentSlate
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = Muted) }
        }
    )
}

@Composable
private fun ProfileScreen(
    userEmail: String?,
    userId: String?,
    repository: SupabaseChatRepository?,
    cache: ArturChatLocalCache,
    onSignOut: () -> Unit
) {
    val cachedProfile = remember(cache, userId) { userId?.let { cache.readProfile(it, it) } }
    var profile by remember(userId, cache) { mutableStateOf(cachedProfile) }
    var username by remember(userId, cache) { mutableStateOf(cachedProfile?.username.orEmpty()) }
    var displayName by remember(userId, cache) { mutableStateOf(cachedProfile?.displayName.orEmpty()) }
    var statusText by remember(userId, cache) { mutableStateOf(cachedProfile?.statusText.orEmpty()) }
    var about by remember(userId, cache) { mutableStateOf(cachedProfile?.about.orEmpty()) }
    var isLoading by remember(userId) { mutableStateOf(cachedProfile == null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isEditing by remember(userId) { mutableStateOf(false) }
    var editSection by remember(userId) { mutableStateOf(0) }
    var showDiscardChanges by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(repository, userId, cache) {
        val currentRepository = repository
        val currentUserId = userId
        if (currentRepository != null && currentUserId != null) {
            isLoading = profile == null
            runCatching { currentRepository.getMyProfile(currentUserId) }
                .onSuccess { loaded ->
                    profile = loaded
                    if (loaded != null) cache.saveProfile(currentUserId, loaded)
                    if (loaded != null) {
                        username = loaded.username
                        displayName = loaded.displayName
                        statusText = loaded.statusText
                        about = loaded.about
                    }
                    if (loaded == null && profile == null) errorMessage = "Профиль пока не найден."
                }
                .onFailure {
                    errorMessage = it.message ?: "Не удалось обновить профиль."
                    android.util.Log.w("ArturChatProfile", "Could not refresh own profile", it)
                }
        }
        isLoading = false
    }

    val hasChanges = profile?.let {
        username != it.username ||
            displayName != it.displayName ||
            statusText != it.statusText ||
            about != it.about
    } ?: false
    val validUsername = username.matches(Regex("^[a-z0-9_]{3,24}$"))
    val canSave = repository != null && userId != null && !isSaving && hasChanges && validUsername && displayName.trim().isNotEmpty()

    fun resetDraft() {
        username = profile?.username.orEmpty()
        displayName = profile?.displayName.orEmpty()
        statusText = profile?.statusText.orEmpty()
        about = profile?.about.orEmpty()
        errorMessage = null
        successMessage = null
    }

    fun closeEditor() {
        if (hasChanges) {
            showDiscardChanges = true
        } else {
            isEditing = false
            errorMessage = null
            successMessage = null
        }
    }

    BackHandler(enabled = isEditing) {
        closeEditor()
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val currentRepository = repository
        val currentUserId = userId
        val savedProfile = profile
        if (uri != null && currentRepository != null && currentUserId != null && savedProfile != null) {
            scope.launch {
                isSaving = true
                errorMessage = null
                successMessage = null
                runCatching {
                    val prepared = withContext(Dispatchers.IO) { prepareChatImage(context, uri) }
                    val url = currentRepository.uploadProfileAvatar(currentUserId, prepared.bytes)
                    // Avatar changes are independent of the unsaved text draft.
                    currentRepository.updateMyProfile(
                        currentUserId,
                        savedProfile.username,
                        savedProfile.displayName,
                        savedProfile.statusText,
                        savedProfile.about,
                        url
                    )
                }.onSuccess { updated ->
                    profile = updated
                    cache.saveProfile(currentUserId, updated)
                    username = updated.username
                    displayName = updated.displayName
                    statusText = updated.statusText
                    about = updated.about
                    successMessage = "Фотография профиля обновлена."
                }.onFailure {
                    errorMessage = it.message ?: "Не удалось обновить фотографию."
                }
                isSaving = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        if (!isEditing) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ARTURCHAT",
                        color = AccentSlate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Мой профиль",
                        color = MainText,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        "Ваше пространство в ArturChat",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.64f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f))
                ) {
                    Text(
                        "✦",
                        color = AccentSlate,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(11.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (isLoading && profile == null) {
                Surface(
                    color = Color.White.copy(alpha = 0.66f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(21.dp),
                            color = AccentSlate,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Загружаем профиль…", color = Muted, fontSize = 12.sp)
                    }
                }
            } else if (profile != null) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.88f)),
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFE0E6FF),
                                        Color(0xFFDDEEFF),
                                        Color(0xFFECE4FF)
                                    )
                                )
                            )
                            .padding(19.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(
                                displayName.ifBlank { username }.ifBlank { userEmail ?: "A" }
                                    .trim().take(1).uppercase(),
                                1,
                                Modifier.size(92.dp),
                                highlighted = true,
                                imageUrl = profile?.avatarUrl,
                                imageCacheKey = userId
                            )
                            Spacer(Modifier.width(15.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "ВАШ АККАУНТ",
                                    color = AccentSlate,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.35.sp
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    displayName.ifBlank { "Ваше имя" },
                                    color = MainText,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (username.isNotBlank()) "@$username" else "Добавьте псевдоним",
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.48f),
                            shape = RoundedCornerShape(15.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(8.dp).background(
                                        Color(0xFF36A77B),
                                        CircleShape
                                    )
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    statusText.ifBlank { "Добавьте короткий статус о себе" },
                                    color = if (statusText.isBlank()) Muted else MainText,
                                    fontSize = 12.sp,
                                    fontWeight = if (statusText.isBlank()) FontWeight.Normal else FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )
                            }
                        }
                        Spacer(Modifier.height(15.dp))
                        Text(
                            "О СЕБЕ",
                            color = AccentSlate,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            about.ifBlank { "Пока ничего не рассказано о себе." },
                            color = if (about.isBlank()) Muted else MainText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                Modifier.fillMaxWidth().height(1.dp)
                                    .background(Color.White.copy(alpha = 0.72f))
                            )
                            Spacer(Modifier.height(11.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✉", color = AccentSlate, fontSize = 14.sp)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Электронная почта", color = Muted, fontSize = 9.sp)
                                    Text(
                                        userEmail.orEmpty(),
                                        color = MainText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "ПОДТВЕРЖДЁН",
                                    color = AccentSlate,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                Surface(
                    shape = RoundedCornerShape(19.dp),
                    color = Color.Transparent,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF26A9F5), Color(0xFF5364E8), Color(0xFF9160EA))
                                )
                            )
                            .clickable {
                                resetDraft()
                                isEditing = true
                                editSection = 0
                                errorMessage = null
                                successMessage = null
                            }
                            .padding(horizontal = 17.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✎", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Редактировать профиль", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Имя, псевдоним, статус и фото", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp)
                        }
                        Text("›", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                }

            } else {
                Surface(
                    color = Color.White.copy(alpha = 0.66f),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            errorMessage ?: "Личный профиль доступен после входа в аккаунт ArturChat.",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        if (!userEmail.isNullOrBlank()) {
                            TextButton(onClick = onSignOut) { Text("Выйти из аккаунта") }
                        }
                    }
                }
            }

            if (successMessage != null) {
                Spacer(Modifier.height(11.dp))
                ProfileFeedbackMessage(successMessage.orEmpty(), isSuccess = true)
            }
            if (errorMessage != null && profile != null) {
                Spacer(Modifier.height(9.dp))
                ProfileFeedbackMessage(errorMessage.orEmpty(), isSuccess = false)
            }

            Spacer(Modifier.height(21.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ARTURCHAT", color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text("Ваше пространство для общения", color = Muted, fontSize = 10.sp)
                }
                Text("v${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            if (!userEmail.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFFFF3F2).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, Color(0xFFF2D8D7)),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSignOut)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("↪", color = Color(0xFFA85F6E), fontSize = 19.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("Выйти из аккаунта", color = Color(0xFFA85F6E), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "‹",
                    color = AccentSlate,
                    fontSize = 29.sp,
                    modifier = Modifier.clickable { closeEditor() }.padding(end = 12.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("РЕДАКТИРОВАНИЕ", color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Профиль", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                if (hasChanges) {
                    Surface(color = Color(0xFFFFF1DA), shape = RoundedCornerShape(20.dp)) {
                        Text(
                            "Не сохранено",
                            color = Color(0xFF9A7137),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Разделите настройки профиля по категориям", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(17.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ProfileEditorTab("Данные", "01", editSection == 0, Modifier.weight(1f)) { editSection = 0 }
                ProfileEditorTab("О себе", "02", editSection == 1, Modifier.weight(1f)) { editSection = 1 }
                ProfileEditorTab("Фото", "03", editSection == 2, Modifier.weight(1f)) { editSection = 2 }
            }

            Spacer(Modifier.height(19.dp))

            when (editSection) {
                0 -> {
                    Text("ОСНОВНАЯ ИНФОРМАЦИЯ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Как вас будут видеть другие пользователи", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(17.dp))
                    ProfileInputField(
                        label = "Имя",
                        value = displayName,
                        onValueChange = {
                            displayName = it.take(48)
                            successMessage = null
                        },
                        placeholder = "Как к вам обращаться",
                        maxLength = 48
                    )
                    Spacer(Modifier.height(15.dp))
                    ProfileInputField(
                        label = "Уникальный псевдоним",
                        value = username,
                        onValueChange = {
                            username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(24)
                            errorMessage = null
                            successMessage = null
                        },
                        placeholder = "например, artur_01",
                        prefix = "@",
                        maxLength = 24,
                        footnote = "3–24 символа · латинские буквы, цифры и _",
                        isInvalid = username.isNotEmpty() && !validUsername
                    )
                    Spacer(Modifier.height(15.dp))
                    ProfileReadOnlyRow(
                        title = "Электронная почта",
                        value = userEmail.orEmpty(),
                        note = "Используется для входа в ArturChat"
                    )
                }
                1 -> {
                    Text("ВАШЕ ПРИСУТСТВИЕ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Коротко расскажите, чем хотите поделиться", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(17.dp))
                    ProfileInputField(
                        label = "Статус",
                        value = statusText,
                        onValueChange = {
                            statusText = it.take(80)
                            successMessage = null
                        },
                        placeholder = "Например: На работе · Не беспокоить",
                        maxLength = 80,
                        footnote = "Короткая фраза, которую увидят ваши собеседники"
                    )
                    Spacer(Modifier.height(17.dp))
                    ProfileInputField(
                        label = "О себе",
                        value = about,
                        onValueChange = {
                            about = it.take(500)
                            successMessage = null
                        },
                        placeholder = "Расскажите немного о себе…",
                        maxLength = 500,
                        multiline = true,
                        footnote = "До 500 символов"
                    )
                    Spacer(Modifier.height(13.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Совет: оставляйте только ту информацию, которой готовы поделиться с друзьями.",
                            color = Muted,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(13.dp)
                        )
                    }
                }
                else -> {
                    Text("ФОТО ПРОФИЛЯ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Изображение, по которому вас узнают друзья", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(23.dp))
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(
                            displayName.ifBlank { username }.ifBlank { userEmail ?: "A" }.take(1).uppercase(),
                            1,
                            Modifier.size(136.dp),
                            highlighted = true,
                            imageUrl = profile?.avatarUrl,
                            imageCacheKey = userId
                        )
                        Spacer(Modifier.height(17.dp))
                        Text(
                            displayName.ifBlank { "Ваш профиль" },
                            color = MainText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Псевдоним не указан",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(17.dp),
                            color = Color.Transparent,
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF26A9F5), Color(0xFF5364E8), Color(0xFF9160EA))
                                        )
                                    )
                                    .clickable(enabled = !isSaving) { avatarPicker.launch("image/*") }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("＋", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isSaving) "Обновляем фото…" else "Выбрать фотографию",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(11.dp))
                        Text(
                            "Поддерживаются изображения из галереи. Фото обновится сразу после загрузки.",
                            color = Muted,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                ProfileFeedbackMessage(errorMessage.orEmpty(), isSuccess = false)
            }
            if (successMessage != null) {
                Spacer(Modifier.height(10.dp))
                ProfileFeedbackMessage(successMessage.orEmpty(), isSuccess = true)
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                shadowElevation = if (canSave) 5.dp else 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            if (canSave) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF26A9F5), Color(0xFF5364E8), Color(0xFF9160EA))
                                )
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFFD7DEED), Color(0xFFC8D2E7)))
                            }
                        )
                        .clickable(enabled = canSave && repository != null && userId != null) {
                            val currentRepository = repository
                            val currentUserId = userId
                            if (currentRepository != null && currentUserId != null) {
                                scope.launch {
                                    isSaving = true
                                    errorMessage = null
                                    successMessage = null
                                    runCatching {
                                        currentRepository.updateMyProfile(
                                            currentUserId,
                                            username,
                                            displayName,
                                            statusText,
                                            about
                                        )
                                    }.onSuccess { updated ->
                                        profile = updated
                                        cache.saveProfile(currentUserId, updated)
                                        username = updated.username
                                        displayName = updated.displayName
                                        statusText = updated.statusText
                                        about = updated.about
                                        successMessage = "Изменения сохранены и доступны собеседникам."
                                        isEditing = false
                                    }.onFailure { failure ->
                                        errorMessage = when {
                                            failure.message.orEmpty().contains("duplicate", true) ||
                                                failure.message.orEmpty().contains("unique", true) ->
                                                "Этот псевдоним уже занят. Выбери другой."
                                            else -> failure.message ?: "Не удалось сохранить изменения."
                                        }
                                    }
                                    isSaving = false
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSaving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            Modifier.size(19.dp),
                            color = if (canSave) Color.White else Muted,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(
                        when {
                            isSaving -> "Сохраняем…"
                            canSave -> "Сохранить изменения"
                            hasChanges && !validUsername -> "Проверьте псевдоним"
                            hasChanges && displayName.isBlank() -> "Укажите имя"
                            else -> "Все изменения сохранены"
                        },
                        color = if (canSave) Color.White else Color(0xFF73809A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                "Изменения профиля синхронизируются с вашим аккаунтом ArturChat.",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 15.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showDiscardChanges) {
        AlertDialog(
            onDismissRequest = { showDiscardChanges = false },
            title = { Text("Отменить изменения?") },
            text = { Text("Несохранённые данные будут сброшены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardChanges = false
                        resetDraft()
                        isEditing = false
                    }
                ) {
                    Text("Не сохранять", color = Color(0xFFA85F6E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChanges = false }) {
                    Text("Продолжить")
                }
            }
        )
    }
}


@Composable
private fun ProfileEditorTab(
    title: String,
    number: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (selected) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.76f)
        ),
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth()
                .then(
                    if (selected) Modifier.background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF28A8F3), Color(0xFF5364E8), Color(0xFF9160EA))
                        )
                    ) else Modifier
                )
                .padding(horizontal = 6.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                number,
                color = if (selected) Color.White.copy(alpha = 0.82f) else AccentSlate.copy(alpha = 0.72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                title,
                color = if (selected) Color.White else MainText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileReadOnlyRow(
    title: String,
    value: String,
    note: String
) {
    Surface(
        color = Color.White.copy(alpha = 0.58f),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.84f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title.uppercase(), color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(5.dp))
            Text(value.ifBlank { "Не указана" }, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(note, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProfileFeedbackMessage(
    message: String,
    isSuccess: Boolean
) {
    Surface(
        color = if (isSuccess) Color(0xFFEAF7F1).copy(alpha = 0.94f) else Color(0xFFFFF0F1).copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isSuccess) Color(0xFFC9E8D9) else Color(0xFFF0D0D3)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            message,
            color = if (isSuccess) Color(0xFF39795E) else Color(0xFFA85F6E),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    maxLength: Int? = null,
    footnote: String? = null,
    isInvalid: Boolean = false,
    multiline: Boolean = false
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 2.dp)
                .padding(bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label.uppercase(),
                color = if (isInvalid) Color(0xFFA85F6E) else AccentSlate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.15.sp,
                modifier = Modifier.weight(1f)
            )
            if (maxLength != null) {
                Text(
                    "${value.length}/$maxLength",
                    color = if (isInvalid) Color(0xFFA85F6E) else Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.2.dp,
                if (isInvalid) Color(0xFFE2BFC1) else Color.White.copy(alpha = 0.96f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.62f),
                                Color(0xFFE8EDFF).copy(alpha = 0.54f),
                                Color.White.copy(alpha = 0.70f)
                            )
                        )
                    )
                    .padding(
                        horizontal = 15.dp,
                        vertical = if (multiline) 13.dp else 16.dp
                    ),
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically
            ) {
                if (prefix != null) {
                    Text(
                        prefix,
                        color = AccentSlate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp, top = if (multiline) 1.dp else 0.dp)
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = Color(0xFF7E8BAA).copy(alpha = 0.86f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !multiline,
                        minLines = if (multiline) 3 else 1,
                        maxLines = if (multiline) 5 else 1,
                        textStyle = TextStyle(
                            color = MainText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = if (multiline) KeyboardCapitalization.Sentences
                                else KeyboardCapitalization.Words
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentSlate)
                    )
                }
            }
        }
        if (footnote != null) {
            Text(
                footnote,
                color = if (isInvalid) Color(0xFFA85F6E) else Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 3.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun UserProfileScreen(
    userId: String,
    currentUserId: String,
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    onlineUserIds: Set<String>,
    onBack: () -> Unit,
    onOpenChat: (FaceVideoChatSummary) -> Unit
) {
    val cachedProfile = remember(cache, userId, currentUserId) { cache.readProfile(currentUserId, userId) }
    var profile by remember(userId, currentUserId, cache) { mutableStateOf(cachedProfile) }
    var friendship by remember(userId, currentUserId) { mutableStateOf<com.facevideo.app.data.FaceVideoFriendship?>(null) }
    var isLoading by remember(userId) { mutableStateOf(cachedProfile == null) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRemoveFriend by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId, currentUserId, cache) {
        isLoading = profile == null
        runCatching {
            val loadedProfile = repository.getProfile(userId)
            if (loadedProfile != null) {
                profile = loadedProfile
                cache.saveProfile(currentUserId, loadedProfile)
            }
            friendship = repository.getFriendshipWith(currentUserId, userId)
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить профиль."
            android.util.Log.w("ArturChatProfile", "Could not refresh user profile; keeping local cache", it)
        }
        isLoading = false
    }

    fun refreshFriendship() {
        scope.launch {
            isBusy = true
            runCatching { repository.getFriendshipWith(currentUserId, userId) }
                .onSuccess { friendship = it }
                .onFailure { errorMessage = it.message ?: "Не удалось обновить статус дружбы." }
            isBusy = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("‹ Назад", color = AccentSlate, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        if (isLoading && profile == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
            }
        } else if (profile != null) {
            val p = profile!!
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(22.dp))
                Avatar(
                    p.displayName.ifBlank { p.username }.take(1).uppercase(),
                    2,
                    Modifier.size(124.dp),
                    imageUrl = p.avatarUrl,
                    imageCacheKey = p.id
                )
                Spacer(Modifier.height(16.dp))
                Text(p.displayName.ifBlank { p.username }, color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("@${p.username}", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(if (userId in onlineUserIds) "В сети" else "Не в сети", color = if (userId in onlineUserIds) AccentSlate else Muted, fontSize = 11.sp)
                if (p.statusText.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(color = PaletteBlush, shape = RoundedCornerShape(18.dp)) {
                        Text(p.statusText, color = MainText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))

                when {
                    userId == currentUserId -> {
                        Text("Это ваш профиль.", color = Muted, fontSize = 12.sp)
                    }
                    friendship?.status == "accepted" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.getOrCreateDirectChat(userId, currentUserId) }
                                        .onSuccess { chat ->
                                            if (chat != null) onOpenChat(chat)
                                            else errorMessage = "Не удалось открыть переписку."
                                        }
                                        .onFailure { errorMessage = it.message ?: "Не удалось открыть переписку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isBusy) "Открываем…" else "Написать") }
                        TextButton(onClick = { showRemoveFriend = true }, enabled = !isBusy) {
                            Text("Удалить из друзей", color = Color(0xFFA85F6E), fontSize = 11.sp)
                        }
                    }
                    friendship?.status == "pending" && friendship?.requestedBy == currentUserId -> {
                        Surface(color = PanelLight, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Заявка отправлена", color = AccentSlate, fontSize = 13.sp, modifier = Modifier.padding(14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.cancelFriendRequest(userId) }
                                        .onSuccess { friendship = null }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отменить заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy
                        ) { Text("Отменить заявку", color = Muted, fontSize = 11.sp) }
                    }
                    friendship?.status == "pending" -> {
                        Text("Этот пользователь отправил вам заявку в друзья.", color = Muted, fontSize = 12.sp)
                        Button(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.respondToFriendRequest(userId, true) }
                                        .onSuccess { refreshFriendship() }
                                        .onFailure { errorMessage = it.message ?: "Не удалось принять заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Принять заявку") }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.respondToFriendRequest(userId, false) }
                                        .onSuccess { friendship = null }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отклонить заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy
                        ) { Text("Отклонить", color = Color(0xFFA85F6E)) }
                    }
                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.sendFriendRequest(userId) }
                                        .onSuccess { refreshFriendship() }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отправить заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isBusy) "Отправляем…" else "＋ Добавить в друзья") }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 11.sp)
                }

                Spacer(Modifier.height(20.dp))
                Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, PaletteMist), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("О СЕБЕ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(p.about.ifBlank { "Пользователь пока ничего не рассказал о себе." }, color = MainText, fontSize = 14.sp)
                    }
                }
            }
        } else {
            Text(errorMessage ?: "Не удалось загрузить профиль пользователя.", color = Muted)
        }
    }

    if (showRemoveFriend) {
        AlertDialog(
            onDismissRequest = { showRemoveFriend = false },
            title = { Text("Удалить из друзей?") },
            text = { Text("Вы больше не будете отображаться друг у друга в списке друзей.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveFriend = false
                    scope.launch {
                        isBusy = true
                        runCatching { repository.removeFriend(userId) }
                            .onSuccess { friendship = null }
                            .onFailure { errorMessage = it.message ?: "Не удалось удалить друга." }
                        isBusy = false
                    }
                }) { Text("Удалить", color = Color(0xFFA85F6E)) }
            },
            dismissButton = { TextButton(onClick = { showRemoveFriend = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun InboxHeaderActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    // A slim gradient rim and a frosted inner disc make each header action
    // read as a deliberate icon control rather than a plain white circle.
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF9EADFF).copy(alpha = 0.82f),
                        Color(0xFFB7D9FF).copy(alpha = 0.96f),
                        Color(0xFFC7B5FF).copy(alpha = 0.88f)
                    )
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(42.dp).clickable(onClick = onClick),
            color = Color(0xF7FFFFFF),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.98f)),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFF0F3FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ChatHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(42.dp).clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun InboxBackGlyph() {
    Canvas(Modifier.size(27.dp)) {
        val line = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(size.width * 0.79f, size.height * 0.50f)
            lineTo(size.width * 0.23f, size.height * 0.50f)
            moveTo(size.width * 0.23f, size.height * 0.50f)
            lineTo(size.width * 0.54f, size.height * 0.20f)
            moveTo(size.width * 0.23f, size.height * 0.50f)
            lineTo(size.width * 0.54f, size.height * 0.80f)
        }
        drawPath(arrow, InboxAccentStrong, style = line)
    }
}

@Composable
private fun ChatSendGlyph() {
    Canvas(Modifier.size(22.dp)) {
        val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.48f)
            lineTo(size.width * 0.88f, size.height * 0.12f)
            lineTo(size.width * 0.62f, size.height * 0.89f)
            lineTo(size.width * 0.46f, size.height * 0.57f)
            close()
            moveTo(size.width * 0.46f, size.height * 0.57f)
            lineTo(size.width * 0.88f, size.height * 0.12f)
        }
        drawPath(path, Color.White, style = line)
    }
}

@Composable
private fun ChatMicrophoneGlyph(tint: Color = InboxAccentStrong) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        val capsule = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(w * 0.36f, h * 0.10f, w * 0.30f, h * 0.19f, w * 0.30f, h * 0.30f)
            lineTo(w * 0.30f, h * 0.53f)
            cubicTo(w * 0.30f, h * 0.66f, w * 0.38f, h * 0.74f, w * 0.50f, h * 0.74f)
            cubicTo(w * 0.62f, h * 0.74f, w * 0.70f, h * 0.66f, w * 0.70f, h * 0.53f)
            lineTo(w * 0.70f, h * 0.30f)
            cubicTo(w * 0.70f, h * 0.19f, w * 0.64f, h * 0.10f, w * 0.50f, h * 0.10f)
        }
        val cradle = Path().apply {
            moveTo(w * 0.17f, h * 0.47f)
            cubicTo(w * 0.17f, h * 0.72f, w * 0.31f, h * 0.86f, w * 0.50f, h * 0.86f)
            cubicTo(w * 0.69f, h * 0.86f, w * 0.83f, h * 0.72f, w * 0.83f, h * 0.47f)
            moveTo(w * 0.50f, h * 0.86f)
            lineTo(w * 0.50f, h * 0.98f)
            moveTo(w * 0.36f, h * 0.98f)
            lineTo(w * 0.64f, h * 0.98f)
        }
        drawPath(capsule, tint, style = line)
        drawPath(cradle, tint, style = line)
    }
}

@Composable
private fun ChatPlaybackGlyph(isPlaying: Boolean) {
    Canvas(Modifier.size(15.dp)) {
        if (isPlaying) {
            val width = 2.4.dp.toPx()
            drawLine(Color.White, Offset(size.width * 0.34f, size.height * 0.22f), Offset(size.width * 0.34f, size.height * 0.78f), width, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(size.width * 0.68f, size.height * 0.22f), Offset(size.width * 0.68f, size.height * 0.78f), width, cap = StrokeCap.Round)
        } else {
            val play = Path().apply {
                moveTo(size.width * 0.27f, size.height * 0.14f)
                lineTo(size.width * 0.80f, size.height * 0.50f)
                lineTo(size.width * 0.27f, size.height * 0.86f)
                close()
            }
            drawPath(play, Color.White)
        }
    }
}

@Composable
private fun ChatCallGlyph(video: Boolean, tint: Color = InboxAccentStrong) {
    Canvas(Modifier.size(if (video) 21.dp else 22.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        if (video) {
            val cameraBody = Path().apply {
                moveTo(w * 0.13f, h * 0.28f)
                cubicTo(w * 0.13f, h * 0.20f, w * 0.20f, h * 0.15f, w * 0.29f, h * 0.15f)
                lineTo(w * 0.56f, h * 0.15f)
                cubicTo(w * 0.65f, h * 0.15f, w * 0.70f, h * 0.21f, w * 0.70f, h * 0.30f)
                lineTo(w * 0.70f, h * 0.70f)
                cubicTo(w * 0.70f, h * 0.79f, w * 0.64f, h * 0.85f, w * 0.55f, h * 0.85f)
                lineTo(w * 0.29f, h * 0.85f)
                cubicTo(w * 0.20f, h * 0.85f, w * 0.13f, h * 0.79f, w * 0.13f, h * 0.70f)
                close()
            }
            val lens = Path().apply {
                moveTo(w * 0.72f, h * 0.39f)
                lineTo(w * 0.91f, h * 0.27f)
                lineTo(w * 0.91f, h * 0.73f)
                lineTo(w * 0.72f, h * 0.61f)
                close()
            }
            drawPath(cameraBody, tint, style = line)
            drawPath(lens, tint, style = line)
        } else {
            val handset = Path().apply {
                moveTo(w * 0.31f, h * 0.13f)
                cubicTo(w * 0.23f, h * 0.13f, w * 0.17f, h * 0.22f, w * 0.20f, h * 0.33f)
                cubicTo(w * 0.25f, h * 0.61f, w * 0.43f, h * 0.77f, w * 0.69f, h * 0.83f)
                cubicTo(w * 0.79f, h * 0.85f, w * 0.86f, h * 0.78f, w * 0.85f, h * 0.68f)
                lineTo(w * 0.82f, h * 0.56f)
                cubicTo(w * 0.81f, h * 0.51f, w * 0.76f, h * 0.49f, w * 0.70f, h * 0.52f)
                lineTo(w * 0.60f, h * 0.59f)
                cubicTo(w * 0.49f, h * 0.53f, w * 0.42f, h * 0.46f, w * 0.37f, h * 0.35f)
                lineTo(w * 0.43f, h * 0.24f)
                cubicTo(w * 0.46f, h * 0.19f, w * 0.41f, h * 0.14f, w * 0.36f, h * 0.13f)
                close()
            }
            drawPath(handset, tint, style = line)
        }
    }
}

@Composable
private fun InboxSearchGlyph(iconSize: Dp = 21.dp) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(
            color = InboxAccentStrong,
            radius = size.minDimension * 0.31f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = stroke
        )
        drawLine(
            color = InboxAccentStrong,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.91f, size.height * 0.91f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun InboxCameraGlyph() {
    Canvas(Modifier.size(28.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val body = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.34f)
            cubicTo(size.width * 0.16f, size.height * 0.27f, size.width * 0.22f, size.height * 0.23f, size.width * 0.30f, size.height * 0.23f)
            lineTo(size.width * 0.41f, size.height * 0.23f)
            lineTo(size.width * 0.49f, size.height * 0.13f)
            lineTo(size.width * 0.68f, size.height * 0.13f)
            lineTo(size.width * 0.76f, size.height * 0.23f)
            lineTo(size.width * 0.79f, size.height * 0.23f)
            cubicTo(size.width * 0.87f, size.height * 0.23f, size.width * 0.90f, size.height * 0.29f, size.width * 0.90f, size.height * 0.37f)
            lineTo(size.width * 0.90f, size.height * 0.72f)
            cubicTo(size.width * 0.90f, size.height * 0.80f, size.width * 0.84f, size.height * 0.85f, size.width * 0.76f, size.height * 0.85f)
            lineTo(size.width * 0.27f, size.height * 0.85f)
            cubicTo(size.width * 0.19f, size.height * 0.85f, size.width * 0.15f, size.height * 0.79f, size.width * 0.15f, size.height * 0.71f)
            close()
        }
        drawPath(body, InboxAccentStrong, style = stroke)
        drawCircle(
            InboxAccentStrong,
            radius = size.width * 0.15f,
            center = Offset(size.width * 0.53f, size.height * 0.53f),
            style = stroke
        )
    }
}

@Composable
private fun NewChatGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val line = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val bubble = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.18f)
            cubicTo(size.width * 0.18f, size.height * 0.10f, size.width * 0.27f, size.height * 0.08f, size.width * 0.36f, size.height * 0.08f)
            lineTo(size.width * 0.72f, size.height * 0.08f)
            cubicTo(size.width * 0.82f, size.height * 0.08f, size.width * 0.88f, size.height * 0.16f, size.width * 0.88f, size.height * 0.26f)
            lineTo(size.width * 0.88f, size.height * 0.61f)
            cubicTo(size.width * 0.88f, size.height * 0.70f, size.width * 0.80f, size.height * 0.77f, size.width * 0.70f, size.height * 0.77f)
            lineTo(size.width * 0.41f, size.height * 0.77f)
            lineTo(size.width * 0.20f, size.height * 0.92f)
            lineTo(size.width * 0.25f, size.height * 0.75f)
            cubicTo(size.width * 0.20f, size.height * 0.70f, size.width * 0.18f, size.height * 0.65f, size.width * 0.18f, size.height * 0.58f)
            close()
        }
        drawPath(bubble, Color.White, style = line)
        drawLine(Color.White, Offset(size.width * 0.55f, size.height * 0.30f), Offset(size.width * 0.55f, size.height * 0.57f), strokeWidth = line.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(size.width * 0.42f, size.height * 0.435f), Offset(size.width * 0.68f, size.height * 0.435f), strokeWidth = line.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun InboxNavigationGlyph(tab: MainTab, selected: Boolean) {
    val tint = if (selected) InboxAccent else InboxMuted
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (tab) {
            MainTab.CHATS -> {
                val bubble = Path().apply {
                    moveTo(w * 0.18f, h * 0.22f)
                    cubicTo(w * 0.18f, h * 0.13f, w * 0.26f, h * 0.10f, w * 0.36f, h * 0.10f)
                    lineTo(w * 0.72f, h * 0.10f)
                    cubicTo(w * 0.82f, h * 0.10f, w * 0.88f, h * 0.17f, w * 0.88f, h * 0.27f)
                    lineTo(w * 0.88f, h * 0.62f)
                    cubicTo(w * 0.88f, h * 0.72f, w * 0.80f, h * 0.78f, w * 0.70f, h * 0.78f)
                    lineTo(w * 0.42f, h * 0.78f)
                    lineTo(w * 0.22f, h * 0.92f)
                    lineTo(w * 0.26f, h * 0.75f)
                    cubicTo(w * 0.20f, h * 0.70f, w * 0.18f, h * 0.65f, w * 0.18f, h * 0.58f)
                    close()
                }
                drawPath(bubble, tint, style = line)
                listOf(0.39f, 0.53f, 0.67f).forEach { x -> drawCircle(tint, radius = 1.0.dp.toPx(), center = Offset(w * x, h * 0.45f)) }
            }
            MainTab.CALLS -> {
                val handset = Path().apply {
                    moveTo(w * 0.31f, h * 0.14f)
                    cubicTo(w * 0.23f, h * 0.14f, w * 0.18f, h * 0.22f, w * 0.20f, h * 0.32f)
                    cubicTo(w * 0.25f, h * 0.59f, w * 0.43f, h * 0.76f, w * 0.69f, h * 0.82f)
                    cubicTo(w * 0.79f, h * 0.84f, w * 0.86f, h * 0.77f, w * 0.85f, h * 0.68f)
                    lineTo(w * 0.82f, h * 0.56f)
                    cubicTo(w * 0.81f, h * 0.51f, w * 0.76f, h * 0.49f, w * 0.71f, h * 0.52f)
                    lineTo(w * 0.60f, h * 0.59f)
                    cubicTo(w * 0.49f, h * 0.53f, w * 0.42f, h * 0.46f, w * 0.37f, h * 0.35f)
                    lineTo(w * 0.43f, h * 0.24f)
                    cubicTo(w * 0.46f, h * 0.19f, w * 0.43f, h * 0.15f, w * 0.38f, h * 0.14f)
                    close()
                }
                drawPath(handset, tint, style = line)
            }
            MainTab.CONTACTS -> {
                drawCircle(tint, radius = w * 0.105f, center = Offset(w * 0.50f, h * 0.28f), style = line)
                drawCircle(tint, radius = w * 0.085f, center = Offset(w * 0.20f, h * 0.41f), style = line)
                drawCircle(tint, radius = w * 0.085f, center = Offset(w * 0.80f, h * 0.41f), style = line)
                val centerShoulders = Path().apply {
                    moveTo(w * 0.22f, h * 0.88f)
                    cubicTo(w * 0.24f, h * 0.68f, w * 0.36f, h * 0.58f, w * 0.50f, h * 0.58f)
                    cubicTo(w * 0.65f, h * 0.58f, w * 0.77f, h * 0.68f, w * 0.79f, h * 0.88f)
                }
                drawPath(centerShoulders, tint, style = line)
                val leftShoulder = Path().apply {
                    moveTo(w * 0.05f, h * 0.82f)
                    cubicTo(w * 0.06f, h * 0.66f, w * 0.13f, h * 0.59f, w * 0.25f, h * 0.59f)
                }
                drawPath(leftShoulder, tint, style = line)
                val rightShoulder = Path().apply {
                    moveTo(w * 0.75f, h * 0.59f)
                    cubicTo(w * 0.87f, h * 0.59f, w * 0.94f, h * 0.66f, w * 0.95f, h * 0.82f)
                }
                drawPath(rightShoulder, tint, style = line)
            }
            MainTab.PROFILE -> {
                drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.29f), style = line)
                val shoulders = Path().apply {
                    moveTo(w * 0.15f, h * 0.91f)
                    cubicTo(w * 0.17f, h * 0.65f, w * 0.32f, h * 0.56f, w * 0.50f, h * 0.56f)
                    cubicTo(w * 0.68f, h * 0.56f, w * 0.83f, h * 0.65f, w * 0.85f, h * 0.91f)
                }
                drawPath(shoulders, tint, style = line)
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: MainTab,
    unreadChatsCount: Int,
    onSelect: (MainTab) -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xEFFFFFFF), Color(0xD8E8EEFC), Color(0xDDE3EAF8))
                    )
                )
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.values().forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    Modifier.weight(1f)
                        .clickable(interactionSource = null, indication = null) { onSelect(tab) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(width = 42.dp, height = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        InboxNavigationGlyph(tab = tab, selected = selected)
                        if (tab == MainTab.CHATS) {
                            formatUnreadBadge(unreadChatsCount)?.let { badgeText ->
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd)
                                        .height(18.dp).widthIn(min = 18.dp),
                                    color = InboxAccent,
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.height(18.dp).padding(horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            badgeText,
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.title,
                        color = if (selected) InboxAccentStrong else InboxMuted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
