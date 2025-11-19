package com.mtlc.studyplan.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Centralized Toast Management System
 * Provides consistent user feedback across the entire app
 */
object ToastManager {

    private var applicationContext: Context? = null
    private var currentToast: Toast? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    @MainThread
    fun showSuccess(message: String, duration: Int = Toast.LENGTH_SHORT) {
        showToast("✅ $message", duration)
    }

    @MainThread
    fun showError(message: String, duration: Int = Toast.LENGTH_LONG) {
        showToast("❌ $message", duration)
    }

    @MainThread
    fun showWarning(message: String, duration: Int = Toast.LENGTH_LONG) {
        showToast("⚠️ $message", duration)
    }

    @MainThread
    fun showInfo(message: String, duration: Int = Toast.LENGTH_SHORT) {
        showToast("ℹ️ $message", duration)
    }

    @MainThread
    fun showPlain(message: String, duration: Int = Toast.LENGTH_SHORT) {
        showToast(message, duration)
    }

    private fun showToast(message: String, duration: Int) {
        val context = applicationContext ?: return

        // Cancel previous toast to avoid queue buildup
        currentToast?.cancel()

        currentToast = Toast.makeText(context, message, duration).apply {
            show()
        }
    }

    // Thread-safe versions for use from background threads
    fun showSuccessAsync(message: String, duration: Int = Toast.LENGTH_SHORT) {
        scope.launch {
            showSuccess(message, duration)
        }
    }

    fun showErrorAsync(message: String, duration: Int = Toast.LENGTH_LONG) {
        scope.launch {
            showError(message, duration)
        }
    }

    fun showWarningAsync(message: String, duration: Int = Toast.LENGTH_LONG) {
        scope.launch {
            showWarning(message, duration)
        }
    }

    fun showInfoAsync(message: String, duration: Int = Toast.LENGTH_SHORT) {
        scope.launch {
            showInfo(message, duration)
        }
    }
}
