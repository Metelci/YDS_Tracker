package com.mtlc.studyplan.settings.search

import android.content.Context
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsCategory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Advanced search engine for settings with fuzzy matching and indexing
 */
class SettingsSearchEngine(private val context: Context) {

    private val searchScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val settingsIndex = mutableMapOf<String, SearchableItem>()
    private val keywordIndex = mutableMapOf<String, MutableSet<String>>()
    private val categoryIndex = mutableMapOf<String, MutableSet<String>>()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private var isIndexed = false

    companion object {
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_RESULTS = 50
        private const val FUZZY_THRESHOLD = 0.7
    }

    data class SearchableItem(
        val id: String,
        val title: String,
        val description: String,
        val categoryId: String,
        val categoryTitle: String,
        val keywords: List<String>,
        val settingItem: SettingItem,
        val deepLinkPath: String
    )

    data class SearchResult(
        val item: SearchableItem,
        val relevanceScore: Double,
        val matchedText: String,
        val highlightRanges: List<HighlightRange>
    )

    data class HighlightRange(
        val start: Int,
        val end: Int,
        val field: MatchField
    )

    enum class MatchField {
        TITLE, DESCRIPTION, KEYWORDS, CATEGORY
    }

    /**
     * Index all settings for searching
     */
    suspend fun indexSettings(categories: List<SettingsCategory>, allSettings: Map<String, List<SettingItem>>) {
        withContext(Dispatchers.Default) {
            settingsIndex.clear()
            keywordIndex.clear()
            categoryIndex.clear()

            categories.forEach { category ->
                val categorySettings = allSettings[category.id] ?: emptyList()

                categorySettings.forEach { setting ->
                    val searchableItem = createSearchableItem(setting, category)
                    settingsIndex[setting.id] = searchableItem

                    // Index keywords
                    indexKeywords(setting.id, searchableItem)

                    // Index category
                    categoryIndex.getOrPut(category.id) { mutableSetOf() }.add(setting.id)
                }
            }

            isIndexed = true
        }
    }

    /**
     * Search settings with debounced input and fuzzy matching
     */
    fun search(query: String): Flow<List<SearchResult>> = flow {
        if (!isIndexed || query.length < MIN_QUERY_LENGTH) {
            emit(emptyList())
            return@flow
        }

        val results = performSearch(query)
        emit(results.take(MAX_RESULTS))
    }.debounce(300) // Debounce for performance
    .flowOn(Dispatchers.Default)

    /**
     * Get setting by ID for direct navigation
     */
    fun getSettingById(settingId: String): SearchableItem? {
        return settingsIndex[settingId]
    }

    /**
     * Get all settings in category
     */
    fun getSettingsInCategory(categoryId: String): List<SearchableItem> {
        val settingIds = categoryIndex[categoryId] ?: return emptyList()
        return settingIds.mapNotNull { settingsIndex[it] }
    }

    /**
     * Create searchable item from setting and category
     */
    private fun createSearchableItem(setting: SettingItem, category: SettingsCategory): SearchableItem {
        val keywords = mutableListOf<String>().apply {
            // Add setting-specific keywords
            addAll(setting.searchKeywords)

            // Add category keywords
            addAll(category.searchKeywords)

            // Add inferred keywords based on setting type and content
            addAll(inferKeywords(setting))
        }

        return SearchableItem(
            id = setting.id,
            title = setting.title,
            description = setting.description,
            categoryId = category.id,
            categoryTitle = category.title,
            keywords = keywords,
            settingItem = setting,
            deepLinkPath = "settings/${category.id}/${setting.id}"
        )
    }

    /**
     * Index keywords for fast lookup
     */
    private fun indexKeywords(settingId: String, item: SearchableItem) {
        val allText = listOf(
            item.title,
            item.description,
            item.categoryTitle
        ) + item.keywords

        allText.forEach { text ->
            text.split("\\s+".toRegex()).forEach { word ->
                val cleanWord = word.lowercase().trim()
                if (cleanWord.length >= 2) {
                    keywordIndex.getOrPut(cleanWord) { mutableSetOf() }.add(settingId)

                    // Add partial matches for better fuzzy search
                    if (cleanWord.length > 3) {
                        for (i in 2..cleanWord.length - 1) {
                            val partial = cleanWord.substring(0, i)
                            keywordIndex.getOrPut(partial) { mutableSetOf() }.add(settingId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Infer keywords based on setting content
     */
    private fun inferKeywords(setting: SettingItem): List<String> {
        val keywords = mutableListOf<String>()

        // Add type-based keywords
        when (setting) {
            is com.mtlc.studyplan.settings.data.ToggleSetting -> {
                keywords.addAll(listOf("toggle", "switch", "enable", "disable", "on", "off"))
            }
            is com.mtlc.studyplan.settings.data.SelectionSetting<*> -> {
                keywords.addAll(listOf("selection", "choose", "pick", "option"))
                // Add option values as keywords
                setting.options.forEach { option ->
                    keywords.add(option.display.lowercase())
                }
            }
            is com.mtlc.studyplan.settings.data.ActionSetting -> {
                keywords.addAll(listOf("action", "button", "execute", "run"))
                if (setting.actionType == com.mtlc.studyplan.settings.data.ActionSetting.ActionType.DESTRUCTIVE) {
                    keywords.addAll(listOf("delete", "remove", "clear", "reset"))
                }
            }
        }

        // Add context-based keywords
        when (setting.category) {
            "privacy" -> keywords.addAll(listOf("private", "public", "secure", "data", "share"))
            "notifications" -> keywords.addAll(listOf("alert", "reminder", "notify", "push", "email"))
            "gamification" -> keywords.addAll(listOf("points", "streak", "reward", "achievement", "game"))
        }

        return keywords
    }

    /**
     * Perform the actual search with scoring
     */
    private suspend fun performSearch(query: String): List<SearchResult> {
        val queryWords = query.lowercase().split("\\s+".toRegex())
        val candidateIds = mutableSetOf<String>()

        // Find candidate settings using keyword index
        queryWords.forEach { word ->
            keywordIndex.keys.forEach { indexedWord ->
                if (indexedWord.contains(word) || calculateSimilarity(word, indexedWord) > FUZZY_THRESHOLD) {
                    keywordIndex[indexedWord]?.let { ids ->
                        candidateIds.addAll(ids)
                    }
                }
            }
        }

        // Score and rank candidates
        val results = candidateIds.mapNotNull { id ->
            settingsIndex[id]?.let { item ->
                val score = calculateRelevanceScore(item, queryWords)
                if (score > 0) {
                    SearchResult(
                        item = item,
                        relevanceScore = score,
                        matchedText = getMatchedText(item, queryWords),
                        highlightRanges = getHighlightRanges(item, queryWords)
                    )
                } else null
            }
        }.sortedByDescending { it.relevanceScore }

        return results
    }

    /**
     * Calculate relevance score for search result
     */
    private fun calculateRelevanceScore(item: SearchableItem, queryWords: List<String>): Double {
        var score = 0.0

        queryWords.forEach { word ->
            // Exact matches in title (highest weight)
            if (item.title.lowercase().contains(word)) {
                score += if (item.title.lowercase() == word) 10.0 else 5.0
            }

            // Exact matches in description
            if (item.description.lowercase().contains(word)) {
                score += 3.0
            }

            // Matches in keywords
            item.keywords.forEach { keyword ->
                if (keyword.lowercase().contains(word)) {
                    score += 2.0
                }
            }

            // Fuzzy matches
            val titleSimilarity = calculateSimilarity(word, item.title.lowercase())
            val descSimilarity = calculateSimilarity(word, item.description.lowercase())

            if (titleSimilarity > FUZZY_THRESHOLD) {
                score += titleSimilarity * 3.0
            }
            if (descSimilarity > FUZZY_THRESHOLD) {
                score += descSimilarity * 1.5
            }
        }

        return score
    }

    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1

        if (longer.isEmpty()) return 1.0

        val editDistance = levenshteinDistance(longer, shorter)
        return (longer.length - editDistance) / longer.length.toDouble()
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    /**
     * Get matched text snippet for display
     */
    private fun getMatchedText(item: SearchableItem, queryWords: List<String>): String {
        // Find the best matching text snippet
        val title = item.title
        val description = item.description

        queryWords.forEach { word ->
            if (title.lowercase().contains(word.lowercase())) {
                return title
            }
        }

        return if (description.length > 100) {
            "${description.take(97)}..."
        } else {
            description
        }
    }

    /**
     * Get highlight ranges for matched text
     */
    private fun getHighlightRanges(item: SearchableItem, queryWords: List<String>): List<HighlightRange> {
        val ranges = mutableListOf<HighlightRange>()

        queryWords.forEach { word ->
            // Highlight in title
            var index = item.title.lowercase().indexOf(word.lowercase())
            while (index != -1) {
                ranges.add(HighlightRange(index, index + word.length, MatchField.TITLE))
                index = item.title.lowercase().indexOf(word.lowercase(), index + 1)
            }

            // Highlight in description
            index = item.description.lowercase().indexOf(word.lowercase())
            while (index != -1) {
                ranges.add(HighlightRange(index, index + word.length, MatchField.DESCRIPTION))
                index = item.description.lowercase().indexOf(word.lowercase(), index + 1)
            }
        }

        return ranges.sortedBy { it.start }
    }

    /**
     * Clear search results
     */
    fun clearResults() {
        _searchResults.value = emptyList()
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        searchScope.cancel()
        settingsIndex.clear()
        keywordIndex.clear()
        categoryIndex.clear()
    }
}