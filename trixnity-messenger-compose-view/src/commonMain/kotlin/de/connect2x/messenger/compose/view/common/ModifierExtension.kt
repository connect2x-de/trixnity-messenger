package de.connect2x.messenger.compose.view.common

import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput


infix fun Modifier.thenNullable(other: Modifier?): Modifier =
    if (other === Modifier) this else CombinedModifier(this, other ?: Modifier)

fun Modifier.blockPointerInput(): Modifier =
    this.pointerInput(Unit) {}
