package com.facevideo.app.update

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transfers PackageInstaller's user-confirmation intent from the system callback
 * to the visible Compose screen. The Activity launches it only after an explicit
 * user tap, avoiding background-activity-launch restrictions.
 */
internal object UpdateInstallCoordinator {
    private val _pendingConfirmationIntent = MutableStateFlow<Intent?>(null)
    val pendingConfirmationIntent: StateFlow<Intent?> = _pendingConfirmationIntent

    fun requireUserAction(intent: Intent) {
        _pendingConfirmationIntent.value = Intent(intent)
    }

    fun clearPendingAction() {
        _pendingConfirmationIntent.value = null
    }
}
