package com.mtlc.studyplan.settings.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.databinding.ViewActionSettingBinding
import com.mtlc.studyplan.databinding.ViewSelectionSettingBinding
import com.mtlc.studyplan.databinding.ViewToggleSettingBinding
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.ui.views.ActionSettingView
import com.mtlc.studyplan.settings.ui.views.SelectionSettingView
import com.mtlc.studyplan.settings.ui.views.ToggleSettingView

/**
 * Adapter for privacy settings with custom view support
 */
class PrivacySettingsAdapter(
    private val onSettingChanged: (SettingItem, Any?) -> Unit
) : BaseSettingsAdapter() {

    companion object {
        private const val TYPE_TOGGLE = 1
        private const val TYPE_SELECTION = 2
        private const val TYPE_ACTION = 3
    }

    override fun getSettingViewType(setting: SettingItem): Int {
        return when (setting) {
            is ToggleSetting -> TYPE_TOGGLE
            is SelectionSetting<*> -> TYPE_SELECTION
            is ActionSetting -> TYPE_ACTION
            else -> TYPE_TOGGLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_TOGGLE -> {
                val view = ToggleSettingView(parent.context)
                ToggleViewHolder(view)
            }
            TYPE_SELECTION -> {
                val view = SelectionSettingView(parent.context)
                SelectionViewHolder(view)
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
            is SelectionViewHolder -> {
                holder.bind(setting as SelectionSetting<*>)
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
                toggleView.highlightChange()
            }
        }
    }

    inner class SelectionViewHolder(
        private val selectionView: SelectionSettingView
    ) : RecyclerView.ViewHolder(selectionView) {

        fun bind(setting: SelectionSetting<*>) {
            selectionView.bind(setting) { newValue ->
                onSettingChanged(setting, newValue)
                selectionView.highlightChange()
            }
        }
    }

    inner class ActionViewHolder(
        private val actionView: ActionSettingView
    ) : RecyclerView.ViewHolder(actionView) {

        fun bind(setting: ActionSetting) {
            actionView.bind(setting) {
                onSettingChanged(setting, Unit)

                // Show loading state briefly
                actionView.showLoading(true)

                // Simulate processing delay
                actionView.postDelayed({
                    actionView.showLoading(false)

                    // Show appropriate feedback based on action type
                    if (setting.actionType == com.mtlc.studyplan.settings.data.SettingItem.ActionSetting.ActionType.DESTRUCTIVE) {
                        actionView.showSuccess("Completed")
                    } else {
                        actionView.showSuccess()
                    }
                }, 1500)
            }
        }
    }
}


