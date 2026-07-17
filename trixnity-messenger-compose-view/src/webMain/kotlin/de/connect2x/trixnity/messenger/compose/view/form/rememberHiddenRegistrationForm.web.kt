package de.connect2x.trixnity.messenger.compose.view.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberHiddenRegistrationForm(uniqueId: String?): HiddenRegistrationForm {
    val hiddenRegistrationForm = remember { ReattachableHiddenRegistrationForm(uniqueId = uniqueId) }

    DisposableEffect(hiddenRegistrationForm) {
        hiddenRegistrationForm.attach()
        onDispose { hiddenRegistrationForm.detach() }
    }

    return hiddenRegistrationForm
}
