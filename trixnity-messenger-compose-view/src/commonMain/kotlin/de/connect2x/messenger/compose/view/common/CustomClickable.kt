package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import de.connect2x.messenger.compose.view.buttonPointerModifier

@Composable
fun Modifier.customClickable(
    focusRequester: FocusRequester = remember { FocusRequester() },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: IndicationNodeFactory = ripple(bounded = true),
    enabled: Boolean = true,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = focusRequester(focusRequester)
    .focusable(enabled, interactionSource)
    .hoverable(interactionSource, enabled)
    .semantics {
        role?.let { this.role = it }
        this.onClick(onClickLabel) { onClick(); true }
        onLongClick?.let { this.onLongClick(onLongClickLabel) { it(); true } }
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = if (enabled && onDoubleClick != null) {
                { focusRequester.requestFocus(); onDoubleClick() }
            } else null,
            onLongPress = if (enabled && onLongClick != null) {
                { focusRequester.requestFocus(); onLongClick() }
            } else null,
            onPress = { offset ->
                if (enabled) {
                    focusRequester.requestFocus()
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    val endInteraction = if (tryAwaitRelease()) {
                        PressInteraction.Release(press)
                    } else {
                        PressInteraction.Cancel(press)
                    }
                    interactionSource.emit(endInteraction)
                }
            },
            onTap = {
                if (enabled) {
                    onClick()
                }
            }
        )
    }
    .customKeySelect(interactionSource, enabled, onClick)
    .customIndication(interactionSource, indication)
    .buttonPointerModifier(enabled)
