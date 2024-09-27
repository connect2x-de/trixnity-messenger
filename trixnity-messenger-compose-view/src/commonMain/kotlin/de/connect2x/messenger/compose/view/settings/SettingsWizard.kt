package de.connect2x.messenger.compose.view.settings

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNextButton
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.connecting.AdditionalConnectingWizardStep
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter

val WIZARD_EXPLANATION = "SETTINGS_WIZARD_EXPLANATION"
val WIZARD_NOTIFICATION = "SETTINGS_WIZARD_NOTIFICATION"

@Composable
fun SettingsWizard(wrapper: SettingsWizardRouter.Wrapper) {
    val di = DI.current
    val step = when (wrapper) {
        is SettingsWizardRouter.Wrapper.WizardExplanation -> WizardStep(
            id = WIZARD_EXPLANATION,
            title = { "Explanation" },
            content = { Text("Hier eine Erklärung") },
            nextButton = WizardNextButton.Custom {
                Button(modifier = Modifier.buttonPointerModifier(), onClick = { wrapper.onSwitchToNext() }) {
                    Text("Next")
                }
            }
        )
        is SettingsWizardRouter.Wrapper.NotificationSettings -> WizardStep(
            id = WIZARD_NOTIFICATION,
            title = { "Notification" },
            content = { Text("Hier die Notification-Settings") },
            nextButton = WizardNextButton.Custom {
                Button(modifier = Modifier.buttonPointerModifier(), onClick = { wrapper.onSwitchToNext() }) {
                    Text("Next")
                }
            }
        )

        else -> di.get<AdditionalConnectingWizardStep>().create(wrapper)
    }
    Wizard(listOf(step))

}
