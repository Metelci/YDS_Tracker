package com.mtlc.studyplan.ui.animations

import android.animation.*
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.mtlc.studyplan.R
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Advanced animation system for settings interactions
 */
object SettingsAnimations {

    private const val ANIMATION_DURATION_SHORT = 200L
    private const val ANIMATION_DURATION_MEDIUM = 300L
    private const val ANIMATION_DURATION_LONG = 500L

    /**
     * Animate setting item appearance with staggered effect
     */
    fun animateSettingItemsAppearance(
        items: List<View>,
        staggerDelay: Long = 50L,
        animationDuration: Long = ANIMATION_DURATION_MEDIUM
    ) {
        items.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animationDuration)
                .setStartDelay(index * staggerDelay)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }

    /**
     * Animate toggle switch state change with custom transition
     */
    suspend fun animateToggleSwitch(
        switch: MaterialSwitch,
        newState: Boolean,
        showValueChange: Boolean = true
    ): Unit = suspendCancellableCoroutine { continuation ->

        val currentState = switch.isChecked
        if (currentState == newState) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        // Create scale animation for visual feedback
        val scaleAnimation = ObjectAnimator.ofFloat(switch, "scaleX", 1f, 1.1f, 1f).apply {
            duration = ANIMATION_DURATION_SHORT
            interpolator = OvershootInterpolator(1.2f)
        }

        val scaleYAnimation = ObjectAnimator.ofFloat(switch, "scaleY", 1f, 1.1f, 1f).apply {
            duration = ANIMATION_DURATION_SHORT
            interpolator = OvershootInterpolator(1.2f)
        }

        // Color transition animation
        val colorAnimator = createToggleColorAnimation(switch, newState)

        // Combine animations
        val animatorSet = AnimatorSet().apply {
            playTogether(scaleAnimation, scaleYAnimation, colorAnimator)
            addListener(onEnd = {
                switch.isChecked = newState
                continuation.resume(Unit)
            })
        }

        continuation.invokeOnCancellation {
            animatorSet.cancel()
        }

        // Show visual feedback for value change
        if (showValueChange) {
            showValueChangeRipple(switch, newState)
        }

        animatorSet.start()
    }

    /**
     * Create color transition animation for toggle switch
     */
    private fun createToggleColorAnimation(switch: MaterialSwitch, enabled: Boolean): ValueAnimator {
        val context = switch.context
        val fromColor = if (enabled)
            context.getColor(android.R.color.darker_gray)
        else
            context.getColor(android.R.color.holo_blue_light)

        val toColor = if (enabled)
            context.getColor(android.R.color.holo_blue_light)
        else
            context.getColor(android.R.color.darker_gray)

        return ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = ANIMATION_DURATION_SHORT
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                switch.thumbTintList = android.content.res.ColorStateList.valueOf(color)
            }
        }
    }

    /**
     * Show ripple effect for value changes
     */
    private fun showValueChangeRipple(view: View, positive: Boolean) {
        val context = view.context
        val rippleColor = if (positive)
            context.getColor(android.R.color.holo_green_light)
        else
            context.getColor(android.R.color.holo_orange_light)

        // Create temporary ripple overlay
        val rippleView = View(context).apply {
            setBackgroundColor(rippleColor)
            alpha = 0f
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        (view.parent as? ViewGroup)?.addView(rippleView)

        // Animate ripple
        rippleView.animate()
            .alpha(0.3f)
            .setDuration(100L)
            .withEndAction {
                rippleView.animate()
                    .alpha(0f)
                    .setDuration(200L)
                    .withEndAction {
                        (view.parent as? ViewGroup)?.removeView(rippleView)
                    }
            }
    }

    /**
     * Animate setting list item interaction
     */
    fun animateSettingItemClick(view: View) {
        val originalElevation = view.elevation

        // Create press animation
        val pressAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.97f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.97f),
                ObjectAnimator.ofFloat(view, "elevation", originalElevation, originalElevation + 4f)
            )
            duration = 100L
            interpolator = AccelerateInterpolator()
        }

        // Create release animation
        val releaseAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.97f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.97f, 1f),
                ObjectAnimator.ofFloat(view, "elevation", originalElevation + 4f, originalElevation)
            )
            duration = 150L
            interpolator = OvershootInterpolator(1.1f)
        }

        // Chain animations
        pressAnimator.addListener(onEnd = {
            releaseAnimator.start()
        })

        pressAnimator.start()
    }

    /**
     * Animate progress indicator with pulsing effect
     */
    fun animateProgressIndicator(progressView: View, progress: Float) {
        // Scale animation for visual emphasis
        val scaleAnimator = ObjectAnimator.ofFloat(progressView, "scaleX", 1f, 1.05f, 1f).apply {
            duration = ANIMATION_DURATION_SHORT
            interpolator = FastOutSlowInInterpolator()
        }

        // Progress value animation (if supported)
        if (progressView is android.widget.ProgressBar) {
            val currentProgress = progressView.progress
            val targetProgress = (progress * 100).toInt()

            val progressAnimator = ValueAnimator.ofInt(currentProgress, targetProgress).apply {
                duration = ANIMATION_DURATION_MEDIUM
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    progressView.progress = animator.animatedValue as Int
                }
            }

            AnimatorSet().apply {
                playTogether(scaleAnimator, progressAnimator)
                start()
            }
        } else {
            scaleAnimator.start()
        }
    }

    /**
     * Animate loading state with skeleton effect
     */
    fun animateLoadingState(view: View, isLoading: Boolean) {
        if (isLoading) {
            startSkeletonAnimation(view)
        } else {
            stopSkeletonAnimation(view)
        }
    }

    private fun startSkeletonAnimation(view: View) {
        val shimmerAnimator = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        view.setTag(R.id.shimmer_animator, shimmerAnimator)
        shimmerAnimator.start()
    }

    private fun stopSkeletonAnimation(view: View) {
        val shimmerAnimator = view.getTag(R.id.shimmer_animator) as? ObjectAnimator
        shimmerAnimator?.cancel()
        view.alpha = 1f
    }

    /**
     * Animate error state with shake effect
     */
    fun animateErrorState(view: View) {
        val shakeAnimation = ObjectAnimator.ofFloat(
            view, "translationX",
            0f, -10f, 10f, -5f, 5f, 0f
        ).apply {
            duration = ANIMATION_DURATION_MEDIUM
            interpolator = AccelerateDecelerateInterpolator()
        }

        val colorAnimation = createErrorColorAnimation(view)

        AnimatorSet().apply {
            playTogether(shakeAnimation, colorAnimation)
            start()
        }
    }

    private fun createErrorColorAnimation(view: View): ValueAnimator {
        val context = view.context
        val originalColor = context.getColor(android.R.color.transparent)
        val errorColor = context.getColor(android.R.color.holo_red_light)

        return ValueAnimator.ofArgb(originalColor, errorColor, originalColor).apply {
            duration = ANIMATION_DURATION_MEDIUM
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                view.setBackgroundColor(color)
            }
        }
    }

    /**
     * Animate success state with checkmark effect
     */
    fun animateSuccessState(view: View) {
        val scaleAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply {
            duration = ANIMATION_DURATION_SHORT
            interpolator = OvershootInterpolator(1.2f)
        }

        val colorAnimation = createSuccessColorAnimation(view)

        AnimatorSet().apply {
            playTogether(scaleAnimator, colorAnimation)
            start()
        }
    }

    private fun createSuccessColorAnimation(view: View): ValueAnimator {
        val context = view.context
        val originalColor = context.getColor(android.R.color.transparent)
        val successColor = context.getColor(android.R.color.holo_green_light)

        return ValueAnimator.ofArgb(originalColor, successColor, originalColor).apply {
            duration = ANIMATION_DURATION_MEDIUM
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                view.setBackgroundColor(color)
            }
        }
    }

    /**
     * Animate fragment transition with shared elements
     */
    fun createSharedElementTransition(
        sharedElement: View,
        duration: Long = ANIMATION_DURATION_MEDIUM
    ): androidx.transition.Transition {
        return com.google.android.material.transition.MaterialContainerTransform().apply {
            this.duration = duration
            interpolator = FastOutSlowInInterpolator()
            fadeMode = com.google.android.material.transition.MaterialContainerTransform.FADE_MODE_THROUGH
            scrimColor = android.graphics.Color.TRANSPARENT
        }
    }

    /**
     * Animate search results appearance
     */
    fun animateSearchResults(
        resultsContainer: ViewGroup,
        results: List<View>
    ) {
        // Clear existing animations
        resultsContainer.clearAnimation()

        // Fade in container
        resultsContainer.alpha = 0f
        resultsContainer.isVisible = true

        resultsContainer.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION_SHORT)
            .withEndAction {
                // Animate individual results with stagger
                animateSettingItemsAppearance(results, staggerDelay = 30L)
            }
    }

    /**
     * Animate backup/sync progress with indeterminate progress
     */
    fun animateBackupProgress(
        progressView: View,
        labelView: View?,
        progress: Float,
        isIndeterminate: Boolean = false
    ) {
        if (isIndeterminate) {
            // Pulsing animation for indeterminate progress
            val pulseAnimator = ObjectAnimator.ofFloat(progressView, "alpha", 0.5f, 1f, 0.5f).apply {
                duration = 1000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }

            progressView.setTag(R.id.pulse_animator, pulseAnimator)
            pulseAnimator.start()
        } else {
            // Stop pulsing and show actual progress
            val pulseAnimator = progressView.getTag(R.id.pulse_animator) as? ObjectAnimator
            pulseAnimator?.cancel()
            progressView.alpha = 1f

            animateProgressIndicator(progressView, progress)
        }

        // Animate label changes
        labelView?.let { label ->
            label.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100L)
                .withEndAction {
                    label.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150L)
                }
        }
    }

    /**
     * Clean up animations on view destruction
     */
    fun cleanupAnimations(view: View) {
        view.clearAnimation()
        (view.getTag(R.id.shimmer_animator) as? ObjectAnimator)?.cancel()
        (view.getTag(R.id.pulse_animator) as? ObjectAnimator)?.cancel()
    }
}
