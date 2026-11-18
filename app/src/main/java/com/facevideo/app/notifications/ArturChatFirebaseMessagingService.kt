package com.facevideo.app.notifications

import android.util.Log
import com.facevideo.app.data.SupabaseChatRepository
import com.facevideo.app.data.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ArturChatFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenRegistrar.storeToken(applicationContext, token)
        Log.i(TAG, "FCM token refreshed and stored locally.")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"] ?: "message"
        val notificationId = data["entity_id"]
            ?: data["message_id"]
            ?: data["call_id"]
            ?: message.messageId
            ?: System.currentTimeMillis().toString()
        val chatId = data["chat_id"]
        if (
            type == "message" &&
            !chatId.isNullOrBlank() &&
            ActiveChatNotificationState.isChatVisible(this, chatId)
        ) {
            // FCM notification payloads are delivered to this callback while the
            // app is foregrounded. A visible chat is already the notification.
            AppNotificationManager(this).cancelChatNotifications(chatId, listOf(notificationId))
            val client = SupabaseClientProvider.client
            if (client != null) {
                serviceScope.launch {
                    runCatching { SupabaseChatRepository(client).markChatRead(chatId) }
                        .onFailure {
                            Log.w(TAG, "Could not mark visible chat as read from FCM", it)
                        }
                }
            }
            return
        }
        val title = message.notification?.title
            ?: data["title"]
            ?: if (type == "call") "Входящий звонок" else "Новое сообщение"
        val body = message.notification?.body
            ?: data["body"]
            ?: if (type == "call") "Тебе звонят в ArturChat" else "Открой ArturChat, чтобы прочитать сообщение"

        AppNotificationManager(this).showRemoteNotification(
            notificationId = notificationId,
            title = title,
            body = body,
            type = type,
            chatId = chatId
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ArturChatFCM"
    }
}
