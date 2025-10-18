package com.mtlc.studyplan.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.performance.PerformanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LazyListOptimizer(
    private val performanceMonitor: PerformanceMonitor? = null
) {
    private val _isScrolling = MutableStateFlow(false)
    val isScrolling: StateFlow<Boolean> = _isScrolling.asStateFlow()

    private val _visibleItemsRange = MutableStateFlow(0..0)
    val visibleItemsRange: StateFlow<IntRange> = _visibleItemsRange.asStateFlow()

    @Composable
    fun rememberOptimizedListState(
        initialFirstVisibleItemIndex: Int = 0,
        initialFirstVisibleItemScrollOffset: Int = 0
    ): LazyListState {
        val listState = remember {
            LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
        }

        val density = LocalDensity.current

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .collect { isScrolling ->
                    _isScrolling.value = isScrolling
                    if (isScrolling) {
                        performanceMonitor?.logPerformanceIssue("LazyList", "Scroll started")
                    }
                }
        }

        LaunchedEffect(listState) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.let { items ->
                    if (items.isNotEmpty()) {
                        items.first().index..items.last().index
                    } else {
                        0..0
                    }
                }
            }.collect { range ->
                _visibleItemsRange.value = range
            }
        }

        return listState
    }

    fun shouldShowItem(itemIndex: Int, bufferSize: Int = 5): Boolean {
        val range = _visibleItemsRange.value
        return itemIndex in (range.first - bufferSize)..(range.last + bufferSize)
    }

    fun calculateOptimalItemHeight(density: Density, contentHeight: Int): Int {
        return with(density) {
            // Ensure minimum touch target of 48dp
            maxOf(48.dp.roundToPx(), contentHeight)
        }
    }
}

@Composable
fun rememberLazyListOptimizer(
    performanceMonitor: PerformanceMonitor? = null
): LazyListOptimizer {
    return remember { LazyListOptimizer(performanceMonitor) }
}

class RecyclerViewPool<T> {
    private val pool = mutableMapOf<String, MutableList<T>>()
    private val maxPoolSize = 20

    fun acquire(key: String, factory: () -> T): T {
        val poolForKey = pool.getOrPut(key) { mutableListOf() }
        return if (poolForKey.isNotEmpty()) {
            poolForKey.removeAt(poolForKey.size - 1)
        } else {
            factory()
        }
    }

    fun release(key: String, item: T) {
        val poolForKey = pool.getOrPut(key) { mutableListOf() }
        if (poolForKey.size < maxPoolSize) {
            poolForKey.add(item)
        }
    }

    fun clear() {
        pool.clear()
    }
}

@Composable
fun <T> rememberRecyclerViewPool(): RecyclerViewPool<T> {
    return remember { RecyclerViewPool<T>() }
}

data class ViewportInfo(
    val startIndex: Int,
    val endIndex: Int,
    val startOffset: Int,
    val endOffset: Int
)

@Composable
fun LazyListState.collectViewportInfo(): State<ViewportInfo> {
    return remember(this) {
        derivedStateOf {
            val layoutInfo = layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo

            if (visibleItems.isEmpty()) {
                ViewportInfo(0, 0, 0, 0)
            } else {
                ViewportInfo(
                    startIndex = visibleItems.first().index,
                    endIndex = visibleItems.last().index,
                    startOffset = visibleItems.first().offset,
                    endOffset = visibleItems.last().offset + visibleItems.last().size
                )
            }
        }
    }
}

@Composable
fun LazyListState.isItemFullyVisible(index: Int): Boolean {
    val layoutInfo = layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    val item = visibleItems.find { it.index == index } ?: return false

    return item.offset >= 0 &&
           item.offset + item.size <= layoutInfo.viewportEndOffset
}

object LazyListKeys {
    fun taskItem(taskId: String) = "task_$taskId"
    fun categoryHeader(categoryId: String) = "category_$categoryId"
    fun progressItem(progressId: String) = "progress_$progressId"
    fun settingsItem(settingKey: String) = "setting_$settingKey"
}
