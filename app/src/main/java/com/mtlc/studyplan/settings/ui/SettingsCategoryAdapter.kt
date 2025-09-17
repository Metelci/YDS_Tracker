package com.mtlc.studyplan.settings.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ItemSettingsCategoryBinding
import com.mtlc.studyplan.settings.data.SettingsCategory
import java.util.*

/**
 * RecyclerView adapter for settings categories with Material Design 3 styling and animations
 */
class SettingsCategoryAdapter(
    private val clickListener: OnCategoryClickListener,
    private val spanCount: Int
) : RecyclerView.Adapter<SettingsCategoryAdapter.CategoryViewHolder>() {

    interface OnCategoryClickListener {
        fun onCategoryClick(category: SettingsCategory, sharedElement: View)
    }

    private var categories: List<SettingsCategory> = emptyList()
    private var filteredCategories: List<SettingsCategory> = emptyList()
    private var searchQuery: String = ""
    private var selectedPosition: Int = -1

    companion object {
        private const val ANIMATION_DURATION = 250L
        private const val SCALE_SELECTED = 1.05f
        private const val SCALE_NORMAL = 1.0f
        private const val ALPHA_SELECTED = 1.0f
        private const val ALPHA_NORMAL = 0.8f
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSettingsCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = filteredCategories[position]
        holder.bind(category, searchQuery, position == selectedPosition)

        // Set click listener
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = position

            // Animate deselection of previous item
            if (previousSelected != -1 && previousSelected < filteredCategories.size) {
                notifyItemChanged(previousSelected)
            }

            // Animate selection of current item
            notifyItemChanged(position)

            // Trigger click callback with shared element
            clickListener.onCategoryClick(category, holder.itemView)
        }

        // Set up shared element transition name
        holder.itemView.transitionName = "settings_category_${category.id}"
    }

    override fun getItemCount(): Int = filteredCategories.size

    fun updateCategories(newCategories: List<SettingsCategory>, query: String = "") {
        val oldCategories = categories
        categories = newCategories
        searchQuery = query

        filteredCategories = if (query.isBlank()) {
            categories
        } else {
            categories.filter { category ->
                category.title.contains(query, ignoreCase = true) ||
                category.description.contains(query, ignoreCase = true)
            }
        }

        // Use DiffUtil for efficient updates
        val diffResult = DiffUtil.calculateDiff(
            CategoryDiffCallback(oldCategories, categories)
        )
        diffResult.dispatchUpdatesTo(this)
    }

    fun clearSelection() {
        val previousSelected = selectedPosition
        selectedPosition = -1
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected)
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemSettingsCategoryBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentAnimator: ValueAnimator? = null

        fun bind(category: SettingsCategory, searchQuery: String, isSelected: Boolean) {
            // Set category data
            binding.categoryTitle.text = highlightSearchText(category.title, searchQuery)
            binding.categoryDescription.text = highlightSearchText(category.description, searchQuery)

            // Set category icon
            loadCategoryIcon(category)

            // Apply selection state
            applySelectionState(isSelected)

            // Set accessibility
            setupAccessibility(category)

            // Set active state
            binding.activeIndicator.visibility = if (category.isActive) View.VISIBLE else View.GONE
        }

        private fun loadCategoryIcon(category: SettingsCategory) {
            try {
                val drawable = ContextCompat.getDrawable(context, category.iconRes)
                binding.categoryIcon.setImageDrawable(drawable)
            } catch (e: Exception) {
                // Fallback to default icon
                binding.categoryIcon.setImageDrawable(getDefaultIcon(category.id))
            }
        }

        private fun getDefaultIcon(categoryId: String): Drawable? {
            val fallbackIconRes = when (categoryId) {
                SettingsCategory.PRIVACY_ID -> R.drawable.ic_shield
                SettingsCategory.NOTIFICATIONS_ID -> R.drawable.ic_notifications
                SettingsCategory.GAMIFICATION_ID -> R.drawable.ic_star
                SettingsCategory.TASKS_ID -> R.drawable.ic_task
                SettingsCategory.NAVIGATION_ID -> R.drawable.ic_navigation
                SettingsCategory.SOCIAL_ID -> R.drawable.ic_people
                SettingsCategory.ACCESSIBILITY_ID -> R.drawable.ic_accessibility
                SettingsCategory.DATA_ID -> R.drawable.ic_storage
                else -> R.drawable.ic_settings
            }

            return try {
                ContextCompat.getDrawable(context, fallbackIconRes)
            } catch (e: Exception) {
                ContextCompat.getDrawable(context, R.drawable.ic_settings)
            }
        }

        private fun applySelectionState(isSelected: Boolean) {
            currentAnimator?.cancel()

            val targetScale = if (isSelected) SCALE_SELECTED else SCALE_NORMAL
            val targetAlpha = if (isSelected) ALPHA_SELECTED else ALPHA_NORMAL
            val targetElevation = if (isSelected) 8f else 2f

            // Animate scale and alpha
            currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = ANIMATION_DURATION
                interpolator = DecelerateInterpolator()

                val startScale = itemView.scaleX
                val startAlpha = itemView.alpha
                val startElevation = binding.root.cardElevation

                addUpdateListener { animation ->
                    val fraction = animation.animatedFraction

                    itemView.scaleX = startScale + (targetScale - startScale) * fraction
                    itemView.scaleY = startScale + (targetScale - startScale) * fraction
                    itemView.alpha = startAlpha + (targetAlpha - startAlpha) * fraction
                    binding.root.cardElevation = startElevation + (targetElevation - startElevation) * fraction
                }

                start()
            }

            // Update selection overlay
            binding.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Update card colors for selection state
            if (isSelected) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_selected_background)
                )
                binding.categoryIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.icon_selected_tint)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_background)
                )
                binding.categoryIcon.clearColorFilter()
            }
        }

        private fun highlightSearchText(text: String, query: String): CharSequence {
            if (query.isBlank()) return text

            val spannable = SpannableString(text)
            val queryLower = query.lowercase(Locale.getDefault())
            val textLower = text.lowercase(Locale.getDefault())

            var startIndex = 0
            while (startIndex < textLower.length) {
                val index = textLower.indexOf(queryLower, startIndex)
                if (index == -1) break

                val highlightColor = ContextCompat.getColor(context, R.color.search_highlight_color)
                spannable.setSpan(
                    BackgroundColorSpan(highlightColor),
                    index,
                    index + query.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                startIndex = index + query.length
            }

            return spannable
        }

        private fun setupAccessibility(category: SettingsCategory) {
            itemView.contentDescription = context.getString(
                R.string.settings_category_description,
                category.title,
                category.description
            )

            // Set semantic information
            itemView.isClickable = true
            itemView.isFocusable = true

            // Add state description for active categories
            if (category.isActive) {
                itemView.stateDescription = context.getString(R.string.category_active)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class CategoryDiffCallback(
        private val oldList: List<SettingsCategory>,
        private val newList: List<SettingsCategory>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.title == newItem.title &&
                   oldItem.description == newItem.description &&
                   oldItem.iconRes == newItem.iconRes &&
                   oldItem.isActive == newItem.isActive &&
                   oldItem.sortOrder == newItem.sortOrder
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            val changes = mutableMapOf<String, Any>()

            if (oldItem.title != newItem.title) {
                changes["title"] = newItem.title
            }

            if (oldItem.description != newItem.description) {
                changes["description"] = newItem.description
            }

            if (oldItem.isActive != newItem.isActive) {
                changes["active"] = newItem.isActive
            }

            return if (changes.isEmpty()) null else changes
        }
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Handle partial updates
            val category = filteredCategories[position]
            val changes = payloads[0] as Map<String, Any>

            changes["title"]?.let {
                holder.binding.categoryTitle.text = highlightSearchText(it as String, searchQuery)
            }

            changes["description"]?.let {
                holder.binding.categoryDescription.text = highlightSearchText(it as String, searchQuery)
            }

            changes["active"]?.let {
                holder.binding.activeIndicator.visibility = if (it as Boolean) View.VISIBLE else View.GONE
            }
        }
    }

    private fun highlightSearchText(text: String, query: String): CharSequence {
        if (query.isBlank()) return text

        val spannable = SpannableString(text)
        val queryLower = query.lowercase(Locale.getDefault())
        val textLower = text.lowercase(Locale.getDefault())

        var startIndex = 0
        while (startIndex < textLower.length) {
            val index = textLower.indexOf(queryLower, startIndex)
            if (index == -1) break

            // Use a placeholder color - this should be defined in colors.xml
            val highlightColor = android.graphics.Color.YELLOW // Placeholder
            spannable.setSpan(
                BackgroundColorSpan(highlightColor),
                index,
                index + query.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            startIndex = index + query.length
        }

        return spannable
    }
}