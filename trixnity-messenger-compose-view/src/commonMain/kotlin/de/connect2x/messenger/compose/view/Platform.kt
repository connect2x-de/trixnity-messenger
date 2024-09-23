package de.connect2x.messenger.compose.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.core.Koin

@Composable
expect fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState,
)

@Composable
expect fun VerticalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
)

@Composable
expect fun HorizontalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState,
)

@Composable
expect fun HorizontalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
)

@Composable
expect fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    delayMillis: Int = 500,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
)

expect fun Modifier.buttonPointerModifier(enabled: Boolean = true): Modifier

expect fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier

expect suspend fun copyToClipboard(value: String, di: Koin)
