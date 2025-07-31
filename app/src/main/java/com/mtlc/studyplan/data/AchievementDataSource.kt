package com.mtlc.studyplan.data

object AchievementDataSource {
    // Hazırlık dönemi artık ilk 26 hafta
    private val prepPhaseTasks = PlanDataSource.planData.take(26).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()

    val allAchievements = listOf(
        Achievement("first_task", "İlk Adım", "İlk görevini tamamladın!") { userProgress -> userProgress.completedTasks.isNotEmpty() },
        Achievement("hundred_tasks", "Yola Çıktın", "100 görevi tamamladın!") { userProgress -> userProgress.completedTasks.size >= 100 },
        Achievement("prep_complete", "Hazırlık Dönemi Bitti!", "6 aylık hazırlık dönemini tamamladın. Şimdi sıra denemelerde!") { userProgress ->
            userProgress.completedTasks.containsAll(prepPhaseTasks)
        },
        Achievement("first_exam_week", "Sınav Kampı Başladı!", "Son ay deneme kampına başladın!") { userProgress ->
            userProgress.completedTasks.any { taskId -> taskId.startsWith("w27") } // Kamp 27. haftada başlıyor
        },
        Achievement("ten_exams", "10 Deneme Bitti!", "Toplam 10 tam deneme sınavı çözdün!") { userProgress ->
            userProgress.completedTasks.count { it.contains("-exam-") } >= 10
        }
    )
}