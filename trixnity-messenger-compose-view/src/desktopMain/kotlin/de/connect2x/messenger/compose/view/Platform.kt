package de.connect2x.messenger.compose.view

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
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
import de.connect2x.messenger.compose.view.common.tooltipAnchorSemantics
import de.connect2x.messenger.compose.view.common.tooltipGestures
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.TooltipStyle
import org.koin.core.Koin
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.time.Duration


@Composable
private fun defaultScrollbarStyle(): ScrollbarStyle =
    LocalScrollbarStyle.current.copy(
        hoverColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        unhoverColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
    )

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    longPressDelay: Duration,
    hoverShowDelay: Duration,
    hoverHideDelay: Duration,
    content: @Composable () -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    TooltipBox(
        modifier = Modifier
            .tooltipGestures(
                enabled = enabled,
                state = tooltipState,
                longPressDelay = longPressDelay,
                hoverShowDelay = hoverShowDelay,
                hoverHideDelay = hoverHideDelay,
            )
            .tooltipAnchorSemantics(i18n.commonShowTooltip(), enabled, tooltipState, scope),
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { TooltipSurface(content = { tooltip() }) },
        state = tooltipState,
        enableUserInput = false,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipSurface(
    style: TooltipStyle = MaterialTheme.components.tooltip,
    content: @Composable () -> Unit
) {
    Surface(
        shape = style.shape,
        color = style.colors.containerColor,
        tonalElevation = style.tonalElevation,
        shadowElevation = style.shadowElevation
    ) {
        Box(modifier = Modifier
            .sizeIn(
                minWidth = 40.dp,
                maxWidth = 600.dp,
                minHeight = 24.dp
            )
            .padding(8.dp, 4.dp)
            .padding(style.contentPadding)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides style.colors.contentColor,
                LocalTextStyle provides style.textStyle,
                content = content
            )
        }
    }
}

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier =
    this.pointerHoverIcon(
        if (enabled) PointerIcon.Hand else PointerIcon.Default,
        false
    )

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier = this
    .onPointerEvent(PointerEventType.Enter) {
        onEnter()
    }
    .onPointerEvent(PointerEventType.Exit) {
        onExit()
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
