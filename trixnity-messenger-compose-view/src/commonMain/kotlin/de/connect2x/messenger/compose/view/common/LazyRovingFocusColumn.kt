package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.util.RovingFocusState
import de.connect2x.messenger.compose.view.util.getNextItem
import de.connect2x.messenger.compose.view.util.getPreviousItem
import de.connect2x.messenger.compose.view.util.scrollIntoView
import de.connect2x.messenger.compose.view.util.verticalRovingFocus


@Composable
fun LazyRovingFocusColumn(
    defaultItem: Any?,
    references: List<Any?>,
    state: LazyListState = rememberLazyListState(),
    focusContainer: RovingFocusState?,
    content: LazyListScope.() -> Unit
) {
    LaunchedEffect(references) {
        val currentRef = focusContainer?.activeRef?.value
        if (currentRef != null && !references.contains(currentRef)) {
            focusContainer.activeRef.value = defaultItem
        }
    }
    LazyColumn(
        state = state, modifier = Modifier.verticalRovingFocus(
            scroll = { item ->
                val index = references.indexOf(item)
                if (index != -1) {
                    state.scrollIntoView(index)
                }
            },
            up = {
                focusContainer?.getPreviousItem(defaultItem, references)
            },
            down = {
                focusContainer?.getNextItem(defaultItem, references)
            },
        )
    ) { content() }
}
