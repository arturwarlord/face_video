package com.facevideo.app.notifications

import android.content.Context
import android.util.Log
import com.facevideo.app.BuildConfig
import com.facevideo.app.data.SupabaseChatRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PushTokenRegistrar {
    private const val PREFS_NAME = "arturchat_push"
    private const val TOKEN_KEY = "fcm_token"

    suspend fun register(context: Context, repository: SupabaseChatRepository, userId: String) {
        if (!BuildConfig.FIREBASE_CONFIGURED || FirebaseApp.getApps(context).isEmpty()) {
            Log.i("ArturChatPush", "Firebase config is not installed yet; skipping FCM registration.")
            return
        }

        val token = awaitFcmToken()
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousToken = preferences.getString(TOKEN_KEY, null)
        repository.registerPushToken(userId, token)

        if (!previousToken.isNullOrBlank() && previousToken != token) {
            runCatching { repository.removePushToken(userId, previousToken) }
                .onFailure { Log.w("ArturChatPush", "Could not remove an expired FCM token.", it) }
        }
        preferences.edit().putString(TOKEN_KEY, token).apply()
        Log.i("ArturChatPush", "FCM token registered for current account.")
    }

    suspend fun unregister(context: Context, repository: SupabaseChatRepository, userId: String) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = preferences.getString(TOKEN_KEY, null) ?: return
        repository.removePushToken(userId, token)
        preferences.edit().remove(TOKEN_KEY).apply()
    }

    fun storeToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(TOKEN_KEY, token).apply()
    }

    private suspend fun awaitFcmToken(): String =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (continuation.isActive) continuation.resume(token)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
}
