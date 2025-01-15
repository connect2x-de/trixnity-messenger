package de.connect2x.messenger.compose.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import org.koin.core.Koin
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


@Composable
private fun defaultScrollbarStyle(): ScrollbarStyle {
    return LocalScrollbarStyle.current.copy(
        hoverColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        unhoverColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
    )
}

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) = VerticalScrollbar(
    rememberScrollbarAdapter(scrollState),
    modifier,
    style = defaultScrollbarStyle(),
)

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
) = VerticalScrollbar(
    rememberScrollbarAdapter(lazyListState),
    modifier,
    reverseLayout,
    style = defaultScrollbarStyle(),
)

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState,
) = HorizontalScrollbar(
    rememberScrollbarAdapter(scrollState),
    modifier,
    style = defaultScrollbarStyle(),
)

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
) = HorizontalScrollbar(
    rememberScrollbarAdapter(lazyListState),
    modifier,
    reverseLayout,
    style = defaultScrollbarStyle(),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    delayMillis: Int,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    return TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.widthIn(max = 600.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiary
            ) {
                tooltip()
            }
        },
        delayMillis = delayMillis,
        modifier = onClick?.let { modifier.clickable { onClick() } } ?: modifier,
        content = content,
    )
}

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier =
    this.then(
        Modifier.pointerHoverIcon(
            if (enabled) PointerIcon.Hand else PointerIcon.Default,
            false
        )
    )

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier {
    return this.then(
        Modifier
            .onPointerEvent(PointerEventType.Enter) {
                onEnter()
            }
            .onPointerEvent(PointerEventType.Exit) {
                onExit()
            }
    )

}

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
) = this.onPointerEvent(
    eventType,
    pass,
    onEvent,
)

actual suspend fun copyToClipboard(value: String, di: Koin) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}
