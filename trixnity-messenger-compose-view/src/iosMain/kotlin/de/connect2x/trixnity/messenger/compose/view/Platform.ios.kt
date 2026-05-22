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
import platform.UIKit.UIPasteboard

@Composable actual fun VerticalScrollbar(modifier: Modifier, scrollState: ScrollState) = Unit

@Composable
actual fun VerticalScrollbar(modifier: Modifier, lazyListState: LazyListState, reverseLayout: Boolean) = Unit

@Composable actual fun HorizontalScrollbar(modifier: Modifier, scrollState: ScrollState) = Unit

@Composable
actual fun HorizontalScrollbar(modifier: Modifier, lazyListState: LazyListState, reverseLayout: Boolean) = Unit

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier = this

actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier = this

/** Wraps `Modify.onPointerEvent` which will invoke [onEvent] only on Desktop and Web. */
actual fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit,
): Modifier = this

actual suspend fun copyToClipboard(value: String, di: Koin) {
    UIPasteboard.generalPasteboard.string = value
}
