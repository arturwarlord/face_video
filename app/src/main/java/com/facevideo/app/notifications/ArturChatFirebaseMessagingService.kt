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
            type = type
        )
    }

    private companion object {
        const val TAG = "ArturChatFCM"
    }
}
