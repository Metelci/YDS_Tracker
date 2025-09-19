package com.mtlc.studyplan.workflows

import android.content.Context
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.shared.TaskFilter
import com.mtlc.studyplan.shared.AppTask
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.utils.NotificationScheduler
import com.mtlc.studyplan.utils.ToastManager
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.UUID

/**
 * Complete Goal Setting Journey Implementation
 * Handles the entire workflow from goal creation to task generation and navigation
 */
class GoalSettingWorkflow(
    private val sharedViewModel: SharedAppViewModel,
    private val context: Context
) {

    suspend fun executeGoalSetting(goal: StudyGoal): WorkflowResult {
        return try {
            // Step 1: Validate goal
            val validationResult = validateGoal(goal)
            if (validationResult !is ValidationResult.Valid) {
                return WorkflowResult.ValidationError((validationResult as ValidationResult.Invalid).message)
            }
            showValidationFeedback()

            // Step 2: Save goal
            sharedViewModel.saveStudyGoal(goal)
            delay(500) // Brief processing delay

            // Step 3: Create related tasks
            val tasks = generateTasksForGoal(goal)
            sharedViewModel.addTasks(tasks)

            // Step 4: Setup notifications
            NotificationScheduler.scheduleGoalReminders(context, goal)

            // Step 5: Navigate to tasks view
            sharedViewModel.navigateToTasks(TaskFilter.ByCategory(goal.primaryCategory))

            // Step 6: Show success feedback
            showGoalCreatedFeedback(goal, tasks.size)

            WorkflowResult.Success(goal)
        } catch (e: ValidationException) {
            showValidationError(e.message)
            WorkflowResult.ValidationError(e.message)
        } catch (e: Exception) {
            handleWorkflowError(e, "Goal Setting")
            WorkflowResult.Error(e.message ?: "Goal setting failed")
        }
    }

    private fun validateGoal(goal: StudyGoal): ValidationResult {
        return when {
            goal.title.isBlank() -> ValidationResult.Invalid("Goal title cannot be empty")
            goal.title.length < 3 -> ValidationResult.Invalid("Goal title must be at least 3 characters")
            goal.title.length > 100 -> ValidationResult.Invalid("Goal title cannot exceed 100 characters")
            goal.targetHours <= 0 -> ValidationResult.Invalid("Target hours must be greater than 0")
            goal.targetHours > 1000 -> ValidationResult.Invalid("Target hours cannot exceed 1000")
            goal.deadline.isBefore(LocalDate.now()) -> ValidationResult.Invalid("Deadline cannot be in the past")
            goal.deadline.isAfter(LocalDate.now().plusYears(1)) -> ValidationResult.Invalid("Deadline cannot be more than 1 year from now")
            else -> ValidationResult.Valid
        }
    }

    private fun generateTasksForGoal(goal: StudyGoal): List<AppTask> {
        val tasks = mutableListOf<AppTask>()
        val tasksPerCategory = goal.targetHours / 5 // Roughly 5 hours per task

        when (goal.primaryCategory) {
            TaskCategory.VOCABULARY -> {
                repeat(tasksPerCategory.coerceAtLeast(1)) { index ->
                    tasks.add(createVocabularyTask(goal, index + 1))
                }
            }
            TaskCategory.GRAMMAR -> {
                repeat(tasksPerCategory.coerceAtLeast(1)) { index ->
                    tasks.add(createGrammarTask(goal, index + 1))
                }
            }
            TaskCategory.READING -> {
                repeat(tasksPerCategory.coerceAtLeast(1)) { index ->
                    tasks.add(createReadingTask(goal, index + 1))
                }
            }
            TaskCategory.LISTENING -> {
                repeat(tasksPerCategory.coerceAtLeast(1)) { index ->
                    tasks.add(createListeningTask(goal, index + 1))
                }
            }
            TaskCategory.PRACTICE_EXAM -> {
                tasks.add(createPracticeExamTask(goal))
            }
            TaskCategory.OTHER -> {
                tasks.add(createGeneralTask(goal))
            }
        }

        return tasks
    }

    private fun createVocabularyTask(goal: StudyGoal, index: Int): AppTask {
        val vocabularyTopics = listOf(
            "Academic vocabulary building",
            "Business English terms",
            "Scientific terminology",
            "Everyday conversation words",
            "Synonyms and antonyms"
        )

        return AppTask(
            id = UUID.randomUUID().toString(),
            title = "Vocabulary Study Session $index",
            description = vocabularyTopics.random() + " for ${goal.title}",
            category = TaskCategory.VOCABULARY,
            difficulty = goal.difficulty,
            estimatedMinutes = 60,
            isCompleted = false,
            xpReward = 25
        )
    }

    private fun createGrammarTask(goal: StudyGoal, index: Int): AppTask {
        val grammarTopics = listOf(
            "Tense review and practice",
            "Conditional sentences",
            "Passive voice exercises",
            "Modal verbs usage",
            "Complex sentence structures"
        )

        return AppTask(
            id = UUID.randomUUID().toString(),
            title = "Grammar Practice Session $index",
            description = grammarTopics.random() + " for ${goal.title}",
            category = TaskCategory.GRAMMAR,
            difficulty = goal.difficulty,
            estimatedMinutes = 45,
            isCompleted = false,
            xpReward = 20
        )
    }

    private fun createReadingTask(goal: StudyGoal, index: Int): AppTask {
        val readingTypes = listOf(
            "Academic article analysis",
            "News article comprehension",
            "Literature passage study",
            "Technical document review",
            "Research paper analysis"
        )

        return AppTask(
            id = UUID.randomUUID().toString(),
            title = "Reading Comprehension $index",
            description = readingTypes.random() + " for ${goal.title}",
            category = TaskCategory.READING,
            difficulty = goal.difficulty,
            estimatedMinutes = 75,
            isCompleted = false,
            xpReward = 30
        )
    }

    private fun createListeningTask(goal: StudyGoal, index: Int): AppTask {
        val listeningTypes = listOf(
            "Academic lecture practice",
            "Podcast comprehension",
            "Interview analysis",
            "Presentation skills",
            "Conversation practice"
        )

        return AppTask(
            id = UUID.randomUUID().toString(),
            title = "Listening Practice $index",
            description = listeningTypes.random() + " for ${goal.title}",
            category = TaskCategory.LISTENING,
            difficulty = goal.difficulty,
            estimatedMinutes = 50,
            isCompleted = false,
            xpReward = 25
        )
    }

    private fun createPracticeExamTask(goal: StudyGoal): AppTask {
        return AppTask(
            id = UUID.randomUUID().toString(),
            title = "Practice Exam for ${goal.title}",
            description = "Full-length practice exam covering all sections",
            category = TaskCategory.PRACTICE_EXAM,
            difficulty = goal.difficulty,
            estimatedMinutes = 180,
            isCompleted = false,
            xpReward = 100
        )
    }

    private fun createGeneralTask(goal: StudyGoal): AppTask {
        return AppTask(
            id = UUID.randomUUID().toString(),
            title = "Study Session for ${goal.title}",
            description = "General study session focusing on goal objectives",
            category = TaskCategory.OTHER,
            difficulty = goal.difficulty,
            estimatedMinutes = 60,
            isCompleted = false,
            xpReward = 30
        )
    }

    private fun showValidationFeedback() {
        ToastManager.showSuccess("Goal validated successfully! ✅")
    }

    private fun showValidationError(message: String) {
        ToastManager.showError("Validation Error: $message")
    }

    private fun showGoalCreatedFeedback(goal: StudyGoal, taskCount: Int) {
        val message = buildString {
            append("🎯 Goal Created Successfully!\n")
            append("Goal: ${goal.title}\n")
            append("Generated $taskCount tasks\n")
            append("Target: ${goal.targetHours} hours by ${goal.deadline}")
        }

        ToastManager.showSuccess(message)
    }

    private fun handleWorkflowError(error: Exception, operation: String) {
        val errorMessage = "Failed to complete $operation: ${error.message ?: "Unknown error"}"
        ToastManager.showError(errorMessage)
        android.util.Log.e("GoalSettingWorkflow", errorMessage, error)
    }
}

// Data classes and enums
data class StudyGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val targetHours: Int,
    val deadline: LocalDate,
    val primaryCategory: TaskCategory,
    val difficulty: TaskDifficulty,
    val createdDate: LocalDate = LocalDate.now(),
    val isCompleted: Boolean = false
)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

class ValidationException(message: String) : Exception(message)