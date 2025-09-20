package com.mtlc.studyplan.ui.animations

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.accessibility.AccessibilityManager

class SettingsAnimationCoordinator(
    private val context: Context,
    private val accessibilityManager: AccessibilityManager
) {

    private val sharedElementHelper = SharedElementTransitionHelper(accessibilityManager)
    private val activeAnimators = mutableSetOf<Animator>()

    private fun scaledDuration(base: Long): Long =
        if (accessibilityManager.shouldUseReducedMotion()) 0L else base

    fun setupFragment(fragment: Fragment) {
        sharedElementHelper.setupSharedElementTransitions(fragment)
    }

    fun animateSettingToggle(
        view: View,
        isEnabled: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        val targetAlpha = if (isEnabled) 1f else 0.6f
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, targetAlpha).apply {
            duration = scaledDuration(180L)
        }
        startAnimator(animator, onComplete)
    }

    fun animateValueChange(
        view: View,
        fromValue: String,
        toValue: String,
        onComplete: (() -> Unit)? = null
    ) {
        onComplete?.invoke()
    }

    fun animateSearchResults(resultsContainer: ViewGroup, itemCount: Int) {
        // No-op stub
    }

    fun animateConflictResolution(conflictView: View, resolution: Any?) {
        // No-op stub
    }

    fun createLoadingAnimation(view: View): Animator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 0.3f, 1f).apply {
            duration = scaledDuration(400L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
    }

    fun animateProgress(progressView: View, fromProgress: Float, toProgress: Float): Animator {
        return ObjectAnimator.ofFloat(progressView, View.ALPHA, 1f).apply {
            duration = scaledDuration(250L)
        }
    }

    fun animateBackupProgress(
        progressContainer: View,
        statusText: View,
        progressValue: Float,
        statusMessage: String
    ): Animator {
        return ObjectAnimator.ofFloat(progressContainer, View.ALPHA, progressContainer.alpha, 1f).apply {
            duration = scaledDuration(250L)
        }
    }

    fun setupListAnimations(recyclerView: RecyclerView) {
        recyclerView.itemAnimator = null
    }

    fun addSharedElements(
        transaction: FragmentTransaction,
        sharedElements: List<SharedElementTransitionHelper.SharedElementPair>
    ): FragmentTransaction {
        return sharedElementHelper.addSharedElements(transaction, sharedElements)
    }

    fun createSettingsCardTransition(
        cardView: View,
        iconView: View? = null,
        titleView: View? = null,
        valueView: View? = null
    ): List<SharedElementTransitionHelper.SharedElementPair> {
        return sharedElementHelper.createSettingsCardTransition(cardView, iconView, titleView, valueView)
    }

    fun createSearchBarTransition(searchBar: View): SharedElementTransitionHelper.SharedElementPair {
        return sharedElementHelper.createSearchBarTransition(searchBar)
    }

    fun createBackButtonTransition(backButton: View): SharedElementTransitionHelper.SharedElementPair {
        return sharedElementHelper.createBackButtonTransition(backButton)
    }

    fun postponeEnterTransition(fragment: Fragment) {
        sharedElementHelper.postponeEnterTransition(fragment)
    }

    fun startPostponedEnterTransition(fragment: Fragment) {
        sharedElementHelper.startPostponedEnterTransition(fragment)
    }

    fun enhanceAccessibilityForTransitions(view: View, description: String) {
        sharedElementHelper.enhanceAccessibilityForTransitions(view, description)
    }

    fun animateSettingsEntry(container: ViewGroup, onComplete: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(container, View.ALPHA, 0f, 1f).apply {
            duration = scaledDuration(220L)
        }
        startAnimator(animator, onComplete)
    }

    fun animateSettingsExit(container: ViewGroup, onComplete: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(container, View.ALPHA, container.alpha, 0f).apply {
            duration = scaledDuration(180L)
        }
        startAnimator(animator, onComplete)
    }

    fun animatePageTransition(
        exitingView: View,
        enteringView: View,
        direction: PageTransitionDirection,
        onComplete: (() -> Unit)? = null
    ) {
        if (shouldUseReducedMotion()) {
            exitingView.visibility = View.GONE
            enteringView.visibility = View.VISIBLE
            onComplete?.invoke()
            return
        }

        val exitAnimator = ObjectAnimator.ofFloat(exitingView, View.ALPHA, 1f, 0f).apply {
            duration = scaledDuration(200L)
        }
        val enterAnimator = ObjectAnimator.ofFloat(enteringView, View.ALPHA, 0f, 1f).apply {
            duration = scaledDuration(200L)
        }

        AnimatorSet().apply {
            playTogether(exitAnimator, enterAnimator)
            startAnimator(this, onComplete)
        }
    }

    fun animateErrorState(errorView: View, onComplete: (() -> Unit)? = null) {
        if (shouldUseReducedMotion()) {
            onComplete?.invoke()
            return
        }

        val animator = ObjectAnimator.ofFloat(errorView, View.TRANSLATION_X, 0f, -8f, 8f, 0f).apply {
            duration = scaledDuration(300L)
        }
        startAnimator(animator, onComplete)
    }

    fun animateSuccessState(successView: View, onComplete: (() -> Unit)? = null) {
        if (shouldUseReducedMotion()) {
            onComplete?.invoke()
            return
        }

        val scaleX = ObjectAnimator.ofFloat(successView, View.SCALE_X, 0.85f, 1f)
        val scaleY = ObjectAnimator.ofFloat(successView, View.SCALE_Y, 0.85f, 1f)
        AnimatorSet().apply {
            duration = scaledDuration(240L)
            playTogether(scaleX, scaleY)
            startAnimator(this, onComplete)
        }
    }

    private fun startAnimator(animator: Animator, onComplete: (() -> Unit)?) {
        trackAnimator(animator)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
                removeAnimator(animator)
            }
            override fun onAnimationCancel(animation: Animator) {
                removeAnimator(animator)
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.start()
    }

    private fun trackAnimator(animator: Animator) {
        activeAnimators.add(animator)
    }

    private fun removeAnimator(animator: Animator) {
        activeAnimators.remove(animator)
    }

    fun cancelAllAnimations() {
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
    }

    fun isAnimating(): Boolean = activeAnimators.isNotEmpty()

    fun getAnimationDuration(baseDuration: Long): Long = scaledDuration(baseDuration)

    fun shouldUseReducedMotion(): Boolean = accessibilityManager.shouldUseReducedMotion()

    enum class PageTransitionDirection {
        FORWARD, BACKWARD, UP, DOWN
    }
}
