package com.facevideo.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.facevideo.app.MainActivity
import com.facevideo.app.R

/**
 * Keeps an active WebRTC call eligible to continue while ArturChat is minimized.
 *
 * The service is started from the visible call flow, after RECORD_AUDIO/CAMERA
 * permissions have been granted. Its foreground-service types allow the
 * existing WebRTC client to keep using the microphone and, for video calls,
 * the camera while the Activity is in the background.
 */
class CallForegroundService : Service() {

    private var callWakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val peerName = intent?.getStringExtra(EXTRA_PEER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: "Собеседник"
        val mediaType = intent?.getStringExtra(EXTRA_MEDIA_TYPE) ?: MEDIA_AUDIO
        val isVideoCall = mediaType == MEDIA_VIDEO

        return try {
            val notification = buildOngoingCallNotification(peerName, isVideoCall)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (isVideoCall) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }

            acquireCallWakeLock()
            Log.i(TAG, "Foreground call service active; mediaType=$mediaType")
            START_NOT_STICKY
        } catch (error: Exception) {
            Log.e(TAG, "Could not start foreground call service", error)
            releaseCallWakeLock()
            stopSelf(startId)
            START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseCallWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        Log.i(TAG, "Foreground call service stopped")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Активный звонок",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Показывает, что звонок ArturChat продолжается в фоне"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
            )
        }
    }

    private fun buildOngoingCallNotification(
        peerName: String,
        isVideoCall: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(peerName)
            .setContentText(
                if (isVideoCall) {
                    "Идёт видеозвонок · Нажмите, чтобы вернуться"
                } else {
                    "Идёт аудиозвонок · Нажмите, чтобы вернуться"
                }
            )
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return notification.build()
    }

    private fun acquireCallWakeLock() {
        if (callWakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        callWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:ActiveWebRtcCall"
        ).apply {
            setReferenceCounted(false)
            // Held only while this foreground service represents an active call.
            acquire()
        }
    }

    private fun releaseCallWakeLock() {
        val wakeLock = callWakeLock
        callWakeLock = null
        if (wakeLock?.isHeld == true) {
            runCatching { wakeLock.release() }
                .onFailure { Log.w(TAG, "Could not release call wake lock", it) }
        }
    }

    companion object {
        private const val TAG = "ArturChatCallService"
        private const val CHANNEL_ID = "arturchat_active_call"
        private const val NOTIFICATION_ID = 74045

        private const val ACTION_START = "com.facevideo.app.call.START_FOREGROUND"
        private const val ACTION_STOP = "com.facevideo.app.call.STOP_FOREGROUND"
        private const val EXTRA_PEER_NAME = "com.facevideo.app.call.PEER_NAME"
        private const val EXTRA_MEDIA_TYPE = "com.facevideo.app.call.MEDIA_TYPE"
        private const val MEDIA_AUDIO = "audio"
        private const val MEDIA_VIDEO = "video"

        /**
         * Must be called from the visible call UI, before the app is minimized.
         */
        fun start(context: Context, peerName: String, mediaType: String) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, CallForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PEER_NAME, peerName)
                putExtra(
                    EXTRA_MEDIA_TYPE,
                    if (mediaType == MEDIA_VIDEO) MEDIA_VIDEO else MEDIA_AUDIO
                )
            }
            ContextCompat.startForegroundService(appContext, intent)
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            appContext.stopService(Intent(appContext, CallForegroundService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
