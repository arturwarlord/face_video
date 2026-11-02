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
import android.graphics.drawable.Icon
import android.os.Build
import com.facevideo.app.MainActivity
import com.facevideo.app.R

internal object CallNotificationIntents {
    const val EXTRA_CALL_ID = "com.facevideo.app.EXTRA_NOTIFICATION_CALL_ID"
    const val EXTRA_CALL_ACTION = "com.facevideo.app.EXTRA_NOTIFICATION_CALL_ACTION"
    const val ACTION_ANSWER = "answer"
    const val ACTION_DECLINE = "decline"
}

/**
 * Displays Android notifications for incoming messages received through the
 * app's live Supabase Realtime subscription. Reliable delivery after process
 * death requires a separate FCM integration.
 */
class AppNotificationManager(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(MESSAGE_CHANNEL_ID, "Сообщения", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Новые сообщения в ArturChat"
                        enableVibration(true)
                        setShowBadge(true)
                    },
                    NotificationChannel(CALL_CHANNEL_ID, "Звонки", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Входящие звонки ArturChat"
                        enableVibration(true)
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
        type: String
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
            .setVisibility(Notification.VISIBILITY_PRIVATE)
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

        manager.notify(notificationId.hashCode(), builder.build())
    }

    fun cancelNotification(notificationId: String) {
        manager.cancel(notificationId.hashCode())
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

    fun showMessage(notificationId: String, title: String, body: String) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(appContext, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.take(140))
            .setStyle(Notification.BigTextStyle().bigText(body.take(1000)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()

        manager.notify(notificationId.hashCode(), notification)
    }

    private companion object {
        const val MESSAGE_CHANNEL_ID = "arturchat_messages"
        const val CALL_CHANNEL_ID = "arturchat_calls"
    }
}
