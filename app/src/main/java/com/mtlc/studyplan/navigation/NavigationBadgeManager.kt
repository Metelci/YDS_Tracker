package com.mtlc.studyplan.navigation

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mtlc.studyplan.R
import com.mtlc.studyplan.viewmodels.SharedAppViewModel

class NavigationBadgeManager(
    private val sharedViewModel: SharedAppViewModel,
    private val bottomNavigation: BottomNavigationView,
    private val lifecycleOwner: LifecycleOwner
) {

    private val badgeDrawables = mutableMapOf<Int, BadgeDrawable>()
    private val context: Context = bottomNavigation.context

    init {
        setupBadgeObservers()
        initializeBadges()
    }

    private fun initializeBadges() {
        val navItems = listOf(
            R.id.nav_home,
            R.id.nav_tasks,
            R.id.nav_progress,
            R.id.nav_social,
            R.id.nav_settings
        )

        navItems.forEach { itemId ->
            val badge = bottomNavigation.getOrCreateBadge(itemId).apply {
                backgroundColor = ContextCompat.getColor(context, R.color.badge_background)
                badgeTextColor = ContextCompat.getColor(context, R.color.badge_text)
                maxCharacterCount = 3
                isVisible = false
            }
            badgeDrawables[itemId] = badge
        }
    }

    private fun setupBadgeObservers() {
        // Tasks badge - show pending tasks count
        sharedViewModel.pendingTasksCount.observe(lifecycleOwner, Observer { count ->
            updateBadge(R.id.nav_tasks, count, "tasks pending")
        })

        // Social badge - show unread notifications
        sharedViewModel.unreadSocialCount.observe(lifecycleOwner, Observer { count ->
            updateBadge(R.id.nav_social, count, "new notifications")
        })

        // Progress badge - show new achievements
        sharedViewModel.newAchievementsCount.observe(lifecycleOwner, Observer { count ->
            updateBadge(R.id.nav_progress, count, "new achievements")
        })

        // Settings badge - show important updates
        sharedViewModel.settingsUpdatesCount.observe(lifecycleOwner, Observer { count ->
            updateBadge(R.id.nav_settings, count, "updates available")
        })

        // Home badge - show daily streak risk
        sharedViewModel.streakRiskWarning.observe(lifecycleOwner, Observer { atRisk ->
            if (atRisk) {
                showStreakWarningBadge()
            } else {
                hideBadge(R.id.nav_home)
            }
        })
    }

    private fun updateBadge(itemId: Int, count: Int, description: String) {
        val badge = badgeDrawables[itemId] ?: return

        if (count > 0) {
            badge.number = count
            badge.contentDescription = "$count $description"
            badge.isVisible = true

            // Add subtle animation when badge appears
            animateBadgeAppearance(badge)
        } else {
            badge.isVisible = false
        }
    }

    private fun showStreakWarningBadge() {
        val badge = badgeDrawables[R.id.nav_home] ?: return
        badge.apply {
            backgroundColor = ContextCompat.getColor(context, R.color.warning_red)
            badgeTextColor = Color.WHITE
            text = "!"
            contentDescription = "Streak at risk"
            isVisible = true
        }

        // Pulsing animation for warning
        animateWarningBadge(badge)
    }

    private fun animateBadgeAppearance(badge: BadgeDrawable) {
        // Scale animation to draw attention
        val scaleAnimator = ObjectAnimator.ofFloat(1.5f, 1.0f).apply {
            duration = 200
            interpolator = OvershootInterpolator()
        }
        scaleAnimator.start()
    }

    private fun animateWarningBadge(badge: BadgeDrawable) {
        val pulseAnimator = ObjectAnimator.ofFloat(0.8f, 1.2f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulseAnimator.start()
    }

    fun clearBadge(itemId: Int) {
        badgeDrawables[itemId]?.isVisible = false
    }

    fun clearAllBadges() {
        badgeDrawables.values.forEach { it.isVisible = false }
    }

    fun updateTasksBadge(count: Int) {
        updateBadge(R.id.nav_tasks, count, "pending tasks")
    }

    fun updateSocialBadge(count: Int) {
        updateBadge(R.id.nav_social, count, "unread notifications")
    }

    fun updateProgressBadge(count: Int) {
        updateBadge(R.id.nav_progress, count, "new achievements")
    }

    fun updateSettingsBadge(count: Int) {
        updateBadge(R.id.nav_settings, count, "available updates")
    }

    fun showStreakRisk() {
        showStreakWarningBadge()
    }

    fun hideStreakRisk() {
        hideBadge(R.id.nav_home)
    }

    private fun hideBadge(itemId: Int) {
        badgeDrawables[itemId]?.isVisible = false
    }
}