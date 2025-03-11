package de.connect2x.messenger.compose.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import org.koin.core.Koin

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState,
){
    // TODO
}

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
) {
    // TODO
}

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState,
) {
    // TODO
}

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
) {
    // TODO
}

@Composable
actual fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    delayMillis: Int,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    // TODO
}

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier = Modifier // TODO

actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier = Modifier // TODO

/**
 * Wraps `Modify.onPointerEvent` which will invoke [onEvent] only on Desktop and Web.
 */
actual fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
): Modifier = Modifier // TODO

actual suspend fun copyToClipboard(value: String, di: Koin) {} // TODO
