package com.mtlc.studyplan.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.materialswitch.MaterialSwitch
import com.mtlc.studyplan.R
import com.mtlc.studyplan.accessibility.AccessibilityEnhancementManager
import com.mtlc.studyplan.accessibility.AccessibilityUtils
import com.mtlc.studyplan.databinding.ComponentAdvancedToggleBinding
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.ui.animations.SettingsAnimations
import kotlinx.coroutines.*

/**
 * Advanced toggle component with dependency management and validation
 */
class AdvancedToggleComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentAdvancedToggleBinding
    private val accessibilityManager = AccessibilityEnhancementManager(context)
    private var animationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Configuration properties
    private var settingItem: SettingItem? = null
    private var onValueChanged: ((Boolean) -> Unit)? = null
    private var dependencyValidator: DependencyValidator? = null
    private val dependentToggles = mutableListOf<AdvancedToggleComponent>()
    private val dependencies = mutableListOf<ToggleDependency>()

    // State properties
    private var isComponentEnabled: Boolean = true
        set(value) {
            field = value
            updateEnabledState()
        }

    private var isChecked: Boolean = false
        set(value) {
            field = value
            updateCheckedState()
        }

    private var validationState: ValidationState = ValidationState.VALID
        set(value) {
            field = value
            updateValidationState()
        }

    init {
        binding = ComponentAdvancedToggleBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

        setupComponent()
        setupAccessibility()
        setupAnimations()

        // Parse custom attributes if provided
        attrs?.let { parseAttributes(it) }
    }

    /**
     * Configure the toggle with setting data
     */
    fun configure(
        setting: SettingItem,
        initialValue: Boolean = false,
        onValueChanged: (Boolean) -> Unit
    ) {
        this.settingItem = setting
        this.onValueChanged = onValueChanged
        this.isChecked = initialValue

        // Update UI with setting information
        binding.toggleTitle.text = setting.title
        binding.toggleDescription.text = setting.description
        // Update accessibility descriptions
        updateAccessibilityDescriptions()
    }

    /**
     * Add dependency relationship
     */
    fun addDependency(dependency: ToggleDependency) {
        dependencies.add(dependency)
        dependency.parentToggle.addDependent(this)
        updateDependencyState()
    }

    /**
     * Remove dependency relationship
     */
    fun removeDependency(dependency: ToggleDependency) {
        dependencies.remove(dependency)
        dependency.parentToggle.removeDependent(this)
        updateDependencyState()
    }

    /**
     * Add dependent toggle
     */
    private fun addDependent(dependent: AdvancedToggleComponent) {
        if (!dependentToggles.contains(dependent)) {
            dependentToggles.add(dependent)
        }
    }

    /**
     * Remove dependent toggle
     */
    private fun removeDependent(dependent: AdvancedToggleComponent) {
        dependentToggles.remove(dependent)
    }

    /**
     * Set custom dependency validator
     */
    fun setDependencyValidator(validator: DependencyValidator) {
        this.dependencyValidator = validator
    }

    /**
     * Programmatically set toggle state
     */
    fun setToggleState(checked: Boolean, animate: Boolean = true) {
        if (animate) {
            animationScope.launch {
                SettingsAnimations.animateToggleSwitch(
                    binding.toggleSwitch,
                    checked,
                    showValueChange = true
                )
                handleValueChange(checked)
            }
        } else {
            binding.toggleSwitch.isChecked = checked
            handleValueChange(checked)
        }
    }

    /**
     * Get current toggle state
     */
    fun getToggleState(): Boolean = isChecked

    /**
     * Enable or disable the toggle
     */
    fun setToggleEnabled(enabled: Boolean) {
        isComponentEnabled = enabled
    }

    /**
     * Check if toggle is enabled
     */
    fun isToggleEnabled(): Boolean = isComponentEnabled

    /**
     * Setup component interactions
     */
    private fun setupComponent() {
        binding.toggleSwitch.setOnCheckedChangeListener { _, checked ->
            handleValueChange(checked)
        }

        binding.root.setOnClickListener {
            if (isComponentEnabled && validationState == ValidationState.VALID) {
                setToggleState(!isChecked, animate = true)
            }
        }
    }

    /**
     * Setup accessibility enhancements
     */
    private fun setupAccessibility() {
        AccessibilityUtils.applyAccessibilityMinimumSizes(this, accessibilityManager)
        AccessibilityUtils.applyHighContrastColors(this, accessibilityManager)
    }

    /**
     * Setup animation system
     */
    private fun setupAnimations() {
        // Setup interaction animations
        binding.root.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    SettingsAnimations.animateSettingItemClick(view)
                }
            }
            false
        }
    }

    /**
     * Parse custom attributes from XML
     */
    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AdvancedToggleComponent)

        try {
            val title = typedArray.getString(R.styleable.AdvancedToggleComponent_toggleTitle)
            val description = typedArray.getString(R.styleable.AdvancedToggleComponent_toggleDescription)
            val enabled = typedArray.getBoolean(R.styleable.AdvancedToggleComponent_toggleEnabled, true)
            val checked = typedArray.getBoolean(R.styleable.AdvancedToggleComponent_toggleChecked, false)

            title?.let { binding.toggleTitle.text = it }
            description?.let { binding.toggleDescription.text = it }
            isEnabled = enabled
            isChecked = checked

        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Handle toggle value changes
     */
    private fun handleValueChange(newValue: Boolean) {
        val oldValue = isChecked
        isChecked = newValue

        // Validate change
        val validation = validateChange(newValue)
        if (validation != ValidationState.VALID) {
            // Revert change and show error
            isChecked = oldValue
            binding.toggleSwitch.isChecked = oldValue
            validationState = validation
            return
        }

        // Update dependent toggles
        updateDependents()

        // Announce change for accessibility
        announceValueChange(newValue)

        // Notify listener
        onValueChanged?.invoke(newValue)
    }

    /**
     * Validate setting change against constraints
     */
    private fun validateChange(newValue: Boolean): ValidationState {
        // Check custom validator first
        dependencyValidator?.let { validator ->
            if (!validator.validateChange(this, newValue)) {
                return ValidationState.DEPENDENCY_VIOLATION
            }
        }

        // Check dependencies
        for (dependency in dependencies) {
            if (!dependency.isConditionMet()) {
                return ValidationState.DEPENDENCY_VIOLATION
            }
        }
        return ValidationState.VALID
    }
    /**
     * Update dependent toggles based on current state
     */
    private fun updateDependents() {
        dependentToggles.forEach { dependent ->
            dependent.updateDependencyState()
        }
    }

    /**
     * Update state based on dependencies
     */
    private fun updateDependencyState() {
        var shouldEnable = true
        val failedDependencies = mutableListOf<String>()

        for (dependency in dependencies) {
            if (!dependency.isConditionMet()) {
                shouldEnable = false
                failedDependencies.add(dependency.getDependencyDescription())
            }
        }

        // Update enabled state
        setToggleEnabled(shouldEnable)

        // Update validation message if needed
        if (!shouldEnable) {
            showDependencyMessage(failedDependencies)
        } else {
            hideDependencyMessage()
        }
    }

    /**
     * Show dependency requirement message
     */
    private fun showDependencyMessage(failedDependencies: List<String>) {
        val message = when (failedDependencies.size) {
            1 -> "Requires: ${failedDependencies.first()}"
            else -> "Requires: ${failedDependencies.joinToString(", ")}"
        }

        binding.dependencyMessage.text = message
        binding.dependencyMessage.isVisible = true

        // Animate message appearance
        SettingsAnimations.animateErrorState(binding.dependencyMessage)
    }

    /**
     * Hide dependency requirement message
     */
    private fun hideDependencyMessage() {
        binding.dependencyMessage.isVisible = false
    }

    /**
     * Update enabled state visually
     */
    private fun updateEnabledState() {
        binding.toggleSwitch.isEnabled = isComponentEnabled
        binding.toggleTitle.alpha = if (isComponentEnabled) 1.0f else 0.5f
        binding.toggleDescription.alpha = if (isComponentEnabled) 1.0f else 0.5f

        // Update accessibility
        updateAccessibilityDescriptions()
    }

    /**
     * Update checked state visually
     */
    private fun updateCheckedState() {
        binding.toggleSwitch.isChecked = isChecked
    }

    /**
     * Update validation state visually
     */
    private fun updateValidationState() {
        when (validationState) {
            ValidationState.VALID -> {
                binding.errorIcon.isVisible = false
                binding.warningMessage.isVisible = false
            }
            ValidationState.DEPENDENCY_VIOLATION -> {
                binding.errorIcon.isVisible = true
                binding.warningMessage.text = "Dependency requirements not met"
                binding.warningMessage.isVisible = true
                SettingsAnimations.animateErrorState(binding.errorIcon)
            }
            ValidationState.CONSTRAINT_VIOLATION -> {
                binding.errorIcon.isVisible = true
                binding.warningMessage.text = "Setting constraints violated"
                binding.warningMessage.isVisible = true
                SettingsAnimations.animateErrorState(binding.errorIcon)
            }
        }
    }

    /**
     * Update accessibility descriptions
     */
    private fun updateAccessibilityDescriptions() {
        val title = binding.toggleTitle.text.toString()
        val description = binding.toggleDescription.text.toString()
        val state = if (isChecked) "enabled" else "disabled"

        AccessibilityUtils.enhanceSwitchAccessibility(
            binding.toggleSwitch,
            title,
            description,
            "enabled",
            "disabled"
        )

        // Set content description for the entire component
        contentDescription = if (isComponentEnabled) {
            "$title, $description, $state"
        } else {
            "$title, $description, $state, disabled due to dependencies"
        }
    }

    /**
     * Announce value change for accessibility
     */
    private fun announceValueChange(newValue: Boolean) {
        if (accessibilityManager.isTalkBackEnabled()) {
            val title = binding.toggleTitle.text.toString()
            val state = if (newValue) "enabled" else "disabled"
            val announcement = "$title $state"
            AccessibilityUtils.announceForAccessibility(this, announcement)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationScope.cancel()
        SettingsAnimations.cleanupAnimations(this)
    }

    /**
     * Validation states for the toggle
     */
    enum class ValidationState {
        VALID,
        DEPENDENCY_VIOLATION,
        CONSTRAINT_VIOLATION
    }

    /**
     * Toggle dependency definition
     */
    data class ToggleDependency(
        val parentToggle: AdvancedToggleComponent,
        val condition: DependencyCondition,
        val description: String
    ) {
        fun isConditionMet(): Boolean = condition.isMet(parentToggle)
        fun getDependencyDescription(): String = description
    }

    /**
     * Dependency condition interface
     */
    interface DependencyCondition {
        fun isMet(parentToggle: AdvancedToggleComponent): Boolean
    }

    /**
     * Common dependency conditions
     */
    object DependencyConditions {
        fun mustBeEnabled() = object : DependencyCondition {
            override fun isMet(parentToggle: AdvancedToggleComponent): Boolean {
                return parentToggle.getToggleState()
            }
        }

        fun mustBeDisabled() = object : DependencyCondition {
            override fun isMet(parentToggle: AdvancedToggleComponent): Boolean {
                return !parentToggle.getToggleState()
            }
        }

        fun custom(condition: (AdvancedToggleComponent) -> Boolean) = object : DependencyCondition {
            override fun isMet(parentToggle: AdvancedToggleComponent): Boolean {
                return condition(parentToggle)
            }
        }
    }

    /**
     * Custom dependency validator interface
     */
    interface DependencyValidator {
        fun validateChange(toggle: AdvancedToggleComponent, newValue: Boolean): Boolean
    }
}