package de.connect2x.trixnity.messenger.compose.view.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberHiddenAutofillForm(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    interactionSource: MutableInteractionSource,
    uniqueId: String?,
): HiddenAutofillForm {
    return NoopHiddenAutofillForm
}
