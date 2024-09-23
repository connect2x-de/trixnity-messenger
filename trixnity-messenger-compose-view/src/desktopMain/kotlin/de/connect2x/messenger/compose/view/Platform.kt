package de.connect2x.messenger.compose.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.Koin
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


private val log = KotlinLogging.logger { }

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) = VerticalScrollbar(
    rememberScrollbarAdapter(scrollState),
    modifier
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
)

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState,
) = HorizontalScrollbar(
    rememberScrollbarAdapter(scrollState),
    modifier,
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
        modifier = modifier.clickable(onClick = onClick?:{ }),
        content = content,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            // to prevent the tooltip getting in the way of the mouse that in turn prevents clicks
            offset = DpOffset(
                x = 10.dp,
                y = 0.dp
            )
        )
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
    return this.then(Modifier.pointerMoveFilter(onEnter = onEnter, onExit = onExit))
}

actual suspend fun copyToClipboard(value: String, di: Koin) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}
