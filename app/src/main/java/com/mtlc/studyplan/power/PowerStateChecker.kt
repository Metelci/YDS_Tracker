package com.mtlc.studyplan.power

import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * Centralized power state checker with a simple test override hook.
 */
object PowerStateChecker {
    // When non-null, overrides real checks. Intended for tests only.
    @Volatile
    private var forceConstrained: Boolean? = null

    fun isPowerConstrained(context: Context): Boolean {
        val forced = forceConstrained
        if (forced != null) return forced

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val inPowerSaver = pm.isPowerSaveMode
        val inDoze = Build.VERSION.SDK_INT >= 23 && pm.isDeviceIdleMode
        return inPowerSaver || inDoze
    }

    // Visible for testing
    fun overrideForTests(constrained: Boolean?) {
        forceConstrained = constrained
    }
}

