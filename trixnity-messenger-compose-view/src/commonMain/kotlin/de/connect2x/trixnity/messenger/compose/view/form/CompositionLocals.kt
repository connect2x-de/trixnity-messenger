package de.connect2x.trixnity.messenger.compose.view.form

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

internal val LocalHiddenAutofillForm: ProvidableCompositionLocal<HiddenAutofillForm> = compositionLocalOf {
    NoopHiddenAutofillForm
}

internal val LocalHiddenRegistrationForm: ProvidableCompositionLocal<HiddenRegistrationForm> = compositionLocalOf {
    NoopHiddenRegistrationForm
}
