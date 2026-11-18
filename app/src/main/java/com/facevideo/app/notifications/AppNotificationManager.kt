package com.facevideo.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.facevideo.app.MainActivity
import com.facevideo.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private object NotificationAvatarCache {
    private val cache = android.util.LruCache<String, Bitmap>(12)
    fun get(url: String): Bitmap? = cache.get(url)
    fun put(url: String, bitmap: Bitmap) { cache.put(url, bitmap) }
}

@Serializable
private data class StoredNotificationMessage(
    val id: String,
    val text: String,
    val timestampMillis: Long,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null
)

/**
 * Tracks the chat that is actually visible to the user. The foreground flag is
 * maintained by MainActivity so a backgrounded Activity never suppresses FCM alerts.
 */
internal object ActiveChatNotificationState {
    private const val PREFERENCES = "arturchat_notification_state"
    private const val KEY_ACTIVE_CHAT = "active_chat_id"
    private const val KEY_APP_FOREGROUND = "app_foreground"

    fun setAppForeground(context: Context, foreground: Boolean) {
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_FOREGROUND, foreground)
            .apply()
    }

    fun setActiveChat(context: Context, chatId: String) {
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_CHAT, chatId)
            .apply()
    }

    fun clearActiveChat(context: Context, chatId: String) {
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getString(KEY_ACTIVE_CHAT, null) == chatId) {
            preferences.edit().remove(KEY_ACTIVE_CHAT).apply()
        }
    }

    fun clearAnyActiveChat(context: Context) {
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_CHAT)
            .apply()
    }

    fun isChatVisible(context: Context, chatId: String): Boolean {
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_APP_FOREGROUND, false) &&
            preferences.getString(KEY_ACTIVE_CHAT, null) == chatId
    }
}

internal object CallNotificationIntents {
    const val EXTRA_CALL_ID = "com.facevideo.app.EXTRA_NOTIFICATION_CALL_ID"
    const val EXTRA_CALL_ACTION = "com.facevideo.app.EXTRA_NOTIFICATION_CALL_ACTION"
    const val ACTION_ANSWER = "answer"
    const val ACTION_DECLINE = "decline"
}

/**
 * Displays local Realtime notifications and remote FCM notifications.
 * Incoming-call notifications use Android CallStyle on API 31+ and expose
 * Accept/Decline actions that are routed through the signed-in app session.
 */
class AppNotificationManager(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var incomingRingtonePlayer: MediaPlayer? = null
    @Volatile private var incomingCallVibrator: Vibrator? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(MESSAGE_CHANNEL_ID, "Сообщения", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Новые сообщения в ArturChat"
                        enableVibration(true)
                        setShowBadge(true)
                    },
                    NotificationChannel(CALL_CHANNEL_ID, "Входящие звонки", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Аудио- и видеозвонки ArturChat"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 700, 400, 700, 400, 700)
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setShowBadge(true)
                    }
                )
            )
        }
    }

    fun showRemoteNotification(
        notificationId: String,
        title: String,
        body: String,
        type: String,
        chatId: String? = null
    ) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val isCall = type == "call"
        val channelId = if (isCall) CALL_CHANNEL_ID else MESSAGE_CHANNEL_ID
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.take(140))
            .setStyle(Notification.BigTextStyle().bigText(body.take(1000)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(!isCall)
            .setOngoing(isCall)
            .setCategory(if (isCall) Notification.CATEGORY_CALL else Notification.CATEGORY_MESSAGE)
            .setVisibility(if (isCall) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_PRIVATE)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)

        if (isCall && notificationId.isNotBlank()) {
            val answerIntent = callActionPendingIntent(notificationId, CallNotificationIntents.ACTION_ANSWER)
            val declineIntent = callActionPendingIntent(notificationId, CallNotificationIntents.ACTION_DECLINE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val caller = Person.Builder().setName(title).setImportant(true).build()
                builder.setStyle(
                    Notification.CallStyle.forIncomingCall(caller, declineIntent, answerIntent)
                )
            } else {
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(appContext, R.drawable.ic_notification),
                        "Отклонить",
                        declineIntent
                    ).build()
                )
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(appContext, R.drawable.ic_notification),
                        "Принять",
                        answerIntent
                    ).build()
                )
            }
        }

        val notification = builder.build()
        if (!isCall && !chatId.isNullOrBlank()) {
            manager.notify(messageNotificationTag(chatId), MESSAGE_NOTIFICATION_ID, notification)
        } else {
            manager.notify(notificationId.hashCode(), notification)
        }
    }

    fun cancelNotification(notificationId: String) {
        manager.cancel(notificationId.hashCode())
    }

    /**
     * Clears the chat-level FCM notification and older per-message notifications
     * created by previous app versions.
     */
    fun cancelChatNotifications(chatId: String, messageIds: Collection<String> = emptyList()) {
        if (chatId.isBlank()) return
        manager.cancel(messageNotificationTag(chatId), MESSAGE_NOTIFICATION_ID)
        synchronized(MESSAGE_HISTORY_LOCK) {
            appContext.getSharedPreferences(MESSAGE_HISTORY_PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(MESSAGE_HISTORY_PREFIX + chatId.replace(Regex("[^A-Za-z0-9_-]"), "_"))
                .apply()
        }
        messageIds.forEach { messageId ->
            if (messageId.isNotBlank()) manager.cancel(messageId.hashCode())
        }
    }

    /** Play the device ringtone and vibration while an incoming call is waiting. */
    @Synchronized
    fun startIncomingCallAlert() {
        if (incomingRingtonePlayer != null) return
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (ringtoneUri != null) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                player.isLooping = true
                player.setDataSource(appContext, ringtoneUri)
                player.setOnPreparedListener { prepared ->
                    if (incomingRingtonePlayer === prepared) {
                        runCatching { prepared.start() }
                            .onFailure { android.util.Log.w(TAG, "Could not start incoming ringtone", it) }
                    } else {
                        runCatching { prepared.release() }
                    }
                }
                player.setOnErrorListener { failed, _, _ ->
                    synchronized(this) {
                        if (incomingRingtonePlayer === failed) incomingRingtonePlayer = null
                    }
                    runCatching { failed.release() }
                    true
                }
                incomingRingtonePlayer = player
                player.prepareAsync()
            } catch (error: Exception) {
                incomingRingtonePlayer = null
                runCatching { player.release() }
                android.util.Log.w(TAG, "Could not prepare incoming ringtone", error)
            }
        }

        @Suppress("DEPRECATION")
        runCatching {
            val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                incomingCallVibrator = vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 650, 450), 0))
                } else {
                    vibrator.vibrate(longArrayOf(0, 650, 450), 0)
                }
            }
        }.onFailure { android.util.Log.w(TAG, "Could not vibrate for incoming call", it) }
    }

    @Synchronized
    fun stopIncomingCallAlert() {
        val player = incomingRingtonePlayer
        incomingRingtonePlayer = null
        if (player != null) {
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        val vibrator = incomingCallVibrator
        incomingCallVibrator = null
        runCatching { vibrator?.cancel() }
    }

    private fun callActionPendingIntent(callId: String, action: String): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            this.action = "com.facevideo.app.CALL_NOTIFICATION.$action.$callId"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallNotificationIntents.EXTRA_CALL_ID, callId)
            putExtra(CallNotificationIntents.EXTRA_CALL_ACTION, action)
        }
        val requestCode = "${callId}:${action}".hashCode()
        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Posts one conversation-style notification per chat. Messages are retained in a
     * small local history so Android can render a Telegram-like stacked conversation.
     * Avatar downloads run off the main thread and are cached in memory.
     */
    fun showMessage(
        notificationId: String,
        title: String,
        body: String,
        chatId: String? = null,
        senderId: String = "",
        senderAvatarUrl: String? = null,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val conversationId = chatId?.takeIf { it.isNotBlank() } ?: notificationId
        notificationScope.launch {
            val update = updateMessageHistory(
                conversationId = conversationId,
                notificationId = notificationId,
                body = body,
                senderId = senderId,
                senderName = title.ifBlank { "Новое сообщение" },
                senderAvatarUrl = senderAvatarUrl,
                timestampMillis = timestampMillis
            )

            val avatarBitmaps = update.messages.mapNotNull { it.senderAvatarUrl }
                .distinct()
                .associateWithNotNull { url ->
                    NotificationAvatarCache.get(url) ?: downloadAvatar(url)?.also {
                        NotificationAvatarCache.put(url, it)
                    }
                }
            val latestAvatar = update.messages.lastOrNull()?.senderAvatarUrl?.let(avatarBitmaps::get)

            val messagingStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val self = Person.Builder().setName("ArturChat").setKey("arturchat-self").build()
                Notification.MessagingStyle(self).setGroupConversation(false)
            } else {
                Notification.MessagingStyle("ArturChat")
            }

            update.messages.forEach { stored ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val senderBuilder = Person.Builder()
                        .setName(stored.senderName.ifBlank { "Собеседник" })
                        .setKey(stored.senderId.ifBlank { stored.senderName })
                    stored.senderAvatarUrl?.let(avatarBitmaps::get)?.let { bitmap ->
                        senderBuilder.setIcon(Icon.createWithBitmap(bitmap))
                    }
                    messagingStyle.addMessage(
                        Notification.MessagingStyle.Message(
                            stored.text,
                            stored.timestampMillis,
                            senderBuilder.build()
                        )
                    )
                } else {
                    messagingStyle.addMessage(
                        stored.text,
                        stored.timestampMillis,
                        stored.senderName
                    )
                }
            }

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                conversationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = Notification.Builder(appContext, MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title.ifBlank { "Новое сообщение" })
                .setContentText(body.take(140))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setOnlyAlertOnce(!update.isNewMessage)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .setStyle(messagingStyle)

            latestAvatar?.let(builder::setLargeIcon)
            manager.notify(messageNotificationTag(conversationId), MESSAGE_NOTIFICATION_ID, builder.build())
        }
    }

    private data class MessageHistoryUpdate(
        val messages: List<StoredNotificationMessage>,
        val isNewMessage: Boolean
    )

    private fun updateMessageHistory(
        conversationId: String,
        notificationId: String,
        body: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        timestampMillis: Long
    ): MessageHistoryUpdate = synchronized(MESSAGE_HISTORY_LOCK) {
        val preferences = appContext.getSharedPreferences(MESSAGE_HISTORY_PREFS, Context.MODE_PRIVATE)
        val key = MESSAGE_HISTORY_PREFIX + conversationId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val existing = runCatching {
            messageHistoryJson.decodeFromString<List<StoredNotificationMessage>>(
                preferences.getString(key, null).orEmpty()
            )
        }.getOrDefault(emptyList())
        val fresh = existing.filter { timestampMillis - it.timestampMillis <= MESSAGE_HISTORY_MAX_AGE_MS }
        val previous = fresh.firstOrNull { it.id == notificationId }
        val incoming = StoredNotificationMessage(
            id = notificationId,
            text = body,
            timestampMillis = previous?.timestampMillis ?: timestampMillis,
            senderId = senderId.ifBlank { previous?.senderId.orEmpty() },
            senderName = senderName.ifBlank { previous?.senderName ?: "Собеседник" },
            senderAvatarUrl = senderAvatarUrl?.takeIf { it.isNotBlank() } ?: previous?.senderAvatarUrl
        )
        val merged = if (previous == null) fresh + incoming else fresh.map {
            if (it.id == notificationId) incoming else it
        }
        val bounded = merged.sortedBy { it.timestampMillis }.takeLast(MAX_NOTIFICATION_MESSAGES)
        preferences.edit().putString(key, messageHistoryJson.encodeToString(bounded)).apply()
        MessageHistoryUpdate(bounded, previous == null)
    }

    private fun downloadAvatar(avatarUrl: String): Bitmap? {
        if (avatarUrl.length > 2048 || !avatarUrl.startsWith("https://", ignoreCase = true)) return null
        val connection = runCatching { URL(avatarUrl).openConnection() as? HttpURLConnection }.getOrNull()
            ?: return null
        return try {
            connection.connectTimeout = AVATAR_CONNECT_TIMEOUT_MS
            connection.readTimeout = AVATAR_READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            if (!connection.contentType.orEmpty().startsWith("image/", ignoreCase = true)) return null
            if (connection.contentLengthLong > MAX_AVATAR_BYTES) return null

            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_AVATAR_BYTES) return null
                    output.write(buffer, 0, count)
                }
            }
            val decoded = BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
                ?: return null
            val largest = maxOf(decoded.width, decoded.height)
            if (largest <= MAX_AVATAR_EDGE) return decoded
            val scale = MAX_AVATAR_EDGE.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } catch (error: Exception) {
            android.util.Log.w(TAG, "Could not load notification avatar", error)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun messageNotificationTag(chatId: String): String =
        MESSAGE_NOTIFICATION_TAG_PREFIX + chatId

    private fun <K, V : Any> Iterable<K>.associateWithNotNull(transform: (K) -> V?): Map<K, V> =
        mapNotNull { key -> transform(key)?.let { key to it } }.toMap()

    private companion object {
        val MESSAGE_HISTORY_LOCK = Any()
        val messageHistoryJson = Json { ignoreUnknownKeys = true }
        const val MESSAGE_HISTORY_PREFS = "arturchat_notification_history"
        const val MESSAGE_HISTORY_PREFIX = "conversation_"
        const val MESSAGE_HISTORY_MAX_AGE_MS = 24L * 60L * 60L * 1000L
        const val MAX_NOTIFICATION_MESSAGES = 8
        const val MAX_AVATAR_BYTES = 1024 * 1024
        const val MAX_AVATAR_EDGE = 160
        const val AVATAR_CONNECT_TIMEOUT_MS = 3000
        const val AVATAR_READ_TIMEOUT_MS = 3000
        const val MESSAGE_CHANNEL_ID = "arturchat_messages"
        const val MESSAGE_NOTIFICATION_TAG_PREFIX = "arturchat-chat-"
        const val MESSAGE_NOTIFICATION_ID = 0
        const val CALL_CHANNEL_ID = "arturchat_calls_v2"
        const val TAG = "ArturChatCallAlert"
    }
}
