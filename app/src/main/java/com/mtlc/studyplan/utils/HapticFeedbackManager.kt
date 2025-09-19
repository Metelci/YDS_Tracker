package com.mtlc.studyplan.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticFeedbackManager {

    private var isEnabled = true

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun performHapticFeedback(context: Context, type: HapticType = HapticType.LIGHT_CLICK) {
        if (!isEnabled) return

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.LIGHT_CLICK -> VibrationEffect.createOneShot(50, 100)
                    HapticType.MEDIUM_CLICK -> VibrationEffect.createOneShot(100, 150)
                    HapticType.HEAVY_CLICK -> VibrationEffect.createOneShot(150, 255)
                    HapticType.SUCCESS -> VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 50, 100),
                        intArrayOf(0, 150, 0, 200),
                        -1
                    )
                    HapticType.ERROR -> VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.LIGHT_CLICK -> vibrator.vibrate(50)
                    HapticType.MEDIUM_CLICK -> vibrator.vibrate(100)
                    HapticType.HEAVY_CLICK -> vibrator.vibrate(150)
                    HapticType.SUCCESS -> vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                    HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
                }
            }
        } catch (e: Exception) {
            // Handle vibrator access failure gracefully
        }
    }
}

enum class HapticType {
    LIGHT_CLICK,
    MEDIUM_CLICK,
    HEAVY_CLICK,
    SUCCESS,
    ERROR
}