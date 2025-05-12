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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import de.connect2x.messenger.compose.view.buttonPointerModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val keys = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar)

@Composable
fun Modifier.justClickable(
    focusRequester: FocusRequester = remember { FocusRequester() },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: IndicationNodeFactory = ripple(bounded = true),
    indicationScope: CoroutineScope = rememberCoroutineScope(),
    enabled: Boolean = true,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
) = focusRequester(focusRequester)
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
        detectTapGestures(
            onPress = { offset ->
                if (enabled) {
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    focusRequester.requestFocus()
                    val released = tryAwaitRelease()
                    interactionSource.emit(PressInteraction.Release(press))
                    if (released) onClick()
                }
            },
        )
    }
    .onKeyEvent { event ->
        val currentKeyPressInteractions = mutableMapOf<Key, PressInteraction.Press>()
        when {
            enabled && event.type == KeyEventType.KeyDown && keys.contains(event.key) -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                if (!currentKeyPressInteractions.containsKey(event.key)) {
                    val press = PressInteraction.Press(Offset.Unspecified)
                    currentKeyPressInteractions[event.key] = press
                    indicationScope.launch {
                        interactionSource.emit(press)
                    }
                    true
                } else {
                    false
                }
            }
            enabled && event.type == KeyEventType.KeyUp && keys.contains(event.key) -> {
                currentKeyPressInteractions.remove(event.key)?.let {
                    indicationScope.launch {
                        interactionSource.emit(PressInteraction.Release(it))
                    }
                }
                onClick()
                true
            }
            else -> false
        }
    }
    .justIndication(interactionSource, indication)
    .buttonPointerModifier(enabled)
