package com.mtlc.studyplan.settings.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ActivitySettingsBinding
import com.mtlc.studyplan.settings.data.SettingsCategory
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.viewmodel.SettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.SettingsViewModelFactory
import com.mtlc.studyplan.ui.components.ErrorCard
import com.mtlc.studyplan.ui.components.LoadingErrorState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main Settings Activity with Material Design 3 styling and comprehensive features
 */
class SettingsActivity : AppCompatActivity(), SettingsCategoryAdapter.OnCategoryClickListener {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var categoryAdapter: SettingsCategoryAdapter
    private lateinit var repository: SettingsRepository

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(repository, this)
    }

    private var searchView: SearchView? = null
    private var originalCategories: List<SettingsCategory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable dynamic colors for Material You
        DynamicColors.applyToActivityIfAvailable(this)

        // Set up shared element transitions
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementsUseOverlay = false

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = SettingsRepository(this)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        setupBottomActions()
        handleWindowInsets()

        // Load initial data
        viewModel.refresh()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.settings_title)
        }

        // Set up search functionality
        setupSearch()

        // Set up error handling
        setupErrorHandling()
    }

    private fun setupRecyclerView() {
        val spanCount = getSpanCount()
        categoryAdapter = SettingsCategoryAdapter(this, spanCount)

        binding.categoriesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@SettingsActivity, spanCount).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
            adapter = categoryAdapter

            // Add item animations
            itemAnimator?.apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
        }
    }

    private fun getSpanCount(): Int {
        return if (isTablet()) {
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 4 else 3
        } else {
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        }
    }

    private fun isTablet(): Boolean = this.isTablet()

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }
        })

        binding.searchView.setOnCloseListener {
            resetSearch()
            false
        }
    }

    private fun performSearch(query: String?) {
        if (query.isNullOrBlank()) {
            resetSearch()
            return
        }

        val filteredCategories = originalCategories.filter { category ->
            category.title.contains(query, ignoreCase = true) ||
            category.description.contains(query, ignoreCase = true)
        }

        categoryAdapter.updateCategories(filteredCategories, query)

        // Show/hide empty state
        if (filteredCategories.isEmpty()) {
            showEmptySearchState()
        } else {
            hideEmptySearchState()
        }
    }

    private fun resetSearch() {
        categoryAdapter.updateCategories(originalCategories)
        hideEmptySearchState()
    }

    private fun showEmptySearchState() {
        binding.emptySearchState.visibility = View.VISIBLE
        binding.categoriesRecyclerView.visibility = View.GONE
    }

    private fun hideEmptySearchState() {
        binding.emptySearchState.visibility = View.GONE
        binding.categoriesRecyclerView.visibility = View.VISIBLE
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                when {
                    uiState.isLoading -> showLoading()
                    uiState.isError -> showError(uiState.error!!)
                    uiState.isSuccess -> showSuccess(uiState.categories, uiState.appVersion)
                }
            }
        }

        // No events flow; UI reacts to uiState only
    }

    private fun showLoading() {
        binding.apply {
            progressIndicator.visibility = View.VISIBLE
            categoriesRecyclerView.visibility = View.GONE
            errorContainer.visibility = View.GONE
        }
    }

    private fun showError(error: com.mtlc.studyplan.core.error.AppError) {
        binding.apply {
            progressIndicator.visibility = View.GONE
            categoriesRecyclerView.visibility = View.GONE
            errorContainer.visibility = View.VISIBLE
        }

        // Show error via Compose inside a ComposeView
        binding.errorContainer.removeAllViews()
        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ErrorCard(
                    error = error,
                    onRetry = { viewModel.retry() },
                    onDismiss = { binding.errorContainer.visibility = View.GONE }
                )
            }
        }
        binding.errorContainer.addView(composeView)
    }

    private fun showSuccess(categories: List<SettingsCategory>, appVersion: String) {
        binding.apply {
            progressIndicator.visibility = View.GONE
            errorContainer.visibility = View.GONE
            categoriesRecyclerView.visibility = View.VISIBLE

            // Update app version
            appVersionText.text = getString(R.string.app_version_format, appVersion)
        }

        originalCategories = categories
        categoryAdapter.updateCategories(categories)

        // Animate in the content
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_up)
        binding.categoriesRecyclerView.startAnimation(slideIn)
    }

    private fun setupErrorHandling() {
        // No-op: global error channel not implemented; UI reacts via uiState
    }

    private fun showErrorDialog(error: com.mtlc.studyplan.core.error.AppError) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_error_title)
            .setMessage(error.userMessage)
            .setPositiveButton(R.string.settings_ok) { dialog, _ ->
                dialog.dismiss()
                viewModel.clearError()
            }
            .setNegativeButton(R.string.retry) { dialog, _ ->
                dialog.dismiss()
                viewModel.retry()
            }
            .show()
    }

    private fun setupBottomActions() {
        // Reset notifications button
        binding.resetNotificationsButton.setOnClickListener {
            showSnackbar("Not implemented")
        }

        // Reset progress button (danger zone)
        binding.resetProgressButton.setOnClickListener {
            showResetProgressDialog()
        }

        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.appVersionText.text = getString(
                R.string.app_version_format,
                "${packageInfo.versionName} (${packageInfo.longVersionCode})"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            binding.appVersionText.text = getString(R.string.app_version_unknown)
        }
    }

    private fun showResetProgressDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_progress_title)
            .setMessage(R.string.reset_progress_warning)
            .setIcon(R.drawable.ic_error)
            .setPositiveButton(R.string.reset_progress_confirm) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onCategoryClick(category: SettingsCategory, sharedElement: View) {
        navigateToCategory(category, sharedElement)
    }

    private fun navigateToCategory(category: SettingsCategory, sharedElement: View? = null) {
        val fragment = SettingsDetailFragment.newInstance(category.id)

        supportFragmentManager.commit {
            setReorderingAllowed(true)

            if (sharedElement != null) {
                addSharedElement(sharedElement, "settings_category_${category.id}")
            }

            replace(R.id.fragment_container, fragment)
            addToBackStack("category_${category.id}")
        }

        // Show fragment container and hide main content
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.mainContent.visibility = View.GONE

        // Update toolbar
        supportActionBar?.title = category.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        searchView?.apply {
            queryHint = getString(R.string.search_settings_hint)
            maxWidth = Integer.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    performSearch(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    performSearch(newText)
                    return true
                }
            })
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_export_settings -> { showSnackbar("Not implemented"); true }
            R.id.action_import_settings -> { showSnackbar("Not implemented"); true }
            R.id.action_reset_all -> {
                showResetAllDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_all_settings_title)
            .setMessage(R.string.reset_all_settings_warning)
            .setIcon(R.drawable.ic_error)
            .setPositiveButton(R.string.reset_all_confirm) { dialog, _ ->
                viewModel.resetAllSettings()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onBackPressed() {
        when {
            searchView?.isIconified == false -> {
                searchView?.isIconified = true
                resetSearch()
            }
            supportFragmentManager.backStackEntryCount > 0 -> {
                supportFragmentManager.popBackStack()

                // Show main content and hide fragment container
                binding.fragmentContainer.visibility = View.GONE
                binding.mainContent.visibility = View.VISIBLE

                // Restore main toolbar
                supportActionBar?.title = getString(R.string.settings_title)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.bottom
            )

            // Apply insets to toolbar
            binding.appBarLayout.setPadding(
                binding.appBarLayout.paddingLeft,
                insets.top,
                binding.appBarLayout.paddingRight,
                binding.appBarLayout.paddingBottom
            )

            windowInsets
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { viewModel.retry() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.dispose()
    }
}

/**
 * Extension function to check if device is a tablet
 */
private fun android.content.Context.isTablet(): Boolean {
    val xlarge = resources.configuration.screenLayout and
                android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK >=
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    val large = resources.configuration.screenLayout and
               android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK ==
               android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    return xlarge || large
}
