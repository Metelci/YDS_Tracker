package com.mtlc.studyplan.ui.animations

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.ui.animations.AnimationIntegrationManager
import com.mtlc.studyplan.ui.animations.SharedElementTransitionHelper
import kotlinx.coroutines.flow.StateFlow

class SettingsAnimationCoordinator(
    private val context: Context,
    private val accessibilityManager: AccessibilityManager
) {

    private val animationIntegrationManager = AnimationIntegrationManager(context, accessibilityManager)
    private val sharedElementHelper = SharedElementTransitionHelper(accessibilityManager)
    private val appAnimations = AppAnimations(context)

    private val activeAnimators = mutableSetOf<Animator>()

    fun setupFragment(fragment: Fragment) {
        sharedElementHelper.setupSharedElementTransitions(fragment)
        animationIntegrationManager.setupActivityTransitions(fragment)
    }

    fun animateSettingToggle(
        view: View,
        isEnabled: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        val animator = animationIntegrationManager.animateSettingToggle(view, isEnabled) {
            onComplete?.invoke()
        }

        trackAnimator(animator)
        animator.start()
    }

    fun animateValueChange(
        view: View,
        fromValue: String,
        toValue: String,
        onComplete: (() -> Unit)? = null
    ) {
        val animator = animationIntegrationManager.animateValueChange(view, fromValue, toValue) {
            onComplete?.invoke()
        }

        trackAnimator(animator)
        animator.start()
    }

    fun animateSearchResults(resultsContainer: ViewGroup, itemCount: Int) {
        animationIntegrationManager.animateSearchResults(resultsContainer, itemCount)
    }

    fun animateConflictResolution(
        conflictView: View,
        resolution: AnimationIntegrationManager.ConflictResolution
    ) {
        animationIntegrationManager.animateConflictResolution(conflictView, resolution)
    }

    fun createLoadingAnimation(view: View): Animator {
        val animator = animationIntegrationManager.createLoadingAnimation(view)
        trackAnimator(animator)
        return animator
    }

    fun animateProgress(
        progressView: View,
        fromProgress: Float,
        toProgress: Float
    ): Animator {
        val animator = animationIntegrationManager.animateProgress(progressView, fromProgress, toProgress)
        trackAnimator(animator)
        return animator
    }

    fun animateBackupProgress(
        progressContainer: View,
        statusText: View,
        progressValue: Float,
        statusMessage: String
    ): Animator {
        val animator = animationIntegrationManager.animateBackupProgress(
            progressContainer, statusText, progressValue, statusMessage
        )
        trackAnimator(animator)
        return animator
    }

    fun setupListAnimations(recyclerView: RecyclerView) {
        if (!accessibilityManager.shouldUseReducedMotion()) {
            recyclerView.itemAnimator = animationIntegrationManager.createStaggeredListAnimation(recyclerView)
        } else {
            recyclerView.itemAnimator = null
        }
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

    fun animateSettingsEntry(
        container: ViewGroup,
        onComplete: (() -> Unit)? = null
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) {
            onComplete?.invoke()
            return
        }

        val slideAnimation = appAnimations.createSlideInFromBottomAnimation(container)
        val fadeAnimation = appAnimations.createFadeInAnimation(container)

        val animatorSet = AnimatorSet().apply {
            playTogether(slideAnimation, fadeAnimation)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                    removeAnimator(this@apply)
                }
                override fun onAnimationCancel(animation: Animator) {
                    removeAnimator(this@apply)
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        trackAnimator(animatorSet)
        animatorSet.start()
    }

    fun animateSettingsExit(
        container: ViewGroup,
        onComplete: (() -> Unit)? = null
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) {
            onComplete?.invoke()
            return
        }

        val slideAnimation = appAnimations.createSlideOutToTopAnimation(container)
        val fadeAnimation = appAnimations.createFadeOutAnimation(container)

        val animatorSet = AnimatorSet().apply {
            playTogether(slideAnimation, fadeAnimation)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                    removeAnimator(this@apply)
                }
                override fun onAnimationCancel(animation: Animator) {
                    removeAnimator(this@apply)
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        trackAnimator(animatorSet)
        animatorSet.start()
    }

    fun animatePageTransition(
        exitingView: View,
        enteringView: View,
        direction: PageTransitionDirection,
        onComplete: (() -> Unit)? = null
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) {
            exitingView.visibility = View.GONE
            enteringView.visibility = View.VISIBLE
            onComplete?.invoke()
            return
        }

        val (exitAnimation, enterAnimation) = when (direction) {
            PageTransitionDirection.FORWARD -> {
                appAnimations.createSlideOutToLeftAnimation(exitingView) to
                appAnimations.createSlideInFromRightAnimation(enteringView)
            }
            PageTransitionDirection.BACKWARD -> {
                appAnimations.createSlideOutToRightAnimation(exitingView) to
                appAnimations.createSlideInFromLeftAnimation(enteringView)
            }
            PageTransitionDirection.UP -> {
                appAnimations.createSlideOutToTopAnimation(exitingView) to
                appAnimations.createSlideInFromBottomAnimation(enteringView)
            }
            PageTransitionDirection.DOWN -> {
                appAnimations.createSlideOutToBottomAnimation(exitingView) to
                appAnimations.createSlideInFromTopAnimation(enteringView)
            }
        }

        val animatorSet = AnimatorSet().apply {
            playTogether(exitAnimation, enterAnimation)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                    removeAnimator(this@apply)
                }
                override fun onAnimationCancel(animation: Animator) {
                    removeAnimator(this@apply)
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        trackAnimator(animatorSet)
        animatorSet.start()
    }

    fun animateErrorState(
        errorView: View,
        onComplete: (() -> Unit)? = null
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) {
            onComplete?.invoke()
            return
        }

        val shakeAnimation = appAnimations.createShakeAnimation(errorView)
        val pulseAnimation = appAnimations.createPulseAnimation(errorView)

        val animatorSet = AnimatorSet().apply {
            play(shakeAnimation).before(pulseAnimation)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                    removeAnimator(this@apply)
                }
                override fun onAnimationCancel(animation: Animator) {
                    removeAnimator(this@apply)
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        trackAnimator(animatorSet)
        animatorSet.start()
    }

    fun animateSuccessState(
        successView: View,
        onComplete: (() -> Unit)? = null
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) {
            onComplete?.invoke()
            return
        }

        val bounceAnimation = appAnimations.createBounceAnimation(successView)
        val fadeAnimation = appAnimations.createFadeInAnimation(successView)

        val animatorSet = AnimatorSet().apply {
            playTogether(bounceAnimation, fadeAnimation)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                    removeAnimator(this@apply)
                }
                override fun onAnimationCancel(animation: Animator) {
                    removeAnimator(this@apply)
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        trackAnimator(animatorSet)
        animatorSet.start()
    }

    private fun trackAnimator(animator: Animator) {
        activeAnimators.add(animator)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                removeAnimator(animation)
            }
            override fun onAnimationCancel(animation: Animator) {
                removeAnimator(animation)
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun removeAnimator(animator: Animator) {
        activeAnimators.remove(animator)
    }

    fun cancelAllAnimations() {
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
    }

    fun isAnimating(): Boolean {
        return activeAnimators.isNotEmpty()
    }

    fun getAnimationDuration(baseDuration: Long): Long {
        return accessibilityManager.getAnimationDuration(baseDuration)
    }

    fun shouldUseReducedMotion(): Boolean {
        return accessibilityManager.shouldUseReducedMotion()
    }

    enum class PageTransitionDirection {
        FORWARD, BACKWARD, UP, DOWN
    }
}