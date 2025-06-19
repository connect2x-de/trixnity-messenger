package de.connect2x.messenger.compose.view

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
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
import de.connect2x.messenger.compose.view.common.tooltipAnchorSemantics
import de.connect2x.messenger.compose.view.common.tooltipGestures
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.ThemedPlainTooltip
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.koin.core.Koin
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) = VerticalScrollbar(
    rememberScrollbarAdapter(scrollState),
    modifier,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalAtomicApi::class)
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
        tooltip = { ThemedPlainTooltip { tooltip() } },
        state = tooltipState,
        enableUserInput = false,
    ) {
        content()
    }
}

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier =
    this.pointerHoverIcon(
        if (enabled) PointerIcon.Hand else PointerIcon.Default,
        false,
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
    window.navigator.clipboard.writeText(value).await()
}
