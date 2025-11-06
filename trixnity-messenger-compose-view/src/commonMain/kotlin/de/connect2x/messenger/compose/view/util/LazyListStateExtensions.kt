package de.connect2x.messenger.compose.view.util

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.util.fastSumBy

private val LazyListLayoutInfo.visibleItemsAverageSize: Int
    get() {
        val visibleItems = visibleItemsInfo
        val itemsSum = visibleItems.fastSumBy { it.size }
        return itemsSum / visibleItems.size + mainAxisItemSpacing
    }

private val LazyListLayoutInfo.viewportItems: List<LazyListItemInfo>
    get() = visibleItemsInfo.filter {
        val start = it.offset
        val end = it.offset + it.size
        start >= viewportStartOffset && end <= viewportEndOffset
    }

suspend fun LazyListState.scrollIntoView(index: Int) {
    val actuallyVisibleItems = layoutInfo.viewportItems
    val firstVisibleItem = actuallyVisibleItems.first().index
    val lastVisibleItem = actuallyVisibleItems.last().index
    when {
        index <= firstVisibleItem -> {
            scrollToItem(index, -layoutInfo.viewportStartOffset)
        }

        index >= lastVisibleItem -> {
            val item = layoutInfo.visibleItemsInfo.find { it.index == index }
            if (item == null) {
                scrollToItem(index, -(layoutInfo.viewportEndOffset - layoutInfo.visibleItemsAverageSize))
                val item = layoutInfo.visibleItemsInfo.find { it.index == index }
                if (item != null) {
                    scrollToItem(index, -(layoutInfo.viewportEndOffset - item.size))
                }
            } else {
                scrollToItem(index, -(layoutInfo.viewportEndOffset - item.size))
            }
        }

        else -> Unit
    }
}
