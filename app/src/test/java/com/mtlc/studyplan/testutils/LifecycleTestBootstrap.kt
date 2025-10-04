package com.mtlc.studyplan.testutils

import android.content.Context

/**
 * Ensures ProcessLifecycleOwner is initialized in local unit tests (Robolectric).
 * In production this is done by the lifecycle-process ContentProvider; tests may need a manual bootstrap.
 */
object LifecycleTestBootstrap {
    fun ensureProcessLifecycleInitialized(context: Context) {
        // Best-effort: try public (but @RestrictTo) init via reflection; else try Robolectric shadow.
        try {
            val clazz = Class.forName("androidx.lifecycle.ProcessLifecycleOwner")
            val method = clazz.getDeclaredMethod("init", Context::class.java)
            method.isAccessible = true
            method.invoke(null, context.applicationContext)
            return
        } catch (_: Throwable) {
            // ignore and try Robolectric shadow
        }

        try {
            val shadowClazz = Class.forName("org.robolectric.androidx.lifecycle.ShadowProcessLifecycleOwner")
            val reset = shadowClazz.getDeclaredMethod("reset")
            reset.isAccessible = true
            reset.invoke(null)
        } catch (_: Throwable) {
            // If both strategies fail, leave as-is; tests that rely on it may still fail.
        }
    }
}

