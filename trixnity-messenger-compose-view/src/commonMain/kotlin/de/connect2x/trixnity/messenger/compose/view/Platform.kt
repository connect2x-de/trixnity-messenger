package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import org.koin.core.Koin

@Composable expect fun VerticalScrollbar(modifier: Modifier, scrollState: ScrollState)

@Composable expect fun VerticalScrollbar(modifier: Modifier, lazyListState: LazyListState, reverseLayout: Boolean)

@Composable expect fun HorizontalScrollbar(modifier: Modifier, scrollState: ScrollState)

@Composable expect fun HorizontalScrollbar(modifier: Modifier, lazyListState: LazyListState, reverseLayout: Boolean)

expect fun Modifier.buttonPointerModifier(enabled: Boolean = true): Modifier

expect fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier

/** Wraps `Modify.onPointerEvent` which will invoke [onEvent] only on Desktop and Web. */
expect fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit,
): Modifier

expect suspend fun copyToClipboard(value: String, di: Koin)
