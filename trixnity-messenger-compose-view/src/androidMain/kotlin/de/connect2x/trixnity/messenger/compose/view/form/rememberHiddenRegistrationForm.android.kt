package de.connect2x.trixnity.messenger.compose.view.form

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberHiddenRegistrationForm(uniqueId: String?): HiddenRegistrationForm {
    return NoopHiddenRegistrationForm
}
