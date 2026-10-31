package com.facevideo.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmationIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirmationIntent?.let(context::startActivity)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                context.getSharedPreferences("arturchat_updates", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_successful_update_at", System.currentTimeMillis())
                    .apply()
            }
            else -> Unit
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.facevideo.app.action.UPDATE_INSTALL_STATUS"
    }
}
