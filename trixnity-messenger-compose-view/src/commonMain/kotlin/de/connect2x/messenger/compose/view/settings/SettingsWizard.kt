package de.connect2x.messenger.compose.view.settings

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNextButton
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter
import io.github.oshai.kotlinlogging.KotlinLogging

val WIZARD_EXPLANATION = "SETTINGS_WIZARD_EXPLANATION"
val WIZARD_NOTIFICATION = "SETTINGS_WIZARD_NOTIFICATION"
val WIZARD_CONFIRM = "SETTINGS_WIZARD_CONFIRM"

private val log = KotlinLogging.logger { }

@Composable
fun SettingsWizard(list: List<SettingsWizardRouter.Wrapper>) {
    val di = DI.current
    val showWizard = remember { mutableStateOf(true) }

    if (showWizard.value) {
        val steps = remember {
            mutableListOf<WizardStep>().apply {
                list.forEach {
                    when (it) {
                        is SettingsWizardRouter.Wrapper.WizardExplanation -> add(
                            WizardStep(
                                id = WIZARD_EXPLANATION,
                                title = { "Explanation" },
                                content = { Text("Hier eine Erklärung") },
                            )
                        )

                        is SettingsWizardRouter.Wrapper.NotificationSettings -> add(
                            WizardStep(
                                id = WIZARD_NOTIFICATION,
                                title = { "Notification" },
                                content = { Text("Hier die Notification-Settings") },
                            )
                        )

                        is SettingsWizardRouter.Wrapper.WizardConfirm -> add(
                            WizardStep(
                                id = WIZARD_CONFIRM,
                                title = { "Confirm" },
                                content = {
                                    Text("Wizard beenden")
                                },
                                nextButton = WizardNextButton.Custom {
                                    Button(onClick = { showWizard.value = false }) {
                                        Text("Close Wizard")
                                    }
                                }
                            )
                        )

                        SettingsWizardRouter.Wrapper.None -> {}
                    }
                }
            }
        }
        log.debug { "Wizard steps are ${steps.size}" }
        Wizard(steps)
    }

}
