package com.mtlc.studyplan.settings.models

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a settings category in the main settings grid
 */
data class SettingsCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

/**
 * Base class for different types of setting items
 */
sealed class SettingItem {
    abstract val id: String
    abstract val title: String
    abstract val description: String?

    /**
     * Toggle switch setting item
     */
    data class Toggle(
        override val id: String,
        override val title: String,
        override val description: String? = null,
        val isEnabled: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingItem()

    /**
     * Clickable setting item that navigates or shows dialog
     */
    data class Clickable(
        override val id: String,
        override val title: String,
        override val description: String? = null,
        val value: String? = null,
        val onClick: () -> Unit
    ) : SettingItem()

    /**
     * Setting item with multiple choice options
     */
    data class Selection(
        override val id: String,
        override val title: String,
        override val description: String? = null,
        val selectedValue: String,
        val options: List<SelectionOption>,
        val onSelectionChange: (String) -> Unit
    ) : SettingItem()

    /**
     * Divider item for grouping settings
     */
    data class Divider(
        override val id: String,
        override val title: String = "",
        override val description: String? = null
    ) : SettingItem()

    /**
     * Header item for grouping settings
     */
    data class Header(
        override val id: String,
        override val title: String,
        override val description: String? = null
    ) : SettingItem()
}

/**
 * Represents an option in a selection setting
 */
data class SelectionOption(
    val value: String,
    val displayName: String,
    val description: String? = null
)

/**
 * UI state for settings screens
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val items: List<SettingItem> = emptyList(),
    val error: com.mtlc.studyplan.core.error.AppError? = null,
    val isRefreshing: Boolean = false
) {
    val isSuccess: Boolean get() = items.isNotEmpty() && error == null && !isLoading
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = items.isEmpty() && !isLoading && error == null
}