package com.mtlc.studyplan.settings.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.ui.views.ActionSettingView
import com.mtlc.studyplan.settings.ui.views.SelectionSettingView
import com.mtlc.studyplan.settings.ui.views.TimePickerSettingView
import com.mtlc.studyplan.settings.ui.views.ToggleSettingView

/**
 * Adapter for notification settings with time picker support
 */
class NotificationSettingsAdapter(
    private val onSettingChanged: (SettingItem, Any?) -> Unit
) : BaseSettingsAdapter() {

    companion object {
        private const val TYPE_TOGGLE = 1
        private const val TYPE_SELECTION = 2
        private const val TYPE_ACTION = 3
        private const val TYPE_TIME = 4
    }

    override fun getSettingViewType(setting: SettingItem): Int {
        return when (setting) {
            is ToggleSetting -> TYPE_TOGGLE
            is SelectionSetting<*> -> TYPE_SELECTION
            is ActionSetting -> TYPE_ACTION
            is TimeSetting -> TYPE_TIME
            else -> TYPE_TOGGLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
            TYPE_TIME -> {
                val view = TimePickerSettingView(parent.context)
                TimeViewHolder(view)
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
            is TimeViewHolder -> {
                holder.bind(setting as TimeSetting)
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

                // Show loading briefly for test actions
                actionView.showLoading(true)
                actionView.postDelayed({
                    actionView.showLoading(false)
                    actionView.showSuccess("Sent!")
                }, 1000)
            }
        }
    }

    inner class TimeViewHolder(
        private val timeView: TimePickerSettingView
    ) : RecyclerView.ViewHolder(timeView) {

        fun bind(setting: TimeSetting) {
            timeView.bind(setting) { newTime ->
                onSettingChanged(setting, newTime)
                timeView.highlightChange()
            }
        }
    }
}