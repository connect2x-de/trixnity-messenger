package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.EscapeKeyPressed
import de.connect2x.messenger.compose.view.common.modifier.tooltipAnchorSemantics
import de.connect2x.messenger.compose.view.common.modifier.tooltipGestures
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.TooltipStyle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    longPressDelay: Duration = MaterialTheme.components.tooltip.longPressDelay,
    hoverShowDelay: Duration = MaterialTheme.components.tooltip.hoverShowDelay,
    hoverHideDelay: Duration = MaterialTheme.components.tooltip.hoverHideDelay,
    content: @Composable () -> Unit,
) {
    Tooltip(
        tooltip = { Text(tooltip) },
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        longPressDelay = longPressDelay,
        hoverShowDelay = hoverShowDelay,
        hoverHideDelay = hoverHideDelay,
    ) {
        Box(Modifier.semantics(true) { text = AnnotatedString(tooltip) }) {
            content()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    longPressDelay: Duration = MaterialTheme.components.tooltip.longPressDelay,
    hoverShowDelay: Duration = MaterialTheme.components.tooltip.hoverShowDelay,
    hoverHideDelay: Duration = MaterialTheme.components.tooltip.hoverHideDelay,
    content: @Composable () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    // We need to hoist those tasks here to allow keeping the tooltip open when hovering over the tooltip itself
    // (which is not part of the TooltipArea).
    var showTask: Job? = null
    var hideTask: Job? = null

    val showTooltip = {
        hideTask?.cancel()
        hideTask = null

        if (showTask == null) {
            showTask = scope.launch {
                delay(hoverShowDelay)
                tooltipState.show(MutatePriority.PreventUserInput)
                showTask = null
            }
        }
    }

    val hideTooltip = {
        showTask?.cancel()
        showTask = null

        if (hideTask == null && tooltipState.isPersistent.not()) {
            hideTask = scope.launch {
                delay(hoverHideDelay)
                tooltipState.dismiss()
                hideTask = null
            }
        }
    }

    val escapeKeyPressed = EscapeKeyPressed.current
    LaunchedEffect(Unit) {
        escapeKeyPressed.collect {
            hideTooltip()
        }
    }

    TooltipBox(
        modifier = modifier
            .tooltipGestures(
                enabled = enabled,
                state = tooltipState,
                longPressDelay = longPressDelay,
                showTooltip = showTooltip,
                hideTooltip = hideTooltip,
            )
            .tooltipAnchorSemantics(i18n.commonShowTooltip(), enabled, tooltipState, scope),
        // TODO: Tooltips should be adaptive (top, bottom, etc. depending on location)
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            TooltipSurface(
                showTooltip = showTooltip,
                hideTooltip = hideTooltip,
            ) { tooltip() }
        },
        state = tooltipState,
        enableUserInput = false,
    ) {
        Box(
            Modifier
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        if (enabled) {
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                delay(hoverShowDelay)
                                tooltipState.show()
                            }
                        }
                    } else {
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            delay(hoverHideDelay)
                            tooltipState.dismiss()
                        }
                    }
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipSurface(
    style: TooltipStyle = MaterialTheme.components.tooltip,
    showTooltip: () -> Unit,
    hideTooltip: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.pointerInput(Unit) {
            coroutineScope {
                awaitPointerEventScope {
                    val pass = PointerEventPass.Main
                    while (true) {
                        val event = awaitPointerEvent(pass)
                        val inputType = event.changes[0].type
                        if (inputType == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter -> {
                                    showTooltip()
                                }

                                PointerEventType.Exit -> {
                                    hideTooltip()
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = style.shape,
        color = style.colors.containerColor,
        tonalElevation = style.tonalElevation,
        shadowElevation = style.shadowElevation
    ) {
        Box(
            modifier = Modifier
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
