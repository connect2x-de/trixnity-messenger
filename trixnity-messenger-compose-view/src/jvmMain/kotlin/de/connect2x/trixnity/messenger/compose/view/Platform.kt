package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import org.koin.core.Koin

@Composable
private fun defaultScrollbarStyle(): ScrollbarStyle =
    LocalScrollbarStyle.current.copy(
        hoverColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        unhoverColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
    )

@Composable
actual fun VerticalScrollbar(modifier: Modifier, scrollState: ScrollState) =
    VerticalScrollbar(rememberScrollbarAdapter(scrollState), modifier, style = defaultScrollbarStyle())

@Composable
actual fun VerticalScrollbar(modifier: Modifier, lazyListState: LazyListState, reverseLayout: Boolean) =
    VerticalScrollbar(rememberScrollbarAdapter(lazyListState), modifier, reverseLayout, style = defaultScrollbarStyle())

@Composable
actual fun HorizontalScrollbar(modifier: Modifier, scrollState: ScrollState) =
    HorizontalScrollbar(rememberScrollbarAdapter(scrollState), modifier, style = defaultScrollbarStyle())

@Composable
actual fun HorizontalScrollbar(modifier: Modifier, lazyListState: LazyListState, reverseLayout: Boolean) =
    HorizontalScrollbar(
        rememberScrollbarAdapter(lazyListState),
        modifier,
        reverseLayout,
        style = defaultScrollbarStyle(),
    )

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier =
    this.pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default, false)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier =
    this.onPointerEvent(PointerEventType.Enter) { onEnter() }.onPointerEvent(PointerEventType.Exit) { onExit() }

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit,
) = this.onPointerEvent(eventType, pass, onEvent)

actual suspend fun copyToClipboard(value: String, di: Koin) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}
