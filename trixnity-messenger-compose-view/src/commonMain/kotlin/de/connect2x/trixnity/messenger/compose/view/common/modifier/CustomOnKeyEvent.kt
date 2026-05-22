package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.CoroutineScope

fun Modifier.customOnKeyEvent(onKeyEvent: CoroutineScope.(KeyEvent) -> Boolean): Modifier =
    this then CustomKeyInputElement(onKeyEvent = onKeyEvent, onPreKeyEvent = null)

fun Modifier.customOnPreviewKeyEvent(onPreviewKeyEvent: CoroutineScope.(KeyEvent) -> Boolean): Modifier =
    this then CustomKeyInputElement(onKeyEvent = null, onPreKeyEvent = onPreviewKeyEvent)

data class CustomKeyInputElement(
    val onKeyEvent: (CoroutineScope.(KeyEvent) -> Boolean)?,
    val onPreKeyEvent: (CoroutineScope.(KeyEvent) -> Boolean)?,
) : ModifierNodeElement<CustomKeyInputNode>() {
    override fun create() = CustomKeyInputNode(onKeyEvent, onPreKeyEvent)

    override fun update(node: CustomKeyInputNode) {
        node.onEvent = onKeyEvent
        node.onPreEvent = onPreKeyEvent
    }

    override fun InspectorInfo.inspectableProperties() {
        onKeyEvent?.let {
            name = "onKeyEvent"
            properties["onKeyEvent"] = it
        }
        onPreKeyEvent?.let {
            name = "onPreviewKeyEvent"
            properties["onPreviewKeyEvent"] = it
        }
    }
}

class CustomKeyInputNode(
    var onEvent: (CoroutineScope.(KeyEvent) -> Boolean)?,
    var onPreEvent: (CoroutineScope.(KeyEvent) -> Boolean)?,
) : KeyInputModifierNode, Modifier.Node() {
    override fun onKeyEvent(event: KeyEvent): Boolean = onEvent?.let { coroutineScope.it(event) } ?: false

    override fun onPreKeyEvent(event: KeyEvent): Boolean = onPreEvent?.let { coroutineScope.it(event) } ?: false
}
