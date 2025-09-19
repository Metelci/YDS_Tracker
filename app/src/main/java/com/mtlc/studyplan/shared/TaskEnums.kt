package com.mtlc.studyplan.shared

enum class TaskCategory(val displayName: String) {
    MATHEMATICS("Mathematics"),
    SCIENCE("Science"),
    LANGUAGE("Language"),
    HISTORY("History"),
    ENGLISH_LITERATURE("English Literature"),
    COMPUTER_SCIENCE("Computer Science"),
    GENERAL("General"),
    OTHER("Other")
}

enum class TaskDifficulty(val displayName: String, val points: Int) {
    EASY("Easy", 5),
    MEDIUM("Medium", 10),
    HARD("Hard", 15),
    EXPERT("Expert", 20)
}

enum class TaskPriority(val displayName: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    MEDIUM("Medium", 0xFFFF9800),
    HIGH("High", 0xFFE53E3E),
    CRITICAL("Critical", 0xFFD32F2F)
}

enum class AchievementCategory(val displayName: String) {
    TASKS("Tasks"),
    STREAKS("Streaks"),
    MILESTONES("Milestones"),
    STUDY_TIME("Study Time"),
    PERFORMANCE("Performance"),
    SOCIAL("Social"),
    GENERAL("General")
}