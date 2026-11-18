package com.facevideo.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ArturChatFirebaseMessagingService : FirebaseMessagingService() {

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
            return
        }
        val title = message.notification?.title
            ?: data["title"]
            ?: if (type == "call") "Входящий звонок" else "Новое сообщение"
        val body = message.notification?.body
            ?: data["body"]
            ?: if (type == "call") "Тебе звонят в ArturChat" else "Открой ArturChat, чтобы прочитать сообщение"

        if (type == "message") {
            if (chatId.isNullOrBlank()) {
                Log.w(TAG, "Ignoring message push without chat_id.")
                return
            }
            val timestampMillis = data["created_at"]?.let { raw ->
                runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrNull()
            } ?: System.currentTimeMillis()
            AppNotificationManager(this).showMessage(
                notificationId = notificationId,
                title = title,
                body = body,
                chatId = chatId,
                senderId = data["sender_id"].orEmpty(),
                senderAvatarUrl = data["sender_avatar_url"],
                timestampMillis = timestampMillis
            )
            return
        }

        AppNotificationManager(this).showRemoteNotification(
            notificationId = notificationId,
            title = title,
            body = body,
            type = type,
            chatId = chatId
        )
    }

    private companion object {
        const val TAG = "ArturChatFCM"
    }
}
