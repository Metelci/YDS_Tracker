package com.mtlc.studyplan.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mtlc.studyplan.MainActivity
import com.mtlc.studyplan.R

/**
 * Comprehensive Notification System
 * Handles all types of app notifications with proper channel management
 */
object NotificationHelper {

    private const val CHANNEL_ACHIEVEMENTS = "achievements"
    private const val CHANNEL_REMINDERS = "reminders"
    private const val CHANNEL_SOCIAL = "social"
    private const val CHANNEL_PROGRESS = "progress"

    private var notificationIdCounter = 1000

    fun initialize(context: Context) {
        createNotificationChannels(context)
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Achievements Channel
            val achievementsChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievements & Rewards",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for completed achievements and earned rewards"
                enableVibration(true)
                enableLights(true)
            }

            // Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for study sessions and goals"
                enableVibration(true)
            }

            // Social Channel
            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL,
                "Social Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friend requests, group updates, and social interactions"
            }

            // Progress Channel
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Progress Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Study progress and streak updates"
            }

            notificationManager.createNotificationChannels(listOf(
                achievementsChannel,
                remindersChannel,
                socialChannel,
                progressChannel
            ))
        }
    }

    fun showAchievementNotification(
        context: Context,
        title: String,
        message: String,
        icon: Int = R.drawable.ic_star
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "progress")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                getNextNotificationId(),
                notification
            )
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            ToastManager.showSuccess(title)
        }
    }

    fun showReminderNotification(
        context: Context,
        title: String,
        message: String,
        actionText: String = "Study Now"
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "tasks")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_task,
                actionText,
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                getNextNotificationId(),
                notification
            )
        } catch (e: SecurityException) {
            ToastManager.showInfo(title)
        }
    }

    fun showSocialNotification(
        context: Context,
        title: String,
        message: String,
        profileIcon: Int = R.drawable.ic_people
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "social")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(profileIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                getNextNotificationId(),
                notification
            )
        } catch (e: SecurityException) {
            ToastManager.showInfo(title)
        }
    }

    fun showProgressNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int = -1,
        maxProgress: Int = 100
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "progress")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_trending_up)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (progress >= 0) {
            builder.setProgress(maxProgress, progress, false)
        }

        try {
            NotificationManagerCompat.from(context).notify(
                getNextNotificationId(),
                builder.build()
            )
        } catch (e: SecurityException) {
            // Silently fail for progress notifications
        }
    }

    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun getNextNotificationId(): Int {
        return notificationIdCounter++
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}