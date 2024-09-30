package de.connect2x.messenger.compose.view.common

import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput


infix fun Modifier.thenNullable(other: Modifier?): Modifier =
    if (other === Modifier) this else CombinedModifier(this, other ?: Modifier)

fun Modifier.blockPointerInput(): Modifier =
    this.pointerInput(Unit) {}

fun Modifier.gesturesDisabled(disabled: Boolean = true) =
    if (disabled) {
        pointerInput(Unit) {
            awaitPointerEventScope {
                // we should wait for all new pointer events
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                        .changes
                        .forEach(PointerInputChange::consume)
                }
            }
        }
    } else {
        this
    }
