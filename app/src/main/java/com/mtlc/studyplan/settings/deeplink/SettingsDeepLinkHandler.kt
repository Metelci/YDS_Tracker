package com.mtlc.studyplan.settings.deeplink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.mtlc.studyplan.R
import com.mtlc.studyplan.settings.ui.*
import kotlinx.coroutines.*

/**
 * Handles deep linking for settings navigation with comprehensive URL support
 */
class SettingsDeepLinkHandler(private val activity: FragmentActivity) {

    companion object {
        // Deep link schemes
        const val SCHEME_HTTPS = "https"
        const val SCHEME_STUDYPLAN = "studyplan"

        // Deep link hosts
        const val HOST_SETTINGS = "settings"
        const val HOST_APP = "app.studyplan.com"

        // Deep link paths
        const val PATH_SETTINGS = "settings"
        const val PATH_SEARCH = "search"

        // Query parameters
        const val PARAM_CATEGORY = "category"
        const val PARAM_SETTING = "setting"
        const val PARAM_QUERY = "q"
        const val PARAM_HIGHLIGHT = "highlight"

        // Supported URLs:
        // studyplan://settings/privacy
        // studyplan://settings/privacy/anonymous_analytics
        // studyplan://settings/search?q=notifications
        // https://app.studyplan.com/settings/privacy
        // https://app.studyplan.com/settings/search?q=privacy
    }

    private val fragmentManager: FragmentManager = activity.supportFragmentManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Handle deep link from Intent
     */
    fun handleDeepLink(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        return handleDeepLink(uri)
    }

    /**
     * Handle deep link from URI
     */
    fun handleDeepLink(uri: Uri): Boolean {
        return when {
            isSettingsDeepLink(uri) -> {
                handleSettingsDeepLink(uri)
                true
            }
            isSearchDeepLink(uri) -> {
                handleSearchDeepLink(uri)
                true
            }
            else -> false
        }
    }

    /**
     * Check if URI is a settings deep link
     */
    private fun isSettingsDeepLink(uri: Uri): Boolean {
        return when (uri.scheme) {
            SCHEME_STUDYPLAN -> uri.host == HOST_SETTINGS
            SCHEME_HTTPS -> uri.host == HOST_APP && uri.pathSegments?.firstOrNull() == PATH_SETTINGS
            else -> false
        }
    }

    /**
     * Check if URI is a search deep link
     */
    private fun isSearchDeepLink(uri: Uri): Boolean {
        return when (uri.scheme) {
            SCHEME_STUDYPLAN -> uri.host == HOST_SETTINGS && uri.pathSegments?.contains(PATH_SEARCH) == true
            SCHEME_HTTPS -> uri.host == HOST_APP && uri.pathSegments?.contains(PATH_SEARCH) == true
            else -> false
        }
    }

    /**
     * Handle settings deep link
     */
    private fun handleSettingsDeepLink(uri: Uri) {
        val pathSegments = uri.pathSegments ?: return

        when {
            // Direct category navigation: /settings/privacy
            pathSegments.size >= 2 -> {
                val categoryId = pathSegments[1]
                val settingId = pathSegments.getOrNull(2)

                if (settingId != null) {
                    // Navigate to specific setting
                    navigateToSetting(categoryId, settingId, uri.getQueryParameter(PARAM_HIGHLIGHT))
                } else {
                    // Navigate to category
                    navigateToCategory(categoryId)
                }
            }
            // Main settings: /settings
            pathSegments.size == 1 -> {
                navigateToMainSettings()
            }
        }
    }

    /**
     * Handle search deep link
     */
    private fun handleSearchDeepLink(uri: Uri) {
        val query = uri.getQueryParameter(PARAM_QUERY) ?: ""
        navigateToSearch(query)
    }

    /**
     * Navigate to main settings
     */
    fun navigateToMainSettings() {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SettingsActivity::class.java.name)
            .addToBackStack("main_settings")
            .commit()
    }

    /**
     * Navigate to specific category
     */
    fun navigateToCategory(categoryId: String) {
        val fragment = when (categoryId) {
            "privacy" -> PrivacySettingsFragment.newInstance()
            "notifications" -> NotificationSettingsFragment.newInstance()
            "gamification" -> GamificationSettingsFragment.newInstance()
            else -> {
                // Navigate to main settings if category not found
                navigateToMainSettings()
                return
            }
        }

        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("category_$categoryId")
            .commit()
    }

    /**
     * Navigate to specific setting
     */
    fun navigateToSetting(categoryId: String, settingId: String, highlightText: String? = null) {
        // First navigate to category
        navigateToCategory(categoryId)

        // Then scroll/highlight the specific setting
        scope.launch {
            delay(500) // Wait for fragment to load
            highlightSetting(settingId, highlightText)
        }
    }

    /**
     * Navigate to search with optional query
     */
    fun navigateToSearch(query: String = "") {
        val fragment = SettingsSearchFragment.newInstance(query)

        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("settings_search")
            .commit()
    }

    /**
     * Highlight specific setting in current fragment
     */
    private fun highlightSetting(settingId: String, highlightText: String?) {
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)

        when (currentFragment) {
            is BaseSettingsFragment<*> -> {
                // TODO: Implement setting highlighting in base fragment
                highlightSettingInFragment(currentFragment, settingId, highlightText)
            }
        }
    }

    /**
     * Highlight setting in fragment
     */
    private fun highlightSettingInFragment(
        fragment: BaseSettingsFragment<*>,
        settingId: String,
        highlightText: String?
    ) {
        // TODO: Implement specific setting highlighting
        // This would scroll to the setting and highlight it temporarily
    }

    /**
     * Generate deep link URL for category
     */
    fun generateCategoryDeepLink(categoryId: String, useHttps: Boolean = false): String {
        return if (useHttps) {
            "https://$HOST_APP/$PATH_SETTINGS/$categoryId"
        } else {
            "$SCHEME_STUDYPLAN://$HOST_SETTINGS/$categoryId"
        }
    }

    /**
     * Generate deep link URL for specific setting
     */
    fun generateSettingDeepLink(
        categoryId: String,
        settingId: String,
        highlightText: String? = null,
        useHttps: Boolean = false
    ): String {
        val baseUrl = if (useHttps) {
            "https://$HOST_APP/$PATH_SETTINGS/$categoryId/$settingId"
        } else {
            "$SCHEME_STUDYPLAN://$HOST_SETTINGS/$categoryId/$settingId"
        }

        return if (highlightText != null) {
            "$baseUrl?$PARAM_HIGHLIGHT=${Uri.encode(highlightText)}"
        } else {
            baseUrl
        }
    }

    /**
     * Generate search deep link URL
     */
    fun generateSearchDeepLink(query: String, useHttps: Boolean = false): String {
        val baseUrl = if (useHttps) {
            "https://$HOST_APP/$PATH_SETTINGS/$PATH_SEARCH"
        } else {
            "$SCHEME_STUDYPLAN://$HOST_SETTINGS/$PATH_SEARCH"
        }

        return "$baseUrl?$PARAM_QUERY=${Uri.encode(query)}"
    }

    /**
     * Share setting as deep link
     */
    fun shareSettingDeepLink(categoryId: String, settingId: String, settingTitle: String) {
        val deepLink = generateSettingDeepLink(categoryId, settingId, useHttps = true)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "StudyPlan Setting: $settingTitle")
            putExtra(Intent.EXTRA_TEXT, "Check out this setting in StudyPlan: $deepLink")
        }

        val chooser = Intent.createChooser(shareIntent, "Share Setting")
        activity.startActivity(chooser)
    }

    /**
     * Copy deep link to clipboard
     */
    fun copyDeepLinkToClipboard(categoryId: String, settingId: String? = null) {
        val deepLink = if (settingId != null) {
            generateSettingDeepLink(categoryId, settingId, useHttps = true)
        } else {
            generateCategoryDeepLink(categoryId, useHttps = true)
        }

        val clipboardManager = activity.getSystemService(Activity.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Settings Deep Link", deepLink)
        clipboardManager.setPrimaryClip(clip)

        // Show feedback
        val message = if (settingId != null) "Setting link copied" else "Category link copied"
        com.google.android.material.snackbar.Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    /**
     * Validate deep link URL
     */
    fun validateDeepLink(url: String): ValidationResult {
        return try {
            val uri = Uri.parse(url)
            when {
                !isValidScheme(uri.scheme) -> ValidationResult.InvalidScheme
                !isValidHost(uri) -> ValidationResult.InvalidHost
                !isValidPath(uri) -> ValidationResult.InvalidPath
                else -> ValidationResult.Valid
            }
        } catch (e: Exception) {
            ValidationResult.InvalidFormat
        }
    }

    private fun isValidScheme(scheme: String?): Boolean {
        return scheme == SCHEME_STUDYPLAN || scheme == SCHEME_HTTPS
    }

    private fun isValidHost(uri: Uri): Boolean {
        return when (uri.scheme) {
            SCHEME_STUDYPLAN -> uri.host == HOST_SETTINGS
            SCHEME_HTTPS -> uri.host == HOST_APP
            else -> false
        }
    }

    private fun isValidPath(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments ?: return false
        return pathSegments.isNotEmpty() && pathSegments.first() == PATH_SETTINGS
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        object InvalidFormat : ValidationResult()
        object InvalidScheme : ValidationResult()
        object InvalidHost : ValidationResult()
        object InvalidPath : ValidationResult()
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        scope.cancel()
    }
}
