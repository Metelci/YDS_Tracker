package com.mtlc.studyplan.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mtlc.studyplan.services.NotificationSchedulerService
import com.mtlc.studyplan.workers.DailyStudyReminderWorker
import org.koin.core.context.GlobalContext

/**
 * Re-schedules all notification workers after device reboot or app updates.
 */
class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val scheduler = runCatching {
                GlobalContext.get().get<NotificationSchedulerService>()
            }.getOrNull()

            if (scheduler != null) {
                scheduler.initializeNotificationScheduling()
            } else {
                DailyStudyReminderWorker.schedule(context.applicationContext)
            }
        }
    }
}
