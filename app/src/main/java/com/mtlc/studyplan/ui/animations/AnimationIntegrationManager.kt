package com.mtlc.studyplan.ui.animations

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.mtlc.studyplan.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.StateFlow

class AnimationIntegrationManager(
    private val context: Context,
    private val accessibilityManager: AccessibilityManager
) {

    companion object {
        private const val ANIMATION_DURATION_SHORT = 150L
        private const val ANIMATION_DURATION_MEDIUM = 300L
        private const val ANIMATION_DURATION_LONG = 500L
        private const val ANIMATION_DURATION_EXTRA_LONG = 800L

        private const val SPRING_DAMPING_RATIO = 0.8f
        private const val SPRING_STIFFNESS = 400f
    }

    fun getAnimationDuration(baseDuration: Long): Long {
        return accessibilityManager.getAnimationDuration(baseDuration)
    }

    fun createSettingsTransition(): Transition {
        val transitionSet = TransitionSet().apply {
            addTransition(Fade().apply {
                duration = getAnimationDuration(ANIMATION_DURATION_SHORT)
            })
            addTransition(Slide().apply {
                duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)
                slideEdge = android.view.Gravity.END
            })
            addTransition(ChangeBounds().apply {
                duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)
            })
            interpolator = DecelerateInterpolator()
        }

        return transitionSet
    }

    fun createSharedElementTransition(): Transition {
        return TransitionSet().apply {
            addTransition(ChangeImageTransform())
            addTransition(ChangeBounds())
            addTransition(ChangeClipBounds())
            duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    fun animateSettingToggle(view: View, isEnabled: Boolean, onAnimationEnd: (() -> Unit)? = null): Animator {
        val duration = getAnimationDuration(ANIMATION_DURATION_SHORT)

        if (accessibilityManager.shouldUseReducedMotion()) {
            view.alpha = if (isEnabled) 1.0f else 0.5f
            onAnimationEnd?.invoke()
            return ValueAnimator.ofFloat(0f, 1f).apply { this.duration = 0L }
        }

        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.1f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.1f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", view.alpha, if (isEnabled) 1.0f else 0.5f)

        return AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = duration
            interpolator = OvershootInterpolator()

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) { onAnimationEnd?.invoke() }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }

    fun animateValueChange(view: View, fromValue: String, toValue: String, onAnimationEnd: (() -> Unit)? = null): Animator {
        val duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)

        if (accessibilityManager.shouldUseReducedMotion()) {
            onAnimationEnd?.invoke()
            return ValueAnimator.ofFloat(0f, 1f).apply { this.duration = 0L }
        }

        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1.0f, 0.3f)
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1.0f)
        val scaleOut = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.9f)
        val scaleIn = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1.0f)

        return AnimatorSet().apply {
            play(fadeOut).with(scaleOut)
            play(fadeIn).with(scaleIn).after(fadeOut)
            this.duration = duration / 2
            interpolator = AccelerateDecelerateInterpolator()

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) { onAnimationEnd?.invoke() }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }

    fun animateListItemChange(viewHolder: RecyclerView.ViewHolder, changeType: ChangeType): Animator {
        val view = viewHolder.itemView
        val duration = getAnimationDuration(ANIMATION_DURATION_SHORT)

        if (accessibilityManager.shouldUseReducedMotion()) {
            return ValueAnimator.ofFloat(0f, 1f).apply { this.duration = 0L }
        }

        return when (changeType) {
            ChangeType.ADDED -> createSlideInAnimation(view, duration)
            ChangeType.REMOVED -> createSlideOutAnimation(view, duration)
            ChangeType.CHANGED -> createPulseAnimation(view, duration)
            ChangeType.MOVED -> createMoveAnimation(view, duration)
        }
    }

    private fun createSlideInAnimation(view: View, duration: Long): Animator {
        view.translationX = view.width.toFloat()
        view.alpha = 0f

        val slideIn = ObjectAnimator.ofFloat(view, "translationX", view.translationX, 0f)
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        return AnimatorSet().apply {
            playTogether(slideIn, fadeIn)
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }
    }

    private fun createSlideOutAnimation(view: View, duration: Long): Animator {
        val slideOut = ObjectAnimator.ofFloat(view, "translationX", 0f, -view.width.toFloat())
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        return AnimatorSet().apply {
            playTogether(slideOut, fadeOut)
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun createPulseAnimation(view: View, duration: Long): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.05f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.05f, 1.0f)

        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun createMoveAnimation(view: View, duration: Long): Animator {
        return ObjectAnimator.ofFloat(view, "translationY", -20f, 0f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }
    }

    fun animateSearchResults(resultsContainer: ViewGroup, itemCount: Int) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        val stagger = getAnimationDuration(50L)

        for (i in 0 until minOf(itemCount, resultsContainer.childCount)) {
            val child = resultsContainer.getChildAt(i)
            child.alpha = 0f
            child.translationY = 50f

            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(getAnimationDuration(ANIMATION_DURATION_SHORT))
                .setStartDelay(i * stagger)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun animateConflictResolution(conflictView: View, resolution: ConflictResolution) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        val duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)

        when (resolution) {
            ConflictResolution.RESOLVED -> {
                val colorAnimation = ValueAnimator.ofArgb(
                    0xFFFFEBEE.toInt(), // Light red
                    0xFFE8F5E8.toInt()  // Light green
                )
                colorAnimation.duration = duration
                colorAnimation.addUpdateListener { animator ->
                    conflictView.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
            }
            ConflictResolution.PENDING -> {
                val pulse = ObjectAnimator.ofFloat(conflictView, "alpha", 1.0f, 0.7f, 1.0f)
                pulse.duration = duration
                pulse.repeatCount = 2
                pulse.start()
            }
            ConflictResolution.ERROR -> {
                val shake = ObjectAnimator.ofFloat(conflictView, "translationX", 0f, -10f, 10f, -5f, 5f, 0f)
                shake.duration = duration
                shake.start()
            }
        }
    }

    fun createLoadingAnimation(view: View): Animator {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return ValueAnimator.ofFloat(0f, 1f).apply { this.duration = 0L }
        }

        val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        rotation.duration = getAnimationDuration(ANIMATION_DURATION_LONG)
        rotation.repeatCount = ValueAnimator.INFINITE
        rotation.interpolator = AccelerateDecelerateInterpolator()
        return rotation
    }

    fun animateProgress(progressView: View, fromProgress: Float, toProgress: Float): Animator {
        val duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)

        if (accessibilityManager.shouldUseReducedMotion()) {
            return ValueAnimator.ofFloat(fromProgress, toProgress).apply {
                this.duration = 0L
                addUpdateListener { progressView.scaleX = it.animatedValue as Float }
            }
        }

        return ObjectAnimator.ofFloat(progressView, "scaleX", fromProgress, toProgress).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }
    }

    fun setupActivityTransitions(fragment: Fragment) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        fragment.enterTransition = createSettingsTransition()
        fragment.exitTransition = createSettingsTransition()
        fragment.sharedElementEnterTransition = createSharedElementTransition()
        fragment.sharedElementReturnTransition = createSharedElementTransition()
    }

    fun animateBackupProgress(
        progressContainer: View,
        statusText: View,
        progressValue: Float,
        statusMessage: String
    ): Animator {
        val duration = getAnimationDuration(ANIMATION_DURATION_MEDIUM)

        if (accessibilityManager.shouldUseReducedMotion()) {
            return ValueAnimator.ofFloat(0f, 1f).apply { this.duration = 0L }
        }

        val progressAnimation = ObjectAnimator.ofFloat(progressContainer, "scaleX", progressContainer.scaleX, progressValue)
        val textFade = ObjectAnimator.ofFloat(statusText, "alpha", 1f, 0f, 1f)

        return AnimatorSet().apply {
            play(progressAnimation).with(textFade)
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    fun createStaggeredListAnimation(recyclerView: RecyclerView, delay: Long = 50L): RecyclerView.ItemAnimator {
        return object : RecyclerView.ItemAnimator() {
            override fun animateDisappearance(
                viewHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo,
                postLayoutInfo: ItemHolderInfo?
            ): Boolean {
                return animateListItemChange(viewHolder, ChangeType.REMOVED).let {
                    it.start()
                    true
                }
            }

            override fun animateAppearance(
                viewHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo?,
                postLayoutInfo: ItemHolderInfo
            ): Boolean {
                return animateListItemChange(viewHolder, ChangeType.ADDED).let {
                    it.startDelay = viewHolder.bindingAdapterPosition * getAnimationDuration(delay)
                    it.start()
                    true
                }
            }

            override fun animatePersistence(
                viewHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo,
                postLayoutInfo: ItemHolderInfo
            ): Boolean = false

            override fun animateChange(
                oldHolder: RecyclerView.ViewHolder,
                newHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo,
                postLayoutInfo: ItemHolderInfo
            ): Boolean {
                return animateListItemChange(newHolder, ChangeType.CHANGED).let {
                    it.start()
                    true
                }
            }

            override fun runPendingAnimations() {}
            override fun endAnimation(item: RecyclerView.ViewHolder) {}
            override fun endAnimations() {}
            override fun isRunning(): Boolean = false
        }
    }

    enum class ChangeType {
        ADDED, REMOVED, CHANGED, MOVED
    }

    enum class ConflictResolution {
        RESOLVED, PENDING, ERROR
    }
}
