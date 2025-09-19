package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mtlc.studyplan.animations.AnimationManager
import com.mtlc.studyplan.databinding.ActivityMainPolishBinding
import com.mtlc.studyplan.navigation.NavigationBadgeManager
import com.mtlc.studyplan.navigation.NavigationStateManager
import com.mtlc.studyplan.viewmodels.AnimatedViewModel
import com.mtlc.studyplan.viewmodels.SharedAppViewModel

/**
 * Enhanced MainActivity with comprehensive polish features
 * Includes dynamic badges, state preservation, and smooth animations
 */
class MainActivity_polish : AppCompatActivity() {

    private lateinit var binding: ActivityMainPolishBinding
    private val sharedViewModel: SharedAppViewModel by viewModels()

    private lateinit var navigationBadgeManager: NavigationBadgeManager
    private lateinit var navigationStateManager: NavigationStateManager
    private lateinit var animationManager: AnimationManager

    private var currentTabId: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainPolishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupPolishFeatures()
        restoreNavigationState()
        observeSharedViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Handle navigation with animations
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val newTabId = getTabIdFromMenuItem(item.itemId)
            if (newTabId != currentTabId) {
                handleNavigationWithAnimation(currentTabId, newTabId)
                currentTabId = newTabId
            }
            true
        }
    }

    private fun setupPolishFeatures() {
        // Initialize badge system
        navigationBadgeManager = NavigationBadgeManager(
            sharedViewModel = sharedViewModel,
            bottomNavigation = binding.bottomNavigation,
            lifecycleOwner = this
        )

        // Initialize state preservation
        navigationStateManager = NavigationStateManager(this)

        // Initialize animation system
        animationManager = AnimationManager(this)

        // Setup animation observers
        observeAnimationTriggers()
    }

    private fun observeSharedViewModel() {
        // Observe badge data
        sharedViewModel.pendingTasksCount.observe(this) { count ->
            navigationBadgeManager.updateTasksBadge(count)
        }

        sharedViewModel.unreadSocialCount.observe(this) { count ->
            navigationBadgeManager.updateSocialBadge(count)
        }

        sharedViewModel.newAchievementsCount.observe(this) { count ->
            navigationBadgeManager.updateProgressBadge(count)
        }

        sharedViewModel.settingsUpdatesCount.observe(this) { count ->
            navigationBadgeManager.updateSettingsBadge(count)
        }

        sharedViewModel.streakRiskWarning.observe(this) { atRisk ->
            if (atRisk) {
                navigationBadgeManager.showStreakRisk()
            } else {
                navigationBadgeManager.hideStreakRisk()
            }
        }

        // Observe UI state changes
        sharedViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showGlobalLoading()
            } else {
                hideGlobalLoading()
            }
        }
    }

    private fun observeAnimationTriggers() {
        sharedViewModel.animationTriggers.observe(this) { trigger ->
            handleAnimationTrigger(trigger)
        }
    }

    private fun handleAnimationTrigger(trigger: AnimatedViewModel.AnimationTrigger) {
        when (trigger) {
            is AnimatedViewModel.AnimationTrigger.StreakIncrease -> {
                // Find streak view and animate
                val streakView = findViewById<android.widget.TextView>(R.id.streak_counter)
                if (streakView != null) {
                    animationManager.animateStreakUpdate(streakView, trigger.newStreak, true)
                }
            }
            is AnimatedViewModel.AnimationTrigger.AchievementUnlock -> {
                // Show achievement animation
                animationManager.animateAchievementUnlock(binding.root, trigger.achievement) {
                    // Animation completed - could show details dialog
                }
            }
            is AnimatedViewModel.AnimationTrigger.BadgeUpdate -> {
                // Animate badge update
                val badgeView = getBadgeViewForType(trigger.badgeType)
                badgeView?.let { view ->
                    animationManager.animateBadgeUpdate(view, trigger.newCount.toString())
                }
            }
            is AnimatedViewModel.AnimationTrigger.TabSwitch -> {
                handleTabSwitchAnimation(trigger.fromTab, trigger.toTab, trigger.direction)
            }
            is AnimatedViewModel.AnimationTrigger.DataRefreshStart -> {
                showRefreshIndicator()
            }
            is AnimatedViewModel.AnimationTrigger.DataRefreshComplete -> {
                hideRefreshIndicator()
            }
            is AnimatedViewModel.AnimationTrigger.LoadingStart -> {
                showGlobalLoading(trigger.message)
            }
            is AnimatedViewModel.AnimationTrigger.LoadingEnd -> {
                hideGlobalLoading()
            }
            is AnimatedViewModel.AnimationTrigger.ErrorDisplay -> {
                showErrorAnimation(trigger.error)
            }
            is AnimatedViewModel.AnimationTrigger.SuccessDisplay -> {
                showSuccessAnimation(trigger.message)
            }
        }
    }

    private fun handleNavigationWithAnimation(fromTab: String, toTab: String) {
        val direction = if (getTabIndex(toTab) > getTabIndex(fromTab)) {
            AnimationManager.TabSwitchDirection.LEFT_TO_RIGHT
        } else {
            AnimationManager.TabSwitchDirection.RIGHT_TO_LEFT
        }

        // Animate tab switch if views are available
        val fromView = getTabView(fromTab)
        val toView = getTabView(toTab)

        if (fromView != null && toView != null) {
            animationManager.animateTabSwitch(fromView, toView, direction)
        }

        // Save navigation state
        saveCurrentNavigationState()
    }

    private fun restoreNavigationState() {
        val state = navigationStateManager.restoreNavigationState()

        // Only restore if state is valid (not too old)
        if (navigationStateManager.hasValidState()) {
            // Restore selected tab
            binding.bottomNavigation.selectedItemId = state.currentTab
            currentTabId = getTabIdFromMenuItem(state.currentTab)

            // Fragment states will be restored by individual fragments
        }

        // Update last active time
        navigationStateManager.updateLastActiveTime()
    }

    private fun saveCurrentNavigationState() {
        val currentState = NavigationStateManager.NavigationState(
            currentTab = binding.bottomNavigation.selectedItemId,
            lastActiveTime = System.currentTimeMillis(),
            sessionId = generateSessionId(),
            isFirstLaunch = false
        )
        navigationStateManager.saveNavigationState(currentState)
    }

    // Helper methods for navigation
    private fun getTabIdFromMenuItem(itemId: Int): String {
        return when (itemId) {
            R.id.nav_home -> "home"
            R.id.nav_tasks -> "tasks"
            R.id.nav_progress -> "progress"
            R.id.nav_social -> "social"
            R.id.nav_settings -> "settings"
            else -> "home"
        }
    }

    private fun getTabIndex(tabId: String): Int {
        return when (tabId) {
            "home" -> 0
            "tasks" -> 1
            "progress" -> 2
            "social" -> 3
            "settings" -> 4
            else -> 0
        }
    }

    private fun getTabView(tabId: String): android.view.View? {
        // Return the actual fragment view for animation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.primaryNavigationFragment?.view
    }

    private fun getBadgeViewForType(badgeType: AnimatedViewModel.BadgeType): android.view.View? {
        // Return the appropriate badge view for animation
        return when (badgeType) {
            AnimatedViewModel.BadgeType.TASKS -> binding.bottomNavigation.getBadge(R.id.nav_tasks)?.let { null } // Badge drawable, not view
            AnimatedViewModel.BadgeType.SOCIAL -> binding.bottomNavigation.getBadge(R.id.nav_social)?.let { null }
            AnimatedViewModel.BadgeType.PROGRESS -> binding.bottomNavigation.getBadge(R.id.nav_progress)?.let { null }
            AnimatedViewModel.BadgeType.SETTINGS -> binding.bottomNavigation.getBadge(R.id.nav_settings)?.let { null }
            AnimatedViewModel.BadgeType.STREAK_WARNING -> binding.bottomNavigation.getBadge(R.id.nav_home)?.let { null }
        }
    }

    // Animation helper methods
    private fun handleTabSwitchAnimation(
        fromTab: String,
        toTab: String,
        direction: AnimatedViewModel.AnimationTrigger.TabSwitch.Direction
    ) {
        val animDirection = when (direction) {
            AnimatedViewModel.AnimationTrigger.TabSwitch.Direction.LEFT_TO_RIGHT -> AnimationManager.TabSwitchDirection.LEFT_TO_RIGHT
            AnimatedViewModel.AnimationTrigger.TabSwitch.Direction.RIGHT_TO_LEFT -> AnimationManager.TabSwitchDirection.RIGHT_TO_LEFT
        }

        val fromView = getTabView(fromTab)
        val toView = getTabView(toTab)

        if (fromView != null && toView != null) {
            animationManager.animateTabSwitch(fromView, toView, animDirection)
        }
    }

    private fun showGlobalLoading(message: String = "Loading...") {
        // Implementation for global loading indicator
        binding.loadingOverlay?.visibility = android.view.View.VISIBLE
    }

    private fun hideGlobalLoading() {
        // Implementation to hide global loading indicator
        binding.loadingOverlay?.visibility = android.view.View.GONE
    }

    private fun showRefreshIndicator() {
        // Implementation for refresh indicator
    }

    private fun hideRefreshIndicator() {
        // Implementation to hide refresh indicator
    }

    private fun showErrorAnimation(error: String) {
        // Implementation for error display animation
    }

    private fun showSuccessAnimation(message: String) {
        // Implementation for success display animation
    }

    private fun generateSessionId(): String {
        return System.currentTimeMillis().toString()
    }

    override fun onPause() {
        super.onPause()
        saveCurrentNavigationState()
    }

    override fun onResume() {
        super.onResume()
        navigationStateManager.updateLastActiveTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear any ongoing animations
        if (::animationManager.isInitialized) {
            // Clean up animations if needed
        }
    }
}