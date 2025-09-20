package com.mtlc.studyplan.feedback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.mtlc.studyplan.R
import com.mtlc.studyplan.shared.AppTask
import com.mtlc.studyplan.utils.NotificationHelper
import com.mtlc.studyplan.utils.ToastManager
import com.mtlc.studyplan.workflows.StudyGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Success Feedback Management System
 * Provides celebratory animations and positive feedback for user achievements
 */
class SuccessFeedbackManager(private val context: Context) {

    private var currentOverlay: View? = null

    fun showTaskCompletion(
        task: AppTask,
        earnedPoints: Int,
        streakExtended: Boolean,
        newAchievements: List<String> = emptyList()
    ) {
        val animation = when {
            newAchievements.isNotEmpty() -> AnimationType.ACHIEVEMENT_UNLOCKED
            streakExtended -> AnimationType.STREAK_CELEBRATION
            earnedPoints > 50 -> AnimationType.BIG_SUCCESS
            else -> AnimationType.SIMPLE_SUCCESS
        }

        showSuccessAnimation(animation) {
            showTaskCompletionMessage(task, earnedPoints, streakExtended, newAchievements)
        }
    }

    fun showGoalAchievement(goal: StudyGoal) {
        showSuccessAnimation(AnimationType.GOAL_ACHIEVEMENT) {
            showGoalAchievementMessage(goal)
        }
    }

    fun showStreakMilestone(streak: Int) {
        val animation = when {
            streak >= 100 -> AnimationType.LEGENDARY_STREAK
            streak >= 30 -> AnimationType.MAJOR_STREAK
            streak >= 7 -> AnimationType.STREAK_CELEBRATION
            else -> AnimationType.SIMPLE_SUCCESS
        }

        showSuccessAnimation(animation) {
            showStreakMilestoneMessage(streak)
        }
    }

    fun showAchievementUnlocked(achievementTitle: String, achievementDescription: String) {
        showSuccessAnimation(AnimationType.ACHIEVEMENT_UNLOCKED) {
            showAchievementUnlockedMessage(achievementTitle, achievementDescription)
        }
    }

    fun showLevelUp(newLevel: Int, unlockedFeatures: List<String> = emptyList()) {
        showSuccessAnimation(AnimationType.LEVEL_UP) {
            showLevelUpMessage(newLevel, unlockedFeatures)
        }
    }

    fun showSocialShareSuccess(platform: String, likes: Int) {
        showSuccessAnimation(AnimationType.SOCIAL_SUCCESS) {
            showSocialShareMessage(platform, likes)
        }
    }

    fun showGoalProgress(goalTitle: String, progressPercentage: Int) {
        if (progressPercentage >= 100) {
            showGoalAchievement(StudyGoal(title = goalTitle, description = "", targetHours = 0, deadline = java.time.LocalDate.now(), primaryCategory = com.mtlc.studyplan.shared.TaskCategory.OTHER, difficulty = com.mtlc.studyplan.shared.TaskDifficulty.MEDIUM))
        } else {
            val animation = when {
                progressPercentage >= 75 -> AnimationType.MAJOR_PROGRESS
                progressPercentage >= 50 -> AnimationType.GOOD_PROGRESS
                progressPercentage >= 25 -> AnimationType.SIMPLE_SUCCESS
                else -> AnimationType.SIMPLE_SUCCESS
            }

            showSuccessAnimation(animation) {
                showGoalProgressMessage(goalTitle, progressPercentage)
            }
        }
    }

    private fun showSuccessAnimation(type: AnimationType, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            // Remove any existing overlay
            removeCurrentOverlay()

            // Create animation overlay
            val overlay = createAnimationOverlay(type)
            showAnimationOverlay(overlay)

            // Wait for animation duration
            delay(type.duration)

            // Remove overlay and show completion message
            removeCurrentOverlay()
            onComplete()
        }
    }

    private fun createAnimationOverlay(type: AnimationType): View {
        val overlay = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
        }

        val animationView = when (type) {
            AnimationType.SIMPLE_SUCCESS -> createCheckmarkAnimation()
            AnimationType.BIG_SUCCESS -> createStarBurstAnimation()
            AnimationType.STREAK_CELEBRATION -> createFireAnimation()
            AnimationType.ACHIEVEMENT_UNLOCKED -> createTrophyAnimation()
            AnimationType.GOAL_ACHIEVEMENT -> createGoalCompletionAnimation()
            AnimationType.LEVEL_UP -> createLevelUpAnimation()
            AnimationType.SOCIAL_SUCCESS -> createSocialAnimation()
            AnimationType.MAJOR_STREAK -> createMajorStreakAnimation()
            AnimationType.LEGENDARY_STREAK -> createLegendaryStreakAnimation()
            AnimationType.MAJOR_PROGRESS -> createProgressAnimation()
            AnimationType.GOOD_PROGRESS -> createProgressAnimation()
        }

        overlay.addView(animationView)
        return overlay
    }

    private fun createCheckmarkAnimation(): View {
        val container = FrameLayout(context)
        val imageView = ImageView(context).apply {
            setImageResource(R.drawable.ic_check_circle)
            layoutParams = FrameLayout.LayoutParams(120, 120).apply {
                gravity = Gravity.CENTER
            }
            setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light))
        }

        container.addView(imageView)

        // Scale animation
        val scaleX = ObjectAnimator.ofFloat(imageView, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(imageView, "scaleY", 0f, 1.2f, 1f)
        scaleX.duration = 600
        scaleY.duration = 600
        scaleX.interpolator = BounceInterpolator()
        scaleY.interpolator = BounceInterpolator()

        scaleX.start()
        scaleY.start()

        return container
    }

    private fun createStarBurstAnimation(): View {
        val container = FrameLayout(context)

        // Create multiple stars for burst effect
        repeat(8) { index ->
            val star = ImageView(context).apply {
                setImageResource(R.drawable.ic_star)
                layoutParams = FrameLayout.LayoutParams(60, 60).apply {
                    gravity = Gravity.CENTER
                }
                setColorFilter(Color.YELLOW)
            }

            container.addView(star)

            // Animate each star outward
            val angle = (index * 45).toFloat()
            val distance = 200f

            val translateX = ObjectAnimator.ofFloat(star, "translationX", 0f,
                distance * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat())
            val translateY = ObjectAnimator.ofFloat(star, "translationY", 0f,
                distance * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat())
            val alpha = ObjectAnimator.ofFloat(star, "alpha", 1f, 0f)

            translateX.duration = 1000
            translateY.duration = 1000
            alpha.duration = 1000
            alpha.startDelay = 500

            translateX.start()
            translateY.start()
            alpha.start()
        }

        return container
    }

    private fun createFireAnimation(): View {
        val container = FrameLayout(context)
        val textView = TextView(context).apply {
            text = "🔥"
            textSize = 72f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(textView)

        // Pulsing animation
        val scaleAnimator = ValueAnimator.ofFloat(1f, 1.5f, 1f, 1.3f, 1f)
        scaleAnimator.duration = 1500
        scaleAnimator.addUpdateListener { animator ->
            val scale = animator.animatedValue as Float
            textView.scaleX = scale
            textView.scaleY = scale
        }
        scaleAnimator.start()

        return container
    }

    private fun createTrophyAnimation(): View {
        val container = FrameLayout(context)
        val imageView = ImageView(context).apply {
            setImageResource(R.drawable.ic_star)
            layoutParams = FrameLayout.LayoutParams(150, 150).apply {
                gravity = Gravity.CENTER
            }
            setColorFilter(Color.parseColor("#FFD700")) // Gold
        }

        container.addView(imageView)

        // Rotation with scale
        val rotate = ObjectAnimator.ofFloat(imageView, "rotation", -15f, 15f, -10f, 10f, 0f)
        val scale = ObjectAnimator.ofFloat(imageView, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(imageView, "scaleY", 0f, 1.2f, 1f)

        rotate.duration = 1200
        scale.duration = 800
        scaleY.duration = 800

        rotate.interpolator = AccelerateDecelerateInterpolator()
        scale.interpolator = OvershootInterpolator()
        scaleY.interpolator = OvershootInterpolator()

        rotate.start()
        scale.start()
        scaleY.start()

        return container
    }

    private fun createGoalCompletionAnimation(): View {
        val container = FrameLayout(context)
        val textView = TextView(context).apply {
            text = "🎯✨"
            textSize = 64f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(textView)

        // Bounce animation
        val bounceY = ObjectAnimator.ofFloat(textView, "translationY", 0f, -100f, 0f, -50f, 0f)
        bounceY.duration = 1000
        bounceY.interpolator = BounceInterpolator()
        bounceY.start()

        return container
    }

    private fun createLevelUpAnimation(): View = createStarBurstAnimation() // Similar to star burst

    private fun createSocialAnimation(): View {
        val container = FrameLayout(context)
        val textView = TextView(context).apply {
            text = "📱✨"
            textSize = 56f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(textView)

        val pulse = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0.5f, 1f)
        pulse.duration = 800
        pulse.repeatCount = 2
        pulse.start()

        return container
    }

    private fun createMajorStreakAnimation(): View = createFireAnimation()

    private fun createLegendaryStreakAnimation(): View {
        val container = FrameLayout(context)
        val textView = TextView(context).apply {
            text = "👑🔥"
            textSize = 80f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(textView)

        // Epic animation
        val rotation = ObjectAnimator.ofFloat(textView, "rotation", 0f, 360f)
        val scale = ObjectAnimator.ofFloat(textView, "scaleX", 0f, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 0f, 1.5f, 1f)

        rotation.duration = 1500
        scale.duration = 1200
        scaleY.duration = 1200

        rotation.start()
        scale.start()
        scaleY.start()

        return container
    }

    private fun createProgressAnimation(): View = createCheckmarkAnimation()

    private fun showAnimationOverlay(overlay: View) {
        // In a real implementation, this would add to window or root view
        currentOverlay = overlay
    }

    private fun removeCurrentOverlay() {
        currentOverlay = null
    }

    // Message showing methods
    private fun showTaskCompletionMessage(
        task: AppTask,
        earnedPoints: Int,
        streakExtended: Boolean,
        newAchievements: List<String>
    ) {
        val message = buildString {
            append("✅ Task Completed!\n")
            append("${task.title}\n\n")
            append("Points earned: +$earnedPoints XP")
            if (streakExtended) append("\n🔥 Streak extended!")
            if (newAchievements.isNotEmpty()) {
                append("\n\n🎉 New Achievements:")
                newAchievements.forEach { append("\n• $it") }
            }
        }

        showSuccessDialog("Great Job!", message)
        ToastManager.showSuccess("Task completed! +$earnedPoints XP")
    }

    private fun showGoalAchievementMessage(goal: StudyGoal) {
        val message = "🎯 Congratulations!\n\nYou've successfully completed your goal:\n${goal.title}\n\nYour dedication and hard work have paid off!"
        showSuccessDialog("Goal Achieved!", message)
    }

    private fun showStreakMilestoneMessage(streak: Int) {
        val message = when {
            streak >= 100 -> "👑 LEGENDARY!\n\n$streak days straight!\nYou're absolutely unstoppable!"
            streak >= 30 -> "🌟 INCREDIBLE!\n\n$streak days in a row!\nYou're building amazing habits!"
            streak >= 7 -> "🔥 AMAZING!\n\n$streak days streak!\nYou're on fire!"
            else -> "🎉 Great work!\n\n$streak days streak!"
        }

        showSuccessDialog("Streak Milestone!", message)
    }

    private fun showAchievementUnlockedMessage(title: String, description: String) {
        val message = "🏆 Achievement Unlocked!\n\n$title\n\n$description"
        showSuccessDialog("New Achievement!", message)
    }

    private fun showLevelUpMessage(level: Int, unlockedFeatures: List<String>) {
        val message = buildString {
            append("🚀 Level Up!\n\nWelcome to Level $level!")
            if (unlockedFeatures.isNotEmpty()) {
                append("\n\nNew features unlocked:")
                unlockedFeatures.forEach { append("\n• $it") }
            }
        }
        showSuccessDialog("Level Up!", message)
    }

    private fun showSocialShareMessage(platform: String, likes: Int) {
        val message = "📱 Shared successfully!\n\nYour achievement has been shared on $platform.\nAlready received $likes reactions!"
        showSuccessDialog("Shared!", message)
    }

    private fun showGoalProgressMessage(goalTitle: String, progressPercentage: Int) {
        val message = "📈 Great Progress!\n\n$progressPercentage% complete on:\n$goalTitle\n\nKeep up the excellent work!"
        showSuccessDialog("Progress Update!", message)
    }

    private fun showSuccessDialog(title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

enum class AnimationType(val duration: Long) {
    SIMPLE_SUCCESS(800),
    BIG_SUCCESS(1200),
    STREAK_CELEBRATION(1500),
    ACHIEVEMENT_UNLOCKED(1500),
    GOAL_ACHIEVEMENT(1800),
    LEVEL_UP(1200),
    SOCIAL_SUCCESS(1000),
    MAJOR_STREAK(1800),
    LEGENDARY_STREAK(2000),
    MAJOR_PROGRESS(1000),
    GOOD_PROGRESS(800)
}

