package com.facevideo.app

import android.Manifest
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.facevideo.app.calls.WebRtcAudioCallClient
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.SupabaseCallRepository
import com.facevideo.app.data.DemoData
import com.facevideo.app.data.SupabaseClientProvider
import com.facevideo.app.data.SupabaseChatRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.facevideo.app.notifications.AppNotificationManager
import com.facevideo.app.notifications.CallNotificationIntents
import com.facevideo.app.notifications.PushTokenRegistrar
import com.facevideo.app.model.ChatMessage
import com.facevideo.app.model.ChatSummary
import com.facevideo.app.model.MessageDeliveryStatus
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
private val AppBackground = Color(0xFFF4E9E5)
private val Panel = Color(0xFFFFFCFA)
private val PanelLight = Color(0xFFE5EAF0)
private val IncomingMessage = Color(0xFFE5EAF0)
private val AccentSlate = Color(0xFF5D6C84)
private val MainText = Color(0xFF344052)
private val Muted = Color(0xFF68758A)
private val PaletteBlush = Color(0xFFF5D2D0)
private val PaletteSlate = Color(0xFF7987A1)
private val PaletteBlueGray = Color(0xFF9EAFC5)
private val PaletteMist = Color(0xFFC9D3DF)
private val AvatarColors = listOf(
    PaletteBlush, PaletteSlate, PaletteBlueGray, PaletteMist
)

private enum class MainTab(val title: String, val symbol: String) {
    CHATS("Чаты", "✉"),
    CALLS("Звонки", "◉"),
    CONTACTS("Друзья", "◎"),
    PROFILE("Профиль", "◌")
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
    data class Outgoing(val chat: FaceVideoChatSummary) : PendingCallAction
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
    var callInfoMessage by remember { mutableStateOf("Видеозвонки будут подключены следующим этапом. Сейчас доступны персональные аудиозвонки.") }

    val supabase = SupabaseClientProvider.client
    val liveRepository = remember(supabase) { supabase?.let { SupabaseChatRepository(it) } }
    val callRepository = remember(supabase) { supabase?.let { SupabaseCallRepository(it) } }
    val scope = rememberCoroutineScope()
    var onlineUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
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
                onlineUserIds = presences.mapTo(mutableSetOf()) { it.userId }
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
            onlineUserIds = emptySet()
            presenceAvailable = false
        } finally {
            presenceCollector.cancel()
            runCatching { currentClient.realtime.removeChannel(presenceChannel) }
                .onFailure {
                    android.util.Log.w("ArturChatPresence", "Could not remove online presence channel", it)
                }
            onlineUserIds = emptySet()
            presenceAvailable = false
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
                val call = repository.startDirectCall(action.chat.id, currentUser, "audio")
                activeCall = call
                activeCallTitle = action.chat.title
                activeCallIsOutgoing = true
                callStatus = "Вызов собеседника…"
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
                callStatus = "Подключаем аудио…"
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

    fun requestCallPermission(action: PendingCallAction) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                runCatching { performCallAction(action) }
                    .onFailure { callError = it.message ?: "Не удалось начать звонок." }
            }
        } else {
            pendingCallAction = action
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

    BackHandler(enabled = activeCall != null || openedLiveChat != null || openedChat != null) {
        when {
            activeCall != null -> finishCall()
            openedLiveChat != null -> openedLiveChat = null
            openedChat != null -> openedChat = null
        }
    }

    val incomingCall = incomingCalls.firstOrNull { it.id != activeCall?.id }
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
                    callStatus = "Собеседник ответил. Подключаем аудио…"
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
        } else if (openedStoriesUserId != null && liveRepository != null && userId != null) {
            StoryViewerScreen(
                storyOwnerId = openedStoriesUserId!!,
                currentUserId = userId,
                repository = liveRepository,
                onBack = { openedStoriesUserId = null }
            )
        } else if (openedProfileUserId != null && liveRepository != null && userId != null) {
            UserProfileScreen(
                userId = openedProfileUserId!!,
                currentUserId = userId,
                repository = liveRepository,
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
                    if (isVideo) {
                        callInfoMessage = "Видеозвонки ещё разрабатываются. Сейчас уже можно звонить голосом."
                        showCallInfo = true
                    } else {
                        requestCallPermission(PendingCallAction.Outgoing(currentChat))
                    }
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
            Column(Modifier.fillMaxSize()) {
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
                        MainTab.CALLS -> CallsScreen(onCall = { showCallInfo = true })
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
            title = { Text("Входящий аудиозвонок", color = MainText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        incomingCallerProfile?.displayName?.ifBlank { incomingCallerProfile?.username ?: "Пользователь" }
                            ?: "Пользователь звонит тебе",
                        color = MainText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("ArturChat · голосовой вызов", color = Muted, fontSize = 12.sp)
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
    var showNewChat by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, reloadKey, userId) {
        if (chats.isEmpty()) isLoading = true
        loadError = null
        try {
            val refreshedChats = repository.listMyChats()
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

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ARTURCHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text(if (contactsMode) "Контакты" else "Сообщения", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = Color(0xFFE5EAF0),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFF9EAFC5))
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccentSlate))
                    Spacer(Modifier.width(7.dp))
                    Text("ONLINE", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        SearchField(value = query, onValueChange = { query = it })

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (contactsMode) "НАЙТИ ПОЛЬЗОВАТЕЛЯ" else "ВАШИ ДИАЛОГИ", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text(
                    if (contactsMode) "Поиск по псевдониму" else "${visibleChats.size} диалогов",
                    color = MainText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(onClick = { showNewChat = true }) {
                Text("＋ Новый чат")
            }
        }

        if (!contactsMode) {
            StoriesStrip(
                repository = repository,
                userId = userId,
                onOpenStories = onOpenStories
            )
        }

        if (isLoading && chats.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
            }
        } else if (visibleChats.isEmpty() && loadError != null) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Не удалось подключиться к перепискам", color = MainText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(loadError.orEmpty(), color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { reloadKey++ }) { Text("Повторить", color = AccentSlate) }
            }
        } else if (visibleChats.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Avatar("FV", 1, Modifier.size(68.dp))
                Spacer(Modifier.height(18.dp))
                Text(if (query.isBlank()) "Пока нет переписок" else "Ничего не найдено", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Нажми «Новый чат», чтобы найти человека по псевдониму.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(15.dp))
                TextButton(onClick = { showNewChat = true }) { Text("Найти пользователя", color = AccentSlate) }
            }
        } else {
            Column(Modifier.weight(1f)) {
                if (loadError != null) {
                    Text(
                        "Нет соединения — показаны сохранённые переписки",
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                }
                LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(visibleChats, key = { it.id }) { chat ->
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
                }
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
    userId: String,
    onOpenStories: (String) -> Unit
) {
    var stories by remember(userId) { mutableStateOf<List<FaceVideoStoryCard>>(emptyList()) }
    var myProfile by remember(userId) { mutableStateOf<FaceVideoProfile?>(null) }
    var loadError by remember(userId) { mutableStateOf<String?>(null) }
    var refreshKey by remember(userId) { mutableStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(repository, userId, refreshKey) {
        loadError = null
        runCatching {
            val visible = repository.listVisibleStories()
            val own = repository.getMyProfile(userId)
            stories = visible
            myProfile = own
        }.onFailure {
            loadError = it.message ?: "Не удалось загрузить истории."
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

    Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ИСТОРИИ", color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "Обновить",
                color = Muted,
                fontSize = 9.sp,
                modifier = Modifier.clickable { refreshKey++ }.padding(4.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(66.dp).clickable {
                    if (ownStories.isNotEmpty()) onOpenStories(userId) else imagePicker.launch("image/*")
                }
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier.size(58.dp).background(
                            Brush.linearGradient(listOf(PaletteBlush, PaletteBlueGray)),
                            CircleShape
                        ).padding(3.dp)
                    ) {
                        Avatar(
                            myProfile?.displayName?.ifBlank { myProfile?.username.orEmpty() }?.take(1)?.uppercase() ?: "Я",
                            0,
                            Modifier.fillMaxSize(),
                            imageUrl = myProfile?.avatarUrl
                        )
                    }
                    Surface(
                        color = AccentSlate,
                        shape = CircleShape,
                        border = BorderStroke(2.dp, AppBackground),
                        modifier = Modifier.size(22.dp).clickable { imagePicker.launch("image/*") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Моя история", color = MainText, fontSize = 10.sp, maxLines = 1)
            }

            friendGroups.forEach { story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(66.dp).clickable { onOpenStories(story.userId) }
                ) {
                    Box(
                        Modifier.size(58.dp).background(
                            Brush.linearGradient(listOf(PaletteBlush, PaletteSlate, PaletteBlueGray)),
                            CircleShape
                        ).padding(3.dp)
                    ) {
                        Avatar(
                            story.displayName.ifBlank { story.username }.take(1).uppercase(),
                            story.userId.hashCode().ushr(1) % 4,
                            Modifier.fillMaxSize(),
                            highlighted = true,
                            imageUrl = story.avatarUrl
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        story.displayName.ifBlank { story.username },
                        color = MainText,
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
                color = Muted,
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
                                repository.publishStory(userId, prepared.bytes, caption)
                            }.onSuccess { published ->
                                stories = listOf(published) + stories
                                showComposer = false
                                selectedUri = null
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
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                java.net.URL(url).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "История",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier.background(Color(0xFF293444)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun StoryViewerScreen(
    storyOwnerId: String,
    currentUserId: String,
    repository: SupabaseChatRepository,
    onBack: () -> Unit
) {
    var stories by remember(storyOwnerId) { mutableStateOf<List<FaceVideoStoryCard>>(emptyList()) }
    var index by remember(storyOwnerId) { mutableStateOf(0) }
    var isLoading by remember(storyOwnerId) { mutableStateOf(true) }
    var errorMessage by remember(storyOwnerId) { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, storyOwnerId) {
        isLoading = true
        runCatching {
            stories = repository.listVisibleStories()
                .filter { it.userId == storyOwnerId }
                .sortedBy { it.createdAt }
            index = (stories.size - 1).coerceAtLeast(0)
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить историю."
        }
        isLoading = false
    }

    val current = stories.getOrNull(index)
    Column(
        Modifier.fillMaxSize().background(Color(0xFF202A38)).padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    current?.let { it.displayName.ifBlank { it.username } } ?: "История",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(current?.let { storyTimeLabel(it.createdAt) }.orEmpty(), color = Color.LightGray, fontSize = 10.sp)
            }
            if (current?.userId == currentUserId) {
                TextButton(onClick = { showDeleteConfirmation = true }, enabled = !deleting) {
                    Text("Удалить", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        } else if (current != null) {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                StoryImage(current.mediaUrl, Modifier.fillMaxSize(), ContentScale.Fit)
                if (current.caption.isNotBlank()) {
                    Surface(
                        color = Color(0xCC202A38),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
                    ) {
                        Text(
                            current.caption,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { index = (index - 1).coerceAtLeast(0) },
                    enabled = index > 0
                ) { Text("Назад", color = Color.White) }
                Text("${index + 1} / ${stories.size}", color = Color.LightGray, fontSize = 11.sp)
                TextButton(
                    onClick = {
                        if (index < stories.lastIndex) index++ else onBack()
                    }
                ) { Text("Следующая", color = Color.White) }
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: "Истории пока нет или срок её действия истёк.", color = Color.White, fontSize = 13.sp)
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
                        runCatching { repository.deleteStory(current.storyId, "${current.userId}/${current.storyId}.jpg") }
                            .onSuccess {
                                stories = stories.filterNot { it.storyId == current.storyId }
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
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
            .background(Color(0xFFF8F3F0)).padding(horizontal = 11.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(initials, chat.id.hashCode().ushr(1) % 4, Modifier.size(53.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(5.dp))
            Text(
                preview.text.ifBlank { "Начните переписку" },
                color = if (unreadCount > 0) MainText else Muted,
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
                color = if (unreadCount > 0) AccentSlate else Muted,
                fontSize = 10.sp
            )
            formatUnreadBadge(unreadCount)?.let { badgeText ->
                Spacer(Modifier.height(6.dp))
                Surface(color = AccentSlate, shape = CircleShape) {
                    Text(
                        badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Text("⋮", color = Muted, fontSize = 20.sp, modifier = Modifier.clickable { showMenu = true }.padding(start = 8.dp))
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
    var peerUserId by remember(repository, chat.id, userId) { mutableStateOf<String?>(null) }
    LaunchedEffect(repository, chat.id, userId, chat.isGroup) {
        peerUserId = if (chat.isGroup) {
            null
        } else {
            runCatching { repository.getDirectChatPeerId(chat.id, userId) }
                .onFailure {
                    android.util.Log.w("ArturChatPresence", "Could not resolve chat peer", it)
                }
                .getOrNull()
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

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedMessageIds.isNotEmpty()) {
                Text("×", color = AccentSlate, fontSize = 30.sp, modifier = Modifier.clickable { selectedMessageIds = emptySet() }.padding(horizontal = 8.dp))
                Text(
                    "${selectedMessageIds.size} выбрано",
                    color = MainText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text("‹", color = AccentSlate, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp))
                Avatar(
                    chat.title.take(2).uppercase(),
                    chat.id.hashCode().ushr(1) % 4,
                    Modifier.size(43.dp).clickable(enabled = peerUserId != null && !chat.isGroup) {
                        peerUserId?.let(onOpenProfile)
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(
                    Modifier.weight(1f).clickable(enabled = peerUserId != null && !chat.isGroup) {
                        peerUserId?.let(onOpenProfile)
                    }
                ) {
                    Text(chat.title, color = MainText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    val peerIsOnline = peerUserId != null && peerUserId in onlineUserIds
                    Text(
                        when {
                            chat.isGroup -> "Групповой чат"
                            !presenceAvailable -> "Статус недоступен"
                            peerUserId == null -> "Проверяем статус…"
                            peerIsOnline -> "В сети"
                            else -> "Не в сети"
                        },
                        color = if (peerIsOnline) AccentSlate else Muted,
                        fontSize = 10.sp
                    )
                }
                Text("☎", color = AccentSlate, fontSize = 21.sp, modifier = Modifier.clickable { onCall(false) }.padding(7.dp))
                Text("▣", color = AccentSlate, fontSize = 21.sp, modifier = Modifier.clickable { onCall(true) }.padding(7.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFC9D3DF)))

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
                            isDeleted = message.deletedAt != null
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
                color = PanelLight,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFC9D3DF)),
                shadowElevation = 4.dp,
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
                                repository.listMyChats().filter { it.id != chat.id }
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
                "Отправляем фото или сообщение…",
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = PanelLight,
                shape = CircleShape,
                modifier = Modifier.size(42.dp).clickable(enabled = !isSending) {
                    imagePicker.launch("image/*")
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = AccentSlate, fontSize = 25.sp)
                }
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                Modifier.weight(1f),
                color = Panel,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFC9D3DF))
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
                Modifier.size(43.dp).clip(CircleShape).background(if (isSending) Muted else AccentSlate)
                    .clickable(enabled = !isSending) { sendMessage() },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = Panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFC9D3DF))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MainText, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", color = AccentSlate, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text("Поиск по чатам", color = Muted, fontSize = 14.sp)
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
            .background(Color(0xFFF8F3F0)).padding(horizontal = 11.dp, vertical = 13.dp),
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
    imageUrl: String? = null
) {
    var avatarBitmap by remember(imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(imageUrl) {
        avatarBitmap = if (imageUrl.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                java.net.URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
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
            Text("‹", color = AccentSlate, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp))
            Avatar(chat.initials, chat.avatarStyle, Modifier.size(43.dp), highlighted = chat.online)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = MainText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (chat.online) "в сети" else "был(а) недавно", color = if (chat.online) AccentSlate else Muted, fontSize = 11.sp)
            }
            Text("☎", color = AccentSlate, fontSize = 21.sp, modifier = Modifier.clickable(onClick = onCall).padding(7.dp))
            Text("▣", color = AccentSlate, fontSize = 21.sp, modifier = Modifier.clickable(onClick = onCall).padding(7.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFC9D3DF)))

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
            Surface(color = PanelLight, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("+", color = AccentSlate, fontSize = 25.sp) }
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                Modifier.weight(1f),
                color = Panel,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFC9D3DF))
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
                Modifier.size(43.dp).clip(CircleShape).background(AccentSlate).clickable {
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
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        enabled = onSelect != null || onToggleSelection != null || onReply != null || onCopy != null,
                        onClick = { if (selected || onToggleSelection != null) onToggleSelection?.invoke() },
                        onLongClick = { if (onSelect != null) onSelect() else showActions = true }
                    ),
                color = if (selected) Color(0xFFD5DDE8) else if (message.isMine) Color(0xFFF5D2D0) else IncomingMessage,
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (message.isMine) 18.dp else 5.dp,
                    bottomEnd = if (message.isMine) 5.dp else 18.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 7.dp,
                        bottom = if (showTime) 4.dp else 7.dp
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
                                .background(if (message.isMine) Color(0xFFEEDBDA) else Color(0xFFD3DCE7))
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
                    if (attachmentPath != null && attachmentRepository != null) {
                        ChatImagePreview(
                            path = attachmentPath,
                            repository = attachmentRepository
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = messageText,
                        color = MainText,
                        fontSize = 14.sp
                    )
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(34.dp))
        Text("ARTURCHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.weight(1f))
        Avatar(peerName.take(2).uppercase(), peerName.hashCode().ushr(1) % 4, Modifier.size(116.dp))
        Spacer(Modifier.height(24.dp))
        Text(peerName, color = MainText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(status, color = AccentSlate, fontSize = 14.sp)
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = Color(0xFFA85F6E), fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (muted) Color(0xFFC9D3DF) else PanelLight,
                shape = CircleShape,
                modifier = Modifier.size(62.dp).clickable(onClick = onToggleMute)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (muted) "Микрофон выкл." else "Микрофон", color = MainText, fontSize = 10.sp)
                }
            }
            Surface(
                color = Color(0xFFA86F7A),
                shape = CircleShape,
                modifier = Modifier.size(72.dp).clickable(onClick = onEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(36.dp))
        Text("Звонок один на один · WebRTC", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun CallsScreen(onCall: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp)) {
        Text("ARTURCHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text("Звонки", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFFC9D3DF))) {
            Column(Modifier.fillMaxWidth().padding(19.dp)) {
                Text("Голосовая связь уже доступна", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Открой личную переписку и нажми на значок телефонной трубки. Входящий вызов появится прямо в приложении.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(17.dp))
                Text("Видеозвонки и групповые вызовы — следующий этап.", color = AccentSlate, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(25.dp))
        Text("ПОДСКАЗКА", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        Text("Для проверки нужны два аккаунта ArturChat на двух устройствах. Оба устройства должны быть подключены к интернету.", color = Muted, fontSize = 12.sp)
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
                Text("Друзья", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Люди, с которыми вы взаимно подружились", color = Muted, fontSize = 11.sp)
            }
            TextButton(onClick = { refreshKey++ }) { Text("Обновить", color = AccentSlate, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { showAddFriend = true }, modifier = Modifier.fillMaxWidth()) {
            Text("＋ Добавить друга")
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (selectedSection == FriendsSection.FRIENDS) AccentSlate else PanelLight,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).clickable { selectedSection = FriendsSection.FRIENDS }
            ) {
                Text(
                    "Друзья · ${friends.size}",
                    color = if (selectedSection == FriendsSection.FRIENDS) Color.White else MainText,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Surface(
                color = if (selectedSection == FriendsSection.REQUESTS) AccentSlate else PanelLight,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).clickable { selectedSection = FriendsSection.REQUESTS }
            ) {
                Text(
                    "Заявки · ${incoming.size + outgoing.size}",
                    color = if (selectedSection == FriendsSection.REQUESTS) Color.White else MainText,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
            }
        } else if (errorMessage != null) {
            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 12.sp)
            TextButton(onClick = { refreshKey++ }) { Text("Повторить", color = AccentSlate) }
        } else if (selectedSection == FriendsSection.FRIENDS) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Найти среди друзей") }
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
        color = Panel,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, PaletteMist),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                profile.displayName.ifBlank { profile.username }.take(1).uppercase(),
                profile.id.hashCode().ushr(1) % 4,
                Modifier.size(47.dp).clickable(onClick = onProfile),
                imageUrl = profile.avatarUrl
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable(onClick = onProfile)) {
                Text(profile.displayName.ifBlank { profile.username }, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("@${profile.username}", color = Muted, fontSize = 10.sp, maxLines = 1)
                Text(status, color = AccentSlate, fontSize = 10.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onAction, enabled = actionEnabled) {
                    Text(actionLabel, color = AccentSlate, fontSize = 10.sp)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    TextButton(onClick = onSecondaryAction, enabled = actionEnabled) {
                        Text(secondaryActionLabel, color = Color(0xFFA85F6E), fontSize = 9.sp)
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
                            2, Modifier.size(42.dp), imageUrl = foundProfile!!.avatarUrl
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
    onSignOut: () -> Unit
) {
    var profile by remember(userId) { mutableStateOf<FaceVideoProfile?>(null) }
    var username by remember(userId) { mutableStateOf("") }
    var displayName by remember(userId) { mutableStateOf("") }
    var statusText by remember(userId) { mutableStateOf("") }
    var about by remember(userId) { mutableStateOf("") }
    var isLoading by remember(userId) { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(repository, userId) {
        val currentRepository = repository
        val currentUserId = userId
        if (currentRepository != null && currentUserId != null) {
            runCatching { currentRepository.getMyProfile(currentUserId) }
                .onSuccess { loaded ->
                    profile = loaded
                    username = loaded?.username.orEmpty()
                    displayName = loaded?.displayName.orEmpty()
                    statusText = loaded?.statusText.orEmpty()
                    about = loaded?.about.orEmpty()
                    if (loaded == null) errorMessage = "Профиль пока не найден."
                }
                .onFailure { errorMessage = it.message ?: "Не удалось загрузить профиль." }
        }
        isLoading = false
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && repository != null && userId != null) {
            scope.launch {
                isSaving = true
                errorMessage = null
                runCatching {
                    val prepared = withContext(Dispatchers.IO) { prepareChatImage(context, uri) }
                    repository.uploadProfileAvatar(userId, prepared.bytes)
                }.onSuccess { url ->
                    runCatching {
                        repository.updateMyProfile(userId, username, displayName, statusText, about, url)
                    }.onSuccess {
                        profile = it
                        successMessage = "Фотография профиля обновлена."
                    }.onFailure { errorMessage = it.message ?: "Фото загружено, но профиль не обновился." }
                }.onFailure { errorMessage = it.message ?: "Не удалось загрузить фотографию." }
                isSaving = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("МОЙ ПРОФИЛЬ", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(18.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            Avatar(
                (displayName.ifBlank { username }.ifBlank { userEmail ?: "A" }).trim().take(1).uppercase(),
                1,
                Modifier.size(112.dp),
                highlighted = profile != null,
                imageUrl = profile?.avatarUrl
            )
            Surface(
                color = AccentSlate,
                shape = CircleShape,
                modifier = Modifier.size(34.dp).clickable(enabled = !isSaving) { avatarPicker.launch("image/*") }
            ) {
                Box(contentAlignment = Alignment.Center) { Text("✎", color = Color.White, fontSize = 17.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(displayName.ifBlank { "Ваш профиль" }, color = MainText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(if (username.isNotBlank()) "@$username" else userEmail ?: "Демонстрационный режим", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            Text("Загружаем профиль…", color = Muted, fontSize = 12.sp)
        } else if (repository != null && userId != null && profile != null) {
            Surface(color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, PaletteMist), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ЛИЧНЫЕ ДАННЫЕ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(48); successMessage = null },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Имя") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(24)
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Псевдоним") },
                        leadingIcon = { Text("@", color = AccentSlate) },
                        supportingText = { Text("3–24 символа: a–z, 0–9 и _") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("СТАТУС", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { statusText = it.take(80); successMessage = null },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Например: На работе · Не беспокоить") },
                        supportingText = { Text("${statusText.length}/80") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("О СЕБЕ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    OutlinedTextField(
                        value = about,
                        onValueChange = { about = it.take(500); successMessage = null },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5,
                        label = { Text("Расскажи немного о себе") },
                        supportingText = { Text("${about.length}/500") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Электронная почта", color = Muted, fontSize = 10.sp)
                    Text(userEmail.orEmpty(), color = MainText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                    if (errorMessage != null) Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 11.sp)
                    if (successMessage != null) Text(successMessage.orEmpty(), color = AccentSlate, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                successMessage = null
                                runCatching {
                                    repository.updateMyProfile(userId, username, displayName, statusText, about)
                                }.onSuccess {
                                    profile = it
                                    successMessage = "Изменения сохранены и доступны собеседникам."
                                }.onFailure { failure ->
                                    errorMessage = when {
                                        failure.message.orEmpty().contains("duplicate", true) || failure.message.orEmpty().contains("unique", true) ->
                                            "Этот псевдоним уже занят. Выбери другой."
                                        else -> failure.message ?: "Не удалось сохранить изменения."
                                    }
                                }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && username.length >= 3,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isSaving) "Сохраняем…" else "Сохранить профиль") }
                }
            }
        } else {
            Text(errorMessage ?: "Личный профиль доступен после входа в аккаунт ArturChat.", color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        Surface(color = PanelLight, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ВЕРСИЯ ПРИЛОЖЕНИЯ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("ArturChat", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("v${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(18.dp))
        if (userEmail != null) {
            Surface(color = PaletteBlush, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onSignOut)) {
                Text("Выйти из аккаунта", color = Color(0xFFA85F6E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun UserProfileScreen(
    userId: String,
    currentUserId: String,
    repository: SupabaseChatRepository,
    onlineUserIds: Set<String>,
    onBack: () -> Unit,
    onOpenChat: (FaceVideoChatSummary) -> Unit
) {
    var profile by remember(userId) { mutableStateOf<FaceVideoProfile?>(null) }
    var friendship by remember(userId, currentUserId) { mutableStateOf<com.facevideo.app.data.FaceVideoFriendship?>(null) }
    var isLoading by remember(userId) { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRemoveFriend by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId, currentUserId) {
        isLoading = true
        runCatching {
            profile = repository.getProfile(userId)
            friendship = repository.getFriendshipWith(currentUserId, userId)
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить профиль."
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
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
            }
        } else if (profile != null) {
            val p = profile!!
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(22.dp))
                Avatar(p.displayName.ifBlank { p.username }.take(1).uppercase(), 2, Modifier.size(124.dp), imageUrl = p.avatarUrl)
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
private fun BottomNavigation(
    selectedTab: MainTab,
    unreadChatsCount: Int,
    onSelect: (MainTab) -> Unit
) {
    Surface(color = Color(0xFFFFFCFA), shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.values().forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.symbol, color = if (selected) AccentSlate else Muted, fontSize = 20.sp)
                        if (tab == MainTab.CHATS) {
                            formatUnreadBadge(unreadChatsCount)?.let { badgeText ->
                                Spacer(Modifier.width(5.dp))
                                Surface(color = PaletteBlush, shape = CircleShape) {
                                    Text(
                                        badgeText,
                                        color = MainText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.title,
                        color = if (selected) AccentSlate else Muted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
