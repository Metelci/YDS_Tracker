package com.mtlc.studyplan.settings.ui

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.ItemSearchResultBinding
import com.mtlc.studyplan.databinding.ItemSearchSectionHeaderBinding
import com.mtlc.studyplan.settings.search.SearchResult
import java.util.*

/**
 * Adapter for settings search results with highlighting and grouping
 */
class SettingsSearchAdapter(
    private val onResultClick: (SearchResult) -> Unit,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SearchItem> = emptyList()
    private var query: String = ""

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_SEARCH_RESULT = 1
        private const val MAX_DESCRIPTION_LENGTH = 120
    }

    sealed class SearchItem {
        data class SectionHeader(
            val categoryId: String,
            val categoryTitle: String,
            val resultCount: Int
        ) : SearchItem()

        data class Result(val searchResult: SearchResult) : SearchItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SearchItem.SectionHeader -> TYPE_SECTION_HEADER
            is SearchItem.Result -> TYPE_SEARCH_RESULT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_SECTION_HEADER -> {
                val binding = ItemSearchSectionHeaderBinding.inflate(layoutInflater, parent, false)
                SectionHeaderViewHolder(binding, parent.context)
            }
            TYPE_SEARCH_RESULT -> {
                val binding = ItemSearchResultBinding.inflate(layoutInflater, parent, false)
                SearchResultViewHolder(binding, parent.context)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SearchItem.SectionHeader -> {
                (holder as SectionHeaderViewHolder).bind(item)
            }
            is SearchItem.Result -> {
                (holder as SearchResultViewHolder).bind(item.searchResult, query)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Update search results with grouping and sorting
     */
    fun updateResults(results: List<SearchResult>, query: String) {
        val oldItems = items
        this.query = query

        // Group results by category
        val groupedResults = results.groupBy { it.item.categoryId }

        // Create display items
        val newItems = mutableListOf<SearchItem>()

        groupedResults.entries
            .sortedByDescending { (_, results) -> results.maxOf { it.relevanceScore } }
            .forEach { (categoryId, categoryResults) ->
                // Add section header
                val categoryTitle = categoryResults.first().item.categoryTitle
                newItems.add(
                    SearchItem.SectionHeader(
                        categoryId = categoryId,
                        categoryTitle = categoryTitle,
                        resultCount = categoryResults.size
                    )
                )

                // Add sorted results
                categoryResults
                    .sortedByDescending { it.relevanceScore }
                    .forEach { result ->
                        newItems.add(SearchItem.Result(result))
                    }
            }

        items = newItems

        // Use DiffUtil for efficient updates
        val diffResult = DiffUtil.calculateDiff(
            SearchDiffCallback(oldItems, newItems)
        )
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * ViewHolder for section headers
     */
    inner class SectionHeaderViewHolder(
        private val binding: ItemSearchSectionHeaderBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(header: SearchItem.SectionHeader) {
            binding.sectionTitle.text = header.categoryTitle
            binding.resultCount.text = "${header.resultCount} result${if (header.resultCount != 1) "s" else ""}"

            // Set category icon
            val iconRes = getCategoryIcon(header.categoryId)
            binding.categoryIcon.setImageResource(iconRes)

            // Click listener to view all in category
            binding.root.setOnClickListener {
                onCategoryClick(header.categoryId)
            }

            // Accessibility
            binding.root.contentDescription = "${header.categoryTitle} category, ${header.resultCount} results"
        }

        private fun getCategoryIcon(categoryId: String): Int {
            return when (categoryId) {
                "privacy" -> R.drawable.ic_shield
                "notifications" -> R.drawable.ic_notifications
                "gamification" -> R.drawable.ic_star
                "tasks" -> R.drawable.ic_task
                "navigation" -> R.drawable.ic_navigation
                "social" -> R.drawable.ic_people
                "accessibility" -> R.drawable.ic_accessibility
                "data" -> R.drawable.ic_storage
                else -> R.drawable.ic_settings
            }
        }
    }

    /**
     * ViewHolder for search results
     */
    inner class SearchResultViewHolder(
        private val binding: ItemSearchResultBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: SearchResult, query: String) {
            val item = result.item

            // Set title with highlighting
            binding.settingTitle.text = highlightMatches(item.title, query)

            // Set description with highlighting and truncation
            val description = if (item.description.length > MAX_DESCRIPTION_LENGTH) {
                "${item.description.take(MAX_DESCRIPTION_LENGTH - 3)}..."
            } else {
                item.description
            }
            binding.settingDescription.text = highlightMatches(description, query)
            binding.settingDescription.isVisible = description.isNotEmpty()

            // Set category path
            binding.categoryPath.text = item.categoryTitle
            binding.categoryPath.isVisible = true

            // Set setting type icon
            val typeIcon = getSettingTypeIcon(item.settingItem)
            binding.settingTypeIcon.setImageResource(typeIcon)

            // Set relevance indicator
            updateRelevanceIndicator(result.relevanceScore)

            // Set click listener
            binding.root.setOnClickListener {
                onResultClick(result)
            }

            // Setup accessibility
            setupAccessibility(result)
        }

        /**
         * Highlight search matches in text
         */
        private fun highlightMatches(text: String, query: String): SpannableString {
            val spannable = SpannableString(text)
            if (query.isEmpty()) return spannable

            val queryWords = query.lowercase().split("\\s+".toRegex())
            val textLower = text.lowercase()

            queryWords.forEach { word ->
                var startIndex = 0
                while (startIndex < textLower.length) {
                    val index = textLower.indexOf(word, startIndex)
                    if (index == -1) break

                    val endIndex = index + word.length
                    if (endIndex <= text.length) {
                        // Highlight background
                        spannable.setSpan(
                            BackgroundColorSpan(
                                ContextCompat.getColor(context, R.color.search_highlight_color)
                            ),
                            index,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        // Bold text
                        spannable.setSpan(
                            StyleSpan(Typeface.BOLD),
                            index,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    startIndex = index + 1
                }
            }

            return spannable
        }

        /**
         * Get icon for setting type
         */
        private fun getSettingTypeIcon(setting: com.mtlc.studyplan.settings.data.SettingItem): Int {
            return when (setting) {
                is com.mtlc.studyplan.settings.data.ToggleSetting -> R.drawable.ic_toggle_on
                is com.mtlc.studyplan.settings.data.SelectionSetting<*> -> R.drawable.ic_list
                is com.mtlc.studyplan.settings.data.ActionSetting -> R.drawable.ic_play_arrow
                else -> R.drawable.ic_settings
            }
        }

        /**
         * Update relevance indicator based on score
         */
        private fun updateRelevanceIndicator(score: Double) {
            val alpha = when {
                score >= 8.0 -> 1.0f    // Excellent match
                score >= 5.0 -> 0.8f    // Good match
                score >= 3.0 -> 0.6f    // Fair match
                else -> 0.4f            // Weak match
            }

            binding.relevanceIndicator.alpha = alpha
            binding.relevanceIndicator.isVisible = score >= 3.0
        }

        /**
         * Setup accessibility
         */
        private fun setupAccessibility(result: SearchResult) {
            val item = result.item
            binding.root.contentDescription = buildString {
                append(item.title)
                append(", ")
                append(item.description)
                append(", in ")
                append(item.categoryTitle)
                append(" category")

                val relevanceDescription = when {
                    result.relevanceScore >= 8.0 -> ", excellent match"
                    result.relevanceScore >= 5.0 -> ", good match"
                    result.relevanceScore >= 3.0 -> ", fair match"
                    else -> ""
                }
                append(relevanceDescription)
            }

            // Add semantic role
            binding.root.isClickable = true
            binding.root.isFocusable = true
        }
    }

    /**
     * DiffUtil callback for efficient updates
     */
    private class SearchDiffCallback(
        private val oldList: List<SearchItem>,
        private val newList: List<SearchItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return when {
                oldItem is SearchItem.SectionHeader && newItem is SearchItem.SectionHeader ->
                    oldItem.categoryId == newItem.categoryId
                oldItem is SearchItem.Result && newItem is SearchItem.Result ->
                    oldItem.searchResult.item.id == newItem.searchResult.item.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem == newItem
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            if (oldItem is SearchItem.Result && newItem is SearchItem.Result) {
                val changes = mutableMapOf<String, Any>()

                if (oldItem.searchResult.relevanceScore != newItem.searchResult.relevanceScore) {
                    changes["relevance"] = newItem.searchResult.relevanceScore
                }

                return if (changes.isEmpty()) null else changes
            }

            return null
        }
    }
}