package com.mtlc.studyplan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mtlc.studyplan.R
import com.mtlc.studyplan.animations.AnimationManager
import com.mtlc.studyplan.databinding.FragmentTasksBinding
import com.mtlc.studyplan.navigation.NavigationStateManager
import com.mtlc.studyplan.shared.TaskFilter
import com.mtlc.studyplan.shared.TaskSortOrder
import com.mtlc.studyplan.viewmodels.AnimatedViewModel
import com.mtlc.studyplan.viewmodels.TasksViewModel
import kotlinx.coroutines.launch

class TasksFragment : StateAwareFragment<NavigationStateManager.TasksScreenState>() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels()
    private lateinit var animationManager: AnimationManager

    private var currentFilter: TaskFilter? = null
    private var currentSortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE
    private var selectedTask: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animationManager = AnimationManager(requireContext())
        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupAnimationObservers()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Task adapter setup would go here
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    clearSearch()
                }
                return true
            }
        })
    }

    private fun setupObservers() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            // Update task list with animation
            animationManager.animateDataRefresh(
                binding.recyclerView,
                onRefreshStart = { /* Start refresh indicator */ },
                onRefreshEnd = { /* Hide refresh indicator */ }
            )
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            animationManager.animateLoadingState(binding.rootContainer, isLoading)
        }
    }

    private fun setupAnimationObservers() {
        viewModel.animationTriggers.observe(viewLifecycleOwner) { trigger ->
            when (trigger) {
                is AnimatedViewModel.AnimationTrigger.TaskCompletionStart -> {
                    findTaskViewById(trigger.taskId)?.let { taskView ->
                        animationManager.animateTaskCompletion(taskView) {}
                    }
                }
                is AnimatedViewModel.AnimationTrigger.TaskCompletionSuccess -> {
                    val result = trigger.result
                    val taskView = findTaskViewById(result.task.id) ?: binding.recyclerView
                    animationManager.animateTaskCompletion(taskView, result.gamification) {}
                }
                is AnimatedViewModel.AnimationTrigger.LevelUp -> {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.gamification_level_up_message,
                            trigger.level.currentLevel,
                            trigger.level.levelTitle
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AnimatedViewModel.AnimationTrigger.DataRefreshStart -> {
                    animationManager.animateDataRefresh(
                        binding.recyclerView,
                        onRefreshStart = { viewModel.refreshTasks() },
                        onRefreshEnd = { /* Refresh complete */ }
                    )
                }
                else -> { /* Handle other triggers as needed */ }
            }
        }
    }

    // State management implementation
    override fun getCurrentState(): NavigationStateManager.TasksScreenState {
        return NavigationStateManager.TasksScreenState(
            selectedFilter = currentFilter,
            searchQuery = binding.searchView.query.toString(),
            sortOrder = currentSortOrder,
            scrollPosition = getCurrentScrollPosition(),
            expandedCategories = getExpandedCategories(),
            selectedTaskId = selectedTask,
            showCompletedTasks = getShowCompletedTasks(),
            filterByCategory = getCurrentCategoryFilter(),
            filterByPriority = getCurrentPriorityFilter(),
            groupByDate = getGroupByDate(),
            lastRefreshTime = System.currentTimeMillis(),
            viewMode = getCurrentViewMode()
        )
    }

    override fun applyState(state: NavigationStateManager.TasksScreenState) {
        // Apply filter
        state.selectedFilter?.let { filter ->
            applyTaskFilter(filter)
        }

        // Restore search query
        if (state.searchQuery.isNotEmpty()) {
            binding.searchView.setQuery(state.searchQuery, false)
            performSearch(state.searchQuery)
        }

        // Apply sort order
        applySortOrder(state.sortOrder)

        // Restore view mode
        applyViewMode(state.viewMode)

        // Restore other filters
        state.filterByCategory?.let { applyCategfilter(it) }
        state.filterByPriority?.let { applyPriorityFilter(it) }

        // Show/hide completed tasks
        setShowCompletedTasks(state.showCompletedTasks)

        // Set group by date
        setGroupByDate(state.groupByDate)

        // Restore scroll position (after data loads)
        binding.recyclerView.post {
            restoreScrollPosition(state.scrollPosition)
        }

        // Restore expanded categories
        expandCategories(state.expandedCategories)

        // Restore selected task
        state.selectedTaskId?.let { taskId ->
            highlightTask(taskId)
            selectedTask = taskId
        }
    }

    override fun getRestoredState(): NavigationStateManager.TasksScreenState? {
        return navigationStateManager.restoreTasksState()
    }

    override fun saveState(state: NavigationStateManager.TasksScreenState) {
        navigationStateManager.saveTasksState(state)
    }

    override fun getDefaultState(): NavigationStateManager.TasksScreenState {
        return NavigationStateManager.TasksScreenState()
    }

    // Helper methods for state management
    private fun getCurrentScrollPosition(): Int {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
        return layoutManager?.findFirstVisibleItemPosition() ?: 0
    }

    private fun restoreScrollPosition(position: Int) {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.scrollToPosition(position)
    }

    private fun getExpandedCategories(): Set<String> {
        // Implementation to get currently expanded categories
        return emptySet()
    }

    private fun expandCategories(categories: Set<String>) {
        // Implementation to expand specific categories
        categories.forEach { category ->
            // Expand category with animation
        }
    }

    private fun applyTaskFilter(filter: TaskFilter) {
        currentFilter = filter
        viewModel.applyFilter(filter)
    }

    private fun applySortOrder(sortOrder: TaskSortOrder) {
        currentSortOrder = sortOrder
        viewModel.applySortOrder(sortOrder)
    }

    private fun applyViewMode(viewMode: NavigationStateManager.TaskViewMode) {
        when (viewMode) {
            NavigationStateManager.TaskViewMode.LIST -> {
                // Switch to list view
            }
            NavigationStateManager.TaskViewMode.GRID -> {
                // Switch to grid view
            }
            NavigationStateManager.TaskViewMode.CALENDAR -> {
                // Switch to calendar view
            }
        }
    }

    private fun performSearch(query: String) {
        viewModel.searchTasks(query)
    }

    private fun clearSearch() {
        binding.searchView.setQuery("", false)
        viewModel.clearSearch()
    }

    private fun highlightTask(taskId: String) {
        // Find and highlight specific task
        selectedTask = taskId
    }

    private fun findTaskViewById(taskId: String): View? {
        // Implementation to find task view by ID
        return null
    }

    private fun getShowCompletedTasks(): Boolean {
        // Implementation to get current show completed tasks setting
        return false
    }

    private fun setShowCompletedTasks(show: Boolean) {
        // Implementation to show/hide completed tasks
    }

    private fun getCurrentCategoryFilter(): String? {
        // Implementation to get current category filter
        return null
    }

    private fun applyCategfilter(category: String) {
        // Implementation to apply category filter
    }

    private fun getCurrentPriorityFilter(): String? {
        // Implementation to get current priority filter
        return null
    }

    private fun applyPriorityFilter(priority: String) {
        // Implementation to apply priority filter
    }

    private fun getGroupByDate(): Boolean {
        // Implementation to get current group by date setting
        return true
    }

    private fun setGroupByDate(groupByDate: Boolean) {
        // Implementation to set group by date
    }

    private fun getCurrentViewMode(): NavigationStateManager.TaskViewMode {
        // Implementation to get current view mode
        return NavigationStateManager.TaskViewMode.LIST
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
