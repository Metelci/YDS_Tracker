package com.mtlc.studyplan.settings.ui

import com.mtlc.studyplan.core.error.AppError

interface BaseSettingsUiState {
    val isLoading: Boolean
    val isError: Boolean
    val isSuccess: Boolean
    val error: AppError?
}
