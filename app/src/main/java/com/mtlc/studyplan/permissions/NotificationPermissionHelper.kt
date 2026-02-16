package com.mtlc.studyplan.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper object for requesting and checking POST_NOTIFICATIONS permission.
 * Required for Android 13+ (API 33+) to show local notifications.
 */
object NotificationPermissionHelper {

    /**
     * Checks if the POST_NOTIFICATIONS permission is granted.
     * On Android 13+ (API 33+), this checks the runtime permission.
     * On earlier versions, returns true as permission is not required.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 13 doesn't need runtime permission
        }
    }

    /**
     * Checks if we should show permission rationale to the user.
     * Returns true if user previously denied the permission.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }

    /**
     * Creates an ActivityResultLauncher for requesting POST_NOTIFICATIONS permission.
     * Must be called before Activity onCreate completes.
     *
     * @param activity The ComponentActivity requesting permission (must be ComponentActivity for registerForActivityResult)
     * @param onGranted Callback when permission is granted
     * @param onDenied Callback when permission is denied
     */
    fun createPermissionLauncher(
        activity: ComponentActivity,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}
