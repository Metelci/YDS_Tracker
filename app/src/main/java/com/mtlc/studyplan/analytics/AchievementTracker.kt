package com.mtlc.studyplan.analytics

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress

class AchievementTracker {

    fun detect(logs: List<TaskLog>, userProgress: UserProgress?): List<String> {
        if (logs.isEmpty()) return emptyList()
        val achievements = mutableListOf<String>()

        val currentStreak = userProgress?.streakCount ?: 0
        when {
            currentStreak >= 30 -> achievements.add("30-Day Study Streak Master!")
            currentStreak >= 14 -> achievements.add("Two-Week Consistency Champion!")
            currentStreak >= 7 -> achievements.add("Week-Long Study Warrior!")
        }

        val recentAccuracy = logs.takeLast(20).map { if (it.correct) 1f else 0f }.average()
        if (recentAccuracy >= 0.95) {
            achievements.add("Perfectionist - 95%+ Accuracy!")
        }

        val totalMinutes = logs.sumOf { it.minutesSpent }
        when {
            totalMinutes >= 1_000 -> achievements.add("Study Marathon - 1000+ Minutes!")
            totalMinutes >= 500 -> achievements.add("Dedicated Learner - 500+ Minutes!")
        }

        return achievements
    }
}
