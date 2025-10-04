package com.mtlc.studyplan.notifications

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages push notification behavior based on battery optimization modes
 */
@Singleton
class BatteryAwarePushManager @Inject constructor() {
    companion object {
        private const val TAG = "BatteryAwarePushManager"
    }

    private var dozeModeEnabled = false
    private var lowPowerModeEnabled = false
    private var criticalPowerModeEnabled = false

    fun setDozeMode(enabled: Boolean) {
        dozeModeEnabled = enabled
        Log.d(TAG, "Doze mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun setLowPowerMode(enabled: Boolean) {
        lowPowerModeEnabled = enabled
        Log.d(TAG, "Low power mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun setCriticalPowerMode(enabled: Boolean) {
        criticalPowerModeEnabled = enabled
        Log.d(TAG, "Critical power mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun shouldSendNotification(messageType: PushMessageType): Boolean {
        return when {
            criticalPowerModeEnabled -> {
                // Only allow essential notifications in critical mode
                messageType in listOf(PushMessageType.EXAM_UPDATE, PushMessageType.SYSTEM)
            }
            dozeModeEnabled -> {
                // No notifications during Doze mode
                false
            }
            lowPowerModeEnabled -> {
                // Reduce frequency for non-essential notifications
                messageType in listOf(
                    PushMessageType.STUDY_REMINDER,
                    PushMessageType.EXAM_UPDATE,
                    PushMessageType.SYSTEM
                )
            }
            else -> true // Normal operation
        }
    }
}
