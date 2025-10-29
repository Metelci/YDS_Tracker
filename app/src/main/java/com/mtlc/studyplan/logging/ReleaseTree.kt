package com.mtlc.studyplan.logging

import android.util.Log
import timber.log.Timber

/**
 * Custom Timber tree for production builds
 * Filters out verbose and debug logs, keeps info/warning/error for analytics
 */
class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log Warning, Error, and Assert in production
        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }

        // Log to production analytics/crash reporting
        when (priority) {
            Log.WARN -> logWarning(tag, message, t)
            Log.ERROR -> logError(tag, message, t)
            Log.ASSERT -> logAssertion(tag, message, t)
        }
    }

    private fun logWarning(tag: String?, message: String, t: Throwable?) {
        // Send to Firebase Crashlytics or similar
        Log.w("StudyPlan", "[$tag] $message", t)
    }

    private fun logError(tag: String?, message: String, t: Throwable?) {
        // Send to Firebase Crashlytics or similar
        Log.e("StudyPlan", "[$tag] $message", t)
        if (t != null) {
            // Optionally report to crash analytics
        }
    }

    private fun logAssertion(tag: String?, message: String, t: Throwable?) {
        Log.wtf("StudyPlan", "[$tag] $message", t)
    }
}
