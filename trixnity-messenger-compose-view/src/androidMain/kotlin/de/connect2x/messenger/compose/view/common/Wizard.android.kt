package de.connect2x.messenger.compose.view.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
actual fun WizardScope.handleBackPresses(enabled: MutableState<Boolean>) {
    println("BackPress is ${enabled.value}")
    if (enabled.value) {
        BackHandler(enabled.value) {
            previousStep?.let { currentStepId.value = it }
        }
    }
}
