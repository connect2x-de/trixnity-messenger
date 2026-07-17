package de.connect2x.trixnity.messenger.compose.view.form

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal actual fun rememberHiddenAutofillForm(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    interactionSource: MutableInteractionSource,
    uniqueId: String?,
): HiddenAutofillForm {
    val interactionForwarder = rememberInteractionForwarder(interactionSource)
    val currentOnUsernameChange = rememberUpdatedState(onUsernameChange)
    val currentOnPasswordChange = rememberUpdatedState(onPasswordChange)
    val currentOnPointerEvent = rememberUpdatedState(interactionForwarder)

    val hiddenAutofillForm = remember {
        ReattachableHiddenAutofillForm(
            onUsernameChange = { currentOnUsernameChange.value(it) },
            onPasswordChange = { currentOnPasswordChange.value(it) },
            onPointerEvent = { currentOnPointerEvent.value.forward(it) },
            uniqueId = uniqueId,
        )
    }

    DisposableEffect(hiddenAutofillForm) {
        hiddenAutofillForm.attach()
        onDispose { hiddenAutofillForm.detach() }
    }

    return hiddenAutofillForm
}

@Composable
private fun rememberInteractionForwarder(interactionSource: MutableInteractionSource): InteractionForwarder {
    val coroutineScope = rememberCoroutineScope()
    val interactionForwarder =
        remember(coroutineScope, interactionSource) {
            InteractionForwarderImpl(coroutineScope = coroutineScope, interactionSource = interactionSource)
        }

    return interactionForwarder
}

private interface InteractionForwarder {
    fun forward(event: AutofillPointerEvent)
}

private class InteractionForwarderImpl(
    private val coroutineScope: CoroutineScope,
    private val interactionSource: MutableInteractionSource,
) : InteractionForwarder {

    private var currentEnter: HoverInteraction.Enter? = null
    private var currentPress: PressInteraction.Press? = null

    override fun forward(event: AutofillPointerEvent) {
        when (event) {
            is AutofillPointerEvent.HoverEnter -> enter()
            is AutofillPointerEvent.HoverExit -> exit()
            is AutofillPointerEvent.Press -> press(event)
            is AutofillPointerEvent.Release -> release()
            is AutofillPointerEvent.Cancel -> cancel()
        }
    }

    private fun enter() {
        val interaction = HoverInteraction.Enter().also { currentEnter = it }
        coroutineScope.launch { interactionSource.emit(interaction) }
    }

    private fun exit() {
        val enter = currentEnter?.also { currentEnter = null } ?: return
        coroutineScope.launch { interactionSource.emit(HoverInteraction.Exit(enter)) }
    }

    private fun press(event: AutofillPointerEvent.Press) {
        val interaction = PressInteraction.Press(Offset(event.x, event.y)).also { currentPress = it }
        coroutineScope.launch { interactionSource.emit(interaction) }
    }

    private fun release() {
        val press = currentPress?.also { currentPress = null } ?: return
        coroutineScope.launch { interactionSource.emit(PressInteraction.Release(press)) }
    }

    private fun cancel() {
        val press = currentPress?.also { currentPress = null } ?: return
        coroutineScope.launch { interactionSource.emit(PressInteraction.Cancel(press)) }
    }
}
