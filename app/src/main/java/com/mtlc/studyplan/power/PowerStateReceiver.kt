package com.mtlc.studyplan.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.mtlc.studyplan.calendar.CalendarWorkerManager

/**
 * Reacts to power/battery state changes to make background work battery-aware.
 * - Defers heavy work when power saver or battery is low
 * - Triggers catch-up sync when conditions improve
 */
class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BATTERY_LOW -> {
                // No-op: WorkManager constraints already defer; avoid forcing work
            }
            Intent.ACTION_BATTERY_OKAY -> {
                // Conditions improved; request a catch-up calendar sync
                CalendarWorkerManager.requestImmediateSync(context.applicationContext)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                // On charge, it's a good time to sync
                CalendarWorkerManager.requestImmediateSync(context.applicationContext)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // No-op
            }
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val powerSave = pm.isPowerSaveMode
                if (!powerSave) {
                    // Power saver off → attempt catch-up sync
                    CalendarWorkerManager.requestImmediateSync(context.applicationContext)
                }
            }
        }
    }
}

