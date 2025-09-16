package de.connect2x.messenger.compose.view.common.modifier

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration


@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.tooltipGestures(
    enabled: Boolean,
    state: TooltipState,
    longPressDelay: Duration,
    showTooltip: () -> Unit,
    hideTooltip: () -> Unit,
): Modifier =
    if (enabled) {
        pointerInput(state) {
            coroutineScope {
                awaitEachGesture {
                    // Long press will finish before or after show so keep track of it, in a
                    // flow to handle both cases
                    val isLongPressedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
                    val longPressTimeout = longPressDelay
                    val pass = PointerEventPass.Initial

                    // wait for the first down press
                    val inputType = awaitFirstDown(pass = pass).type

                    if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                        try {
                            // listen to if there is up gesture
                            // within the longPressTimeout limit
                            withTimeout(longPressTimeout.inWholeMilliseconds) {
                                waitForUpOrCancellation(pass = pass)
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            // handle long press - Show the tooltip
                            launch(start = CoroutineStart.UNDISPATCHED) {
                                try {
                                    isLongPressedFlow.tryEmit(true)
                                    state.show(MutatePriority.PreventUserInput)
                                } finally {
                                    isLongPressedFlow.collectLatest { isLongPressed ->
                                        if (!isLongPressed) {
                                            state.dismiss()
                                        }
                                    }
                                }
                            }

                            // consume the children's click handling
                            // Long press may still be in progress
                            val upEvent = waitForUpOrCancellation(pass = pass)
                            upEvent?.consume()
                        } finally {
                            isLongPressedFlow.tryEmit(false)
                        }
                    }
                }
            }
        }
            .pointerInput(state) {
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
            }
    } else this

@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.tooltipAnchorSemantics(
    label: String,
    enabled: Boolean,
    state: TooltipState,
    scope: CoroutineScope
): Modifier =
    if (enabled) {
        this.semantics(mergeDescendants = true) {
            onLongClick(
                label = label,
                action = {
                    scope.launch { state.show() }
                    true
                }
            )
        }
    } else this
