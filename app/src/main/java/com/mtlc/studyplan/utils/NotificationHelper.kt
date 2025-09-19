package com.mtlc.studyplan.utils

import android.content.Context

/**
 * Temporary notification helper stub to keep the project building while the notification stack is rebuilt.
 */
object NotificationHelper {

    fun initialize(context: Context) {}

    fun showAchievementNotification(
        context: Context,
        title: String,
        message: String,
        icon: Int = 0
    ) {}

    fun showReminderNotification(
        context: Context,
        title: String,
        message: String,
        actionText: String = "Study Now"
    ) {}

    fun showSocialNotification(
        context: Context,
        title: String,
        message: String,
        profileIcon: Int = 0
    ) {}

    fun showProgressNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int = -1,
        maxProgress: Int = 100
    ) {}

    fun cancelAllNotifications(context: Context) {}

    fun cancelNotification(context: Context, notificationId: Int) {}

    fun areNotificationsEnabled(context: Context): Boolean = true
}
