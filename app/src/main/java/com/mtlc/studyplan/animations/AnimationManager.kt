package com.mtlc.studyplan.animations

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.gamification.GamificationTaskResult
import com.mtlc.studyplan.R
import com.mtlc.studyplan.gamification.AdvancedAchievement
import kotlin.text.StringBuilder
import kotlin.math.cos
import kotlin.math.sin

class AnimationManager(private val context: Context) {

    companion object {
        const val DURATION_SHORT = 150L
        const val DURATION_MEDIUM = 300L
        const val DURATION_LONG = 500L
        const val DURATION_EXTRA_LONG = 750L
    }

    // Task completion animations
    fun animateTaskCompletion(
        taskView: View,
        result: GamificationTaskResult? = null,
        onAnimationEnd: () -> Unit
    ) {
        val checkmark = createCheckmarkView(taskView)

        checkmark.alpha = 0f
        checkmark.scaleX = 0.5f
        checkmark.scaleY = 0.5f

        checkmark.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                taskView.animate()
                    .alpha(0f)
                    .translationX(taskView.width.toFloat())
                    .setDuration(DURATION_MEDIUM)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        onAnimationEnd()
                        removeCheckmarkView(checkmark)
                        result?.let { showGamificationResultToast(it) }
                    }
                    .start()
            }
            .start()

        result?.newAchievements?.forEach { achievement ->
            animateAchievementUnlock(taskView, achievement) {}
        }
    }

    private fun showGamificationResultToast(result: GamificationTaskResult) {
        val messageBuilder = StringBuilder()
        messageBuilder.append(
            context.getString(
                R.string.gamification_points_earned_message,
                result.pointsEarned
            )
        )

        result.levelUp?.let { level ->
            messageBuilder.append('\n')
            messageBuilder.append(
                context.getString(
                    R.string.gamification_level_up_message,
                    level.currentLevel,
                    level.levelTitle
                )
            )
        }

        if (result.newAchievements.isNotEmpty()) {
            val titles = result.newAchievements.joinToString { it.title }
            messageBuilder.append('\n')
            messageBuilder.append(
                context.getString(
                    R.string.gamification_new_achievement_message,
                    titles
                )
            )
        }

        Toast.makeText(context, messageBuilder.toString(), Toast.LENGTH_LONG).show()
    }

    // Progress update animations
    fun animateProgressUpdate(
        progressBar: ProgressBar,
        fromProgress: Int,
        toProgress: Int,
        duration: Long = DURATION_LONG
    ) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", fromProgress, toProgress)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()

        // Add bounce effect for significant progress
        if (toProgress - fromProgress > 20) {
            animator.interpolator = BounceInterpolator()
        }

        animator.start()
    }

    // Streak counter animation
    fun animateStreakUpdate(
        streakView: TextView,
        newStreak: Int,
        isIncreasing: Boolean
    ) {
        if (isIncreasing) {
            // Celebration animation for streak increase
            animateStreakCelebration(streakView, newStreak)
        } else {
            // Simple update for streak reset
            animateStreakReset(streakView, newStreak)
        }
    }

    private fun animateStreakCelebration(streakView: TextView, newStreak: Int) {
        // Create flame particles
        val flameContainer = createFlameParticles(streakView)

        // Scale and bounce animation
        streakView.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(DURATION_SHORT)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                streakView.text = newStreak.toString()
                streakView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(DURATION_SHORT)
                    .start()
            }
            .start()

        // Animate flame particles
        animateFlameParticles(flameContainer)
    }

    private fun animateStreakReset(streakView: TextView, newStreak: Int) {
        // Simple fade and update for streak reset
        streakView.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .withEndAction {
                streakView.text = newStreak.toString()
                streakView.animate()
                    .alpha(1f)
                    .setDuration(DURATION_SHORT)
                    .start()
            }
            .start()
    }

    // Data loading animations
    fun animateDataRefresh(
        recyclerView: RecyclerView,
        onRefreshStart: () -> Unit,
        onRefreshEnd: () -> Unit
    ) {
        // Fade out old content
        recyclerView.animate()
            .alpha(0.5f)
            .setDuration(DURATION_SHORT)
            .withEndAction {
                onRefreshStart()

                // Fade in new content with stagger
                animateItemsStagger(recyclerView) {
                    onRefreshEnd()
                }
            }
            .start()
    }

    private fun animateItemsStagger(
        recyclerView: RecyclerView,
        onComplete: () -> Unit
    ) {
        recyclerView.alpha = 1f

        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            onComplete()
            return
        }

        var animationsCompleted = 0
        val totalAnimations = lastVisible - firstVisible + 1

        for (i in firstVisible..lastVisible) {
            val child = layoutManager.findViewByPosition(i) ?: continue

            child.alpha = 0f
            child.translationY = 50f

            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(DURATION_MEDIUM)
                .setStartDelay((i - firstVisible) * 50L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    animationsCompleted++
                    if (animationsCompleted == totalAnimations) {
                        onComplete()
                    }
                }
                .start()
        }
    }

    // Navigation animations
    fun animateTabSwitch(
        fromTab: View,
        toTab: View,
        direction: TabSwitchDirection
    ) {
        val slideDistance = fromTab.width.toFloat()
        val slideIn = when (direction) {
            TabSwitchDirection.LEFT_TO_RIGHT -> slideDistance
            TabSwitchDirection.RIGHT_TO_LEFT -> -slideDistance
        }

        // Slide out current tab
        fromTab.animate()
            .translationX(-slideIn)
            .alpha(0f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(AccelerateInterpolator())
            .start()

        // Slide in new tab
        toTab.translationX = slideIn
        toTab.alpha = 0f
        toTab.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // Achievement unlock animation
    fun animateAchievementUnlock(
        achievementView: View,
        achievement: AdvancedAchievement,
        onAnimationComplete: () -> Unit
    ) {
        // Create achievement popup
        val popup = createAchievementPopup(achievementView, achievement)

        // Animate popup entrance
        popup.alpha = 0f
        popup.scaleX = 0.8f
        popup.scaleY = 0.8f
        popup.translationY = -100f

        popup.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(DURATION_LONG)
            .setInterpolator(BounceInterpolator())
            .withEndAction {
                // Keep visible for a moment then fade out
                popup.postDelayed({
                    popup.animate()
                        .alpha(0f)
                        .translationY(-50f)
                        .setDuration(DURATION_MEDIUM)
                        .withEndAction {
                            removeAchievementPopup(popup)
                            onAnimationComplete()
                        }
                        .start()
                }, 2000)
            }
            .start()
    }

    // Loading state animations
    fun animateLoadingState(
        containerView: ViewGroup,
        isLoading: Boolean
    ) {
        val loadingView = containerView.findViewById<View>(R.id.loading_view)
        val contentView = containerView.findViewById<View>(R.id.content_view)

        if (isLoading) {
            // Fade to loading state
            contentView?.animate()
                ?.alpha(0f)
                ?.setDuration(DURATION_SHORT)
                ?.start()

            loadingView?.animate()
                ?.alpha(1f)
                ?.setDuration(DURATION_SHORT)
                ?.start()
        } else {
            // Fade to content
            loadingView?.animate()
                ?.alpha(0f)
                ?.setDuration(DURATION_SHORT)
                ?.start()

            contentView?.animate()
                ?.alpha(1f)
                ?.setDuration(DURATION_SHORT)
                ?.start()
        }
    }

    // Badge animations
    fun animateBadgeAppearance(badgeView: View) {
        badgeView.scaleX = 0f
        badgeView.scaleY = 0f
        badgeView.alpha = 0f

        badgeView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    fun animateBadgeUpdate(badgeView: View, newText: String) {
        badgeView.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(DURATION_SHORT)
            .withEndAction {
                if (badgeView is TextView) {
                    badgeView.text = newText
                }
                badgeView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(DURATION_SHORT)
                    .start()
            }
            .start()
    }

    // Card animations
    fun animateCardReveal(cardView: View, delay: Long = 0) {
        cardView.alpha = 0f
        cardView.translationY = 100f
        cardView.scaleX = 0.8f
        cardView.scaleY = 0.8f

        cardView.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delay)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun animateCardPress(cardView: View, onComplete: (() -> Unit)? = null) {
        cardView.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(DURATION_SHORT / 2)
            .withEndAction {
                cardView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(DURATION_SHORT / 2)
                    .withEndAction { onComplete?.invoke() }
                    .start()
            }
            .start()
    }

    // Helper methods for creating animation elements
    private fun createCheckmarkView(parent: View): ImageView {
        val checkmark = ImageView(context)
        checkmark.setImageResource(R.drawable.ic_check_circle)
        checkmark.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light))

        val size = 80
        val layoutParams = FrameLayout.LayoutParams(size, size)
        layoutParams.gravity = Gravity.CENTER
        checkmark.layoutParams = layoutParams

        if (parent.parent is ViewGroup) {
            (parent.parent as ViewGroup).addView(checkmark)
        }

        return checkmark
    }

    private fun removeCheckmarkView(checkmark: View) {
        if (checkmark.parent is ViewGroup) {
            (checkmark.parent as ViewGroup).removeView(checkmark)
        }
    }

    private fun createFlameParticles(parent: View): ViewGroup {
        val container = FrameLayout(context)

        // Add flame emoji particles
        repeat(5) { index ->
            val flame = TextView(context).apply {
                text = "🔥"
                textSize = 20f
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }
            container.addView(flame)
        }

        if (parent.parent is ViewGroup) {
            (parent.parent as ViewGroup).addView(container)
        }

        return container
    }

    private fun animateFlameParticles(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            val flame = container.getChildAt(i)
            val angle = (i * 72).toFloat() // 360/5 for 5 flames
            val distance = 100f

            flame.animate()
                .translationX(distance * cos(Math.toRadians(angle.toDouble())).toFloat())
                .translationY(distance * sin(Math.toRadians(angle.toDouble())).toFloat())
                .alpha(0f)
                .setDuration(DURATION_LONG)
                .setStartDelay(i * 50L)
                .withEndAction {
                    if (container.parent is ViewGroup) {
                        (container.parent as ViewGroup).removeView(container)
                    }
                }
                .start()
        }
    }

    private fun createAchievementPopup(parent: View, achievement: AdvancedAchievement): View {
        val popup = FrameLayout(context)

        // Create achievement display
        val achievementView = TextView(context).apply {
            text = context.getString(R.string.gamification_achievement_banner, achievement.title)
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF4CAF50"))
            setPadding(32, 16, 32, 16)
            gravity = Gravity.CENTER
        }

        popup.addView(achievementView)

        if (parent.parent is ViewGroup) {
            (parent.parent as ViewGroup).addView(popup)
        }

        return popup
    }

    private fun removeAchievementPopup(popup: View) {
        if (popup.parent is ViewGroup) {
            (popup.parent as ViewGroup).removeView(popup)
        }
    }

    // Number counter animation
    fun animateNumberChange(
        textView: TextView,
        fromValue: Int,
        toValue: Int,
        duration: Long = DURATION_MEDIUM,
        prefix: String = "",
        suffix: String = ""
    ) {
        val animator = ValueAnimator.ofInt(fromValue, toValue)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            textView.text = "$prefix$animatedValue$suffix"
        }

        animator.start()
    }

    enum class TabSwitchDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT
    }
}




