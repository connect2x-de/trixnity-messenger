package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import de.connect2x.messenger.compose.view.common.modifier.customOnKeyEvent
import kotlinx.coroutines.launch

private val keys = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar)

@Composable
fun Modifier.customKeySelect(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    onSelect: () -> Unit,
): Modifier {
    val heldKeys = remember { mutableMapOf<Key, PressInteraction.Press>() }
    return customOnKeyEvent { event ->
        when {
            enabled && event.type == KeyEventType.KeyDown && keys.contains(event.key) -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                if (!heldKeys.containsKey(event.key)) {
                    val press = PressInteraction.Press(Offset.Unspecified)
                    heldKeys[event.key] = press
                    if (!interactionSource.tryEmit(press)) {
                        launch { interactionSource.emit(press) }
                    }
                    true
                } else {
                    false
                }
            }

            enabled && event.type == KeyEventType.KeyUp && keys.contains(event.key) -> {
                heldKeys.remove(event.key)?.let {
                    val release = PressInteraction.Release(it)
                    if (!interactionSource.tryEmit(release)) {
                        launch { interactionSource.emit(release) }
                    }
                }
                onSelect()
                true
            }

            else -> false
        }
    }
}
