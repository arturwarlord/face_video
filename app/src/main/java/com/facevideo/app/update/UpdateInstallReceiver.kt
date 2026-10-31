package com.facevideo.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val systemMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            ?.takeIf { it.isNotBlank() }

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                if (confirmationIntent == null) {
                    reportFailure(
                        context,
                        preferences,
                        "Android не вернул окно подтверждения установки. Повтори обновление."
                    )
                    return
                }

                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    preferences.edit()
                        .putString(KEY_INSTALL_STATE, "awaiting_confirmation")
                        .putString(KEY_INSTALL_MESSAGE, "Ожидается подтверждение установки Android.")
                        .putLong(KEY_INSTALL_UPDATED_AT, System.currentTimeMillis())
                        .apply()
                    context.startActivity(confirmationIntent)
                } catch (error: Exception) {
                    Log.e(TAG, "Could not open Android package installer confirmation", error)
                    reportFailure(
                        context,
                        preferences,
                        "Не удалось открыть окно установки Android: ${error.message ?: "неизвестная ошибка"}"
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                preferences.edit()
                    .putString(KEY_INSTALL_STATE, "success")
                    .putString(KEY_INSTALL_MESSAGE, "ArturChat успешно обновлён.")
                    .putLong(KEY_INSTALL_UPDATED_AT, System.currentTimeMillis())
                    .apply()
                Toast.makeText(context, "ArturChat успешно обновлён", Toast.LENGTH_LONG).show()
                Log.i(TAG, "PackageInstaller completed ArturChat update successfully")
            }

            else -> {
                val details = buildString {
                    append(statusDescription(status))
                    append(" (код ")
                    append(status)
                    append(")")
                    if (!systemMessage.isNullOrBlank()) {
                        append(": ")
                        append(systemMessage)
                    }
                }
                reportFailure(context, preferences, details)
            }
        }
    }

    private fun reportFailure(
        context: Context,
        preferences: android.content.SharedPreferences,
        message: String
    ) {
        preferences.edit()
            .putString(KEY_INSTALL_STATE, "failure")
            .putString(KEY_INSTALL_MESSAGE, message)
            .putLong(KEY_INSTALL_UPDATED_AT, System.currentTimeMillis())
            .apply()
        Log.e(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun statusDescription(status: Int): String = when (status) {
        PackageInstaller.STATUS_FAILURE_BLOCKED ->
            "Установка заблокирована системой или администратором устройства"
        PackageInstaller.STATUS_FAILURE_ABORTED ->
            "Установка отменена"
        PackageInstaller.STATUS_FAILURE_INVALID ->
            "Android признал APK некорректным"
        PackageInstaller.STATUS_FAILURE_CONFLICT ->
            "Конфликт установки. Возможно, установлена версия с другой подписью"
        PackageInstaller.STATUS_FAILURE_STORAGE ->
            "Недостаточно места для установки обновления"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
            "Эта версия несовместима с устройством"
        PackageInstaller.STATUS_FAILURE_TIMEOUT ->
            "Истекло время ожидания установки"
        else ->
            "Android не смог установить обновление"
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.facevideo.app.action.UPDATE_INSTALL_STATUS"
        const val PREFERENCES = "arturchat_updates"
        const val KEY_INSTALL_STATE = "last_install_state"
        const val KEY_INSTALL_MESSAGE = "last_install_message"
        const val KEY_INSTALL_UPDATED_AT = "last_install_updated_at"
        private const val TAG = "ArturChatUpdater"
    }
}
