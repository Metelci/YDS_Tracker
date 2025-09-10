package com.mtlc.studyplan.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mtlc.studyplan.data.muteToday
import com.mtlc.studyplan.workers.ReminderWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_SNOOZE_30M -> {
                val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(30, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueue(req)
            }
            ACTION_MUTE_TODAY -> {
                CoroutineScope(Dispatchers.IO).launch { muteToday(context) }
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE_30M = "com.mtlc.studyplan.action.SNOOZE_30M"
        const val ACTION_MUTE_TODAY = "com.mtlc.studyplan.action.MUTE_TODAY"
    }
}

