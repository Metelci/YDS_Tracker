package com.mtlc.studyplan.social.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.ViewModelFactory
import com.mtlc.studyplan.data.social.*
import com.mtlc.studyplan.core.error.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI State for Social features with error handling
 */
data class SocialUiState(
    val profile: SocialProfile? = null,
    val ranks: List<RankEntry> = emptyList(),
    val groups: List<Group> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val awards: List<Award> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val loadingStates: Map<String, Boolean> = emptyMap()
) {
    val isSuccess: Boolean get() = profile != null && error == null && !isLoading
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = profile == null && !isLoading && error == null

    fun isOperationLoading(operation: String): Boolean = loadingStates[operation] == true
}

/**
 * Enhanced Social ViewModel with comprehensive error handling
 */
class SocialViewModel(
    private val repository: SocialRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val _globalError = MutableStateFlow<AppError?>(null)
    val globalError: StateFlow<AppError?> = _globalError.asStateFlow()

    init {
        loadSocialData()
        observeDataChanges()

        // Observe global errors
        viewModelScope.launch {
            errorHandler.globalErrors.collect { error ->
                _globalError.value = error
            }
        }
    }

    private fun loadSocialData() {
        viewModelScope.launch {
            setLoadingState(true)

            executeWithErrorHandling(
                operation = {
                    // Load all social data concurrently
                    val profile = repository.profile.first()
                    val ranks = repository.ranks.first()
                    val groups = repository.groups.first()
                    val friends = repository.friends.first()
                    val awards = repository.awards.first()

                    SocialData(profile, ranks, groups, friends, awards)
                },
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        profile = data.profile,
                        ranks = data.ranks,
                        groups = data.groups,
                        friends = data.friends,
                        awards = data.awards,
                        isLoading = false,
                        error = null
                    )
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            )
        }
    }

    private fun observeDataChanges() {
        viewModelScope.launch {
            try {
                // Combine all repository flows into a single efficient collector
                kotlinx.coroutines.flow.combine(
                    repository.profile,
                    repository.ranks,
                    repository.groups,
                    repository.friends,
                    repository.awards
                ) { profile, ranks, groups, friends, awards ->
                    _uiState.value.copy(
                        profile = profile,
                        ranks = ranks,
                        groups = groups,
                        friends = friends,
                        awards = awards
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                handleError(ErrorMapper.mapThrowable(e))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            setRefreshingState(true)

            executeWithErrorHandling(
                operation = {
                    // Force refresh from repository
                    // If repository supports refresh, call it here
                    // For now, reload data
                    loadSocialData()
                },
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error
                    )
                }
            )
        }
    }

    fun toggleGroupMembership(groupId: String) {
        viewModelScope.launch {
            setOperationLoading("group_$groupId", true)

            executeWithErrorHandling(
                operation = {
                    validateGroupOperation(groupId)
                    repository.toggleGroupMembership(groupId)
                },
                onSuccess = {
                    setOperationLoading("group_$groupId", false)
                },
                onError = { error ->
                    setOperationLoading("group_$groupId", false)
                    handleError(error)
                }
            )
        }
    }

    fun shareGroup(groupId: String) {
        viewModelScope.launch {
            setOperationLoading("share_$groupId", true)

            executeWithErrorHandling(
                operation = {
                    validateGroupOperation(groupId)
                    repository.shareGroup(groupId)
                },
                onSuccess = {
                    setOperationLoading("share_$groupId", false)
                },
                onError = { error ->
                    setOperationLoading("share_$groupId", false)
                    handleError(error)
                }
            )
        }
    }

    fun selectAvatar(avatarId: String) {
        viewModelScope.launch {
            setOperationLoading("avatar", true)

            executeWithErrorHandling(
                operation = {
                    validateAvatarSelection(avatarId)
                    repository.selectAvatar(avatarId)
                },
                onSuccess = {
                    setOperationLoading("avatar", false)
                },
                onError = { error ->
                    setOperationLoading("avatar", false)
                    handleError(error)
                }
            )
        }
    }

    fun updateWeeklyGoal(hours: Int) {
        viewModelScope.launch {
            setOperationLoading("weekly_goal", true)

            executeWithErrorHandling(
                operation = {
                    validateWeeklyGoal(hours)
                    repository.updateWeeklyGoal(hours)
                },
                onSuccess = {
                    setOperationLoading("weekly_goal", false)
                },
                onError = { error ->
                    setOperationLoading("weekly_goal", false)
                    handleError(error)
                }
            )
        }
    }

    fun clearError() {
        _globalError.value = null
        _uiState.value = _uiState.value.copy(error = null)
        errorHandler.clearGlobalError()
    }

    fun retry() {
        clearError()
        loadSocialData()
    }

    private fun setLoadingState(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading, error = null)
    }

    private fun setRefreshingState(isRefreshing: Boolean) {
        _uiState.value = _uiState.value.copy(isRefreshing = isRefreshing)
    }

    private fun setOperationLoading(operation: String, isLoading: Boolean) {
        val currentLoadingStates = _uiState.value.loadingStates.toMutableMap()
        if (isLoading) {
            currentLoadingStates[operation] = true
        } else {
            currentLoadingStates.remove(operation)
        }
        _uiState.value = _uiState.value.copy(loadingStates = currentLoadingStates)
    }

    private suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (AppError) -> Unit = { error -> handleError(error) }
    ) {
        val result = errorHandler.handleOperation(
            operation = operation,
            additionalInfo = mapOf(
                "viewModel" to "SocialViewModel",
                "timestamp" to System.currentTimeMillis()
            )
        )

        result.fold(
            onSuccess = onSuccess,
            onFailure = { exception ->
                val error = ErrorMapper.mapThrowable(exception)
                onError(error)
            }
        )
    }

    private fun handleError(error: AppError) {
        _uiState.value = _uiState.value.copy(error = error)
    }

    // Validation methods
    private fun validateGroupOperation(groupId: String) {
        if (groupId.isBlank()) {
            throw AppError.ValidationError.RequiredFieldEmpty
        }

        val group = _uiState.value.groups.find { it.id == groupId }
            ?: throw AppError.DataError.NotFound

        // Add additional business logic validation
    }

    private fun validateAvatarSelection(avatarId: String) {
        if (avatarId.isBlank()) {
            throw AppError.ValidationError.RequiredFieldEmpty
        }

        val profile = _uiState.value.profile
        val isValidAvatar = profile?.availableAvatars?.any { it.id == avatarId } == true

        if (!isValidAvatar) {
            throw AppError.ValidationError.InvalidInput
        }
    }

    private fun validateWeeklyGoal(hours: Int) {
        val profile = _uiState.value.profile
        val goalRange = profile?.goalRange

        if (goalRange != null && hours !in goalRange) {
            throw AppError.ValidationError.OutOfRange(
                field = "weekly goal",
                min = goalRange.first,
                max = goalRange.last
            )
        }

        if (hours < 0) {
            throw AppError.ValidationError.OutOfRange(
                field = "weekly goal",
                min = 0,
                max = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        errorHandler.clearGlobalError()
    }

    /**
     * Data class to hold all social data for concurrent loading
     */
    private data class SocialData(
        val profile: SocialProfile,
        val ranks: List<RankEntry>,
        val groups: List<Group>,
        val friends: List<Friend>,
        val awards: List<Award>
    )
}

/**
 * Factory for SocialViewModel
 */
class SocialViewModelFactory(
    private val repository: SocialRepository,
    private val context: Context
) : ViewModelFactory<SocialViewModel>({ SocialViewModel(repository, context) })