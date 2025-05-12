package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement

@Composable
fun Modifier.justIndication(interactionSource: InteractionSource, ripple: IndicationNodeFactory = ripple(bounded = true)) =
    then(CustomIndicationElement(interactionSource, ripple))

data class CustomIndicationElement(
    val interactionSource: InteractionSource,
    val factory: IndicationNodeFactory,
): ModifierNodeElement<CustomIndicationNode>() {
    override fun create() = CustomIndicationNode(factory.create(interactionSource))
    override fun update(node: CustomIndicationNode) = node.update(factory.create(interactionSource))
}

data class CustomIndicationNode(
    private var delegate: DelegatableNode
): DelegatingNode() {
    init {
        delegate(delegate)
    }

    fun update(delegate: DelegatableNode) {
        undelegate(this.delegate)
        this.delegate = delegate
        delegate(delegate)
    }
}
