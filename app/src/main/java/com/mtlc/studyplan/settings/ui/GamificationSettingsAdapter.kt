package com.mtlc.studyplan.settings.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.settings.data.ActionSetting
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.ToggleSetting
import com.mtlc.studyplan.settings.ui.views.ActionSettingView
import com.mtlc.studyplan.settings.ui.views.ToggleSettingView

/**
 * Adapter for gamification settings with toggle and action support
 */
class GamificationSettingsAdapter(
    private val onSettingChanged: (SettingItem, Any?) -> Unit
) : BaseSettingsAdapter() {

    companion object {
        private const val TYPE_TOGGLE = 1
        private const val TYPE_ACTION = 2
    }

    override fun getSettingViewType(setting: SettingItem): Int {
        return when (setting) {
            is ToggleSetting -> TYPE_TOGGLE
            is ActionSetting -> TYPE_ACTION
            else -> TYPE_TOGGLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TOGGLE -> {
                val view = ToggleSettingView(parent.context)
                ToggleViewHolder(view)
            }
            TYPE_ACTION -> {
                val view = ActionSettingView(parent.context)
                ActionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val setting = settings[position]

        when (holder) {
            is ToggleViewHolder -> {
                holder.bind(setting as ToggleSetting)
            }
            is ActionViewHolder -> {
                holder.bind(setting as ActionSetting)
            }
        }
    }

    inner class ToggleViewHolder(
        private val toggleView: ToggleSettingView
    ) : RecyclerView.ViewHolder(toggleView) {

        fun bind(setting: ToggleSetting) {
            toggleView.bind(setting) { newValue ->
                onSettingChanged(setting, newValue)

                // Show enhanced feedback for gamification toggles
                when (setting.id) {
                    "streak_tracking", "points_rewards" -> {
                        // Add celebration effect if celebration effects are enabled
                        toggleView.highlightChange()

                        // Additional visual feedback for important settings
                        if (newValue) {
                            showCelebrationFeedback(setting.id)
                        }
                    }
                    else -> toggleView.highlightChange()
                }
            }
        }

        private fun showCelebrationFeedback(settingId: String) {
            // Add confetti or other celebration effects here if celebration effects are enabled
            // This would integrate with the app's celebration system
            when (settingId) {
                "streak_tracking" -> {
                    // Animate the toggle with a special effect
                    toggleView.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(200)
                        .withEndAction {
                            toggleView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                }
                "points_rewards" -> {
                    // Similar celebration effect for points
                    toggleView.animate()
                        .rotationBy(360f)
                        .setDuration(500)
                        .start()
                }
            }
        }
    }

    inner class ActionViewHolder(
        private val actionView: ActionSettingView
    ) : RecyclerView.ViewHolder(actionView) {

        fun bind(setting: ActionSetting) {
            actionView.bind(setting) {
                onSettingChanged(setting, Unit)

                // Show appropriate feedback based on action type
                when (setting.actionType.name) {
                    "DESTRUCTIVE" -> {
                        // Show loading briefly for destructive actions
                        actionView.showLoading(true)
                        actionView.postDelayed({
                            actionView.showLoading(false)
                            actionView.showSuccess("Reset!")
                        }, 1500)
                    }
                    else -> {
                        actionView.showSuccess("Done!")
                    }
                }
            }
        }
    }
}


