package com.mtlc.studyplan.ui.animations

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Pair
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair as AndroidXPair
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.transition.*
import com.mtlc.studyplan.accessibility.AccessibilityManager

class SharedElementTransitionHelper(
    private val accessibilityManager: AccessibilityManager
) {

    companion object {
        const val SHARED_ELEMENT_TRANSITION_DURATION = 300L
        const val TRANSITION_NAME_SETTING_CARD = "setting_card"
        const val TRANSITION_NAME_SETTING_ICON = "setting_icon"
        const val TRANSITION_NAME_SETTING_TITLE = "setting_title"
        const val TRANSITION_NAME_SETTING_VALUE = "setting_value"
        const val TRANSITION_NAME_SEARCH_BAR = "search_bar"
        const val TRANSITION_NAME_BACK_BUTTON = "back_button"
    }

    fun createSharedElementTransition(): Transition {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return Fade().apply {
                duration = 0L
            }
        }

        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER

            addTransition(ChangeBounds().apply {
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
            })

            addTransition(ChangeTransform().apply {
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
            })

            addTransition(ChangeClipBounds().apply {
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
            })

            addTransition(ChangeImageTransform().apply {
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
            })

            interpolator = FastOutSlowInInterpolator()

            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    super.onTransitionStart(transition)
                    // Announce transition start for accessibility
                    if (accessibilityManager.isScreenReaderActive()) {
                        accessibilityManager.announceForAccessibility("Navigating to settings")
                    }
                }

                override fun onTransitionEnd(transition: Transition) {
                    super.onTransitionEnd(transition)
                    // Announce transition completion for accessibility
                    if (accessibilityManager.isScreenReaderActive()) {
                        accessibilityManager.announceForAccessibility("Navigation complete")
                    }
                }
            })
        }
    }

    fun createEnterTransition(): Transition {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return Fade().apply { duration = 0L }
        }

        return TransitionSet().apply {
            addTransition(Slide().apply {
                slideEdge = android.view.Gravity.END
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
                addTarget(android.R.id.content)
            })

            addTransition(Fade().apply {
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION / 2)
                startDelay = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION / 4)
            })

            interpolator = DecelerateInterpolator()
        }
    }

    fun createExitTransition(): Transition {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return Fade().apply { duration = 0L }
        }

        return TransitionSet().apply {
            addTransition(Slide().apply {
                slideEdge = android.view.Gravity.START
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
                addTarget(android.R.id.content)
            })

            addTransition(Fade().apply {
                duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION / 2)
            })

            interpolator = AccelerateInterpolator()
        }
    }

    fun setupSharedElementTransitions(fragment: Fragment) {
        if (accessibilityManager.shouldUseReducedMotion()) {
            // Use simple fade transitions for accessibility
            fragment.enterTransition = Fade().apply { duration = 0L }
            fragment.exitTransition = Fade().apply { duration = 0L }
            fragment.sharedElementEnterTransition = Fade().apply { duration = 0L }
            fragment.sharedElementReturnTransition = Fade().apply { duration = 0L }
            return
        }

        fragment.enterTransition = createEnterTransition()
        fragment.exitTransition = createExitTransition()
        fragment.sharedElementEnterTransition = createSharedElementTransition()
        fragment.sharedElementReturnTransition = createSharedElementTransition()

        // Allow return transition overlap for smoother animations
        fragment.allowReturnTransitionOverlap = true
        fragment.allowEnterTransitionOverlap = false
    }

    fun addSharedElements(
        transaction: FragmentTransaction,
        sharedElements: List<SharedElementPair>
    ): FragmentTransaction {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return transaction
        }

        sharedElements.forEach { pair ->
            transaction.addSharedElement(pair.view, pair.transitionName)
        }

        return transaction
    }

    fun createSharedElementPair(view: View, transitionName: String): SharedElementPair {
        ViewCompat.setTransitionName(view, transitionName)
        return SharedElementPair(view, transitionName)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createActivityOptionsCompat(
        context: Context,
        sharedElements: List<SharedElementPair>
    ): ActivityOptionsCompat? {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return null
        }

        val pairs = sharedElements.map { pair ->
            AndroidXPair.create(pair.view, pair.transitionName)
        }.toTypedArray()

        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            context as androidx.fragment.app.FragmentActivity,
            *pairs
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createActivityOptions(
        sharedElements: List<SharedElementPair>
    ): ActivityOptions? {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return null
        }

        val pairs = sharedElements.map { pair ->
            Pair.create(pair.view, pair.transitionName)
        }.toTypedArray()

        return ActivityOptions.makeSceneTransitionAnimation(
            null, // Will be set by the calling activity
            *pairs
        )
    }

    fun prepareSharedElementTransition(
        sourceView: View,
        targetTransitionName: String
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        // Set the transition name for the source view
        ViewCompat.setTransitionName(sourceView, targetTransitionName)

        // Ensure the view is laid out before transition
        sourceView.post {
            // Pre-draw to ensure smooth transition
            sourceView.viewTreeObserver.addOnPreDrawListener(
                object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        sourceView.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    }
                }
            )
        }
    }

    fun createSettingsCardTransition(
        cardView: View,
        iconView: View?,
        titleView: View?,
        valueView: View?
    ): List<SharedElementPair> {
        val sharedElements = mutableListOf<SharedElementPair>()

        sharedElements.add(createSharedElementPair(cardView, TRANSITION_NAME_SETTING_CARD))

        iconView?.let {
            sharedElements.add(createSharedElementPair(it, TRANSITION_NAME_SETTING_ICON))
        }

        titleView?.let {
            sharedElements.add(createSharedElementPair(it, TRANSITION_NAME_SETTING_TITLE))
        }

        valueView?.let {
            sharedElements.add(createSharedElementPair(it, TRANSITION_NAME_SETTING_VALUE))
        }

        return sharedElements
    }

    fun createSearchBarTransition(searchBar: View): SharedElementPair {
        return createSharedElementPair(searchBar, TRANSITION_NAME_SEARCH_BAR)
    }

    fun createBackButtonTransition(backButton: View): SharedElementPair {
        return createSharedElementPair(backButton, TRANSITION_NAME_BACK_BUTTON)
    }

    fun postponeEnterTransition(fragment: Fragment) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        fragment.postponeEnterTransition()
    }

    fun startPostponedEnterTransition(fragment: Fragment) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        fragment.startPostponedEnterTransition()
    }

    fun scheduleSharedElementCallback(
        fragment: Fragment,
        onMapSharedElements: (names: MutableList<String>, sharedElements: MutableMap<String, View>) -> Unit
    ) {
        if (accessibilityManager.shouldUseReducedMotion()) return

        fragment.setSharedElementEnterTransition(createSharedElementTransition())

        fragment.setEnterSharedElementCallback(object : androidx.core.app.SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>
            ) {
                onMapSharedElements(names, sharedElements)
            }
        })
    }

    fun createMorphTransition(
        startView: View,
        endView: View,
        transitionName: String
    ): Transition {
        if (accessibilityManager.shouldUseReducedMotion()) {
            return Fade().apply { duration = 0L }
        }

        ViewCompat.setTransitionName(startView, transitionName)
        ViewCompat.setTransitionName(endView, transitionName)

        return TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(AutoTransition())
            duration = accessibilityManager.getAnimationDuration(SHARED_ELEMENT_TRANSITION_DURATION)
            interpolator = FastOutSlowInInterpolator()
        }
    }

    fun enhanceAccessibilityForTransitions(view: View, description: String) {
        if (accessibilityManager.isScreenReaderActive()) {
            view.contentDescription = "$description. Will animate when selected."
            view.isAccessibilityFocusable = true
        }
    }

    data class SharedElementPair(
        val view: View,
        val transitionName: String
    )
}