package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNextButton.*
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter
import io.github.oshai.kotlinlogging.KotlinLogging

private val WIZARD_EXPLANATION = "SETTINGS_WIZARD_EXPLANATION"
private val WIZARD_NOTIFICATION = "SETTINGS_WIZARD_NOTIFICATION"
private val WIZARD_CONFIRM = "SETTINGS_WIZARD_CONFIRM"
private val WIZARD_PRIVACY = "SETTINGS_WIZARD_PRIVACY"

private val log = KotlinLogging.logger { }

@Composable
fun SettingsWizard(list: List<SettingsWizardRouter.Wrapper>) {
    val di = DI.current
    val showWizard = remember { mutableStateOf(true) }
    val i18n = di.get<I18nView>()

    if (showWizard.value) {
        val steps = remember {
            mutableListOf<WizardStep>().apply {
                list.forEach {
                    when (it) {
                        is SettingsWizardRouter.Wrapper.WizardExplanation -> add(
                            WizardStep(
                                id = WIZARD_EXPLANATION,
                                title = { "${i18n.commonWelcome()} ${it.userId.localpart}" },
                                content = { Text(i18n.settingsWizardExplanationMessage()) },
                                additionalButton = {
                                    Button(onClick = { showWizard.value = false }) {
                                        Text(i18n.commonClose())
                                    }
                                }
                            )
                        )

                        is SettingsWizardRouter.Wrapper.NotificationSettings -> add(
                            WizardStep(
                                id = WIZARD_NOTIFICATION,
                                title = { i18n.commonNotifications() },
                                content = { Text("Hier die Notification-Settings") },
                            )
                        )

                        is SettingsWizardRouter.Wrapper.WizardConfirm -> add(
                            WizardStep(
                                id = WIZARD_CONFIRM,
                                title = { i18n.settingsWizardFinishSetupTitle() },
                                content = {
                                    Text(i18n.settingsWizardFinishSetup())
                                },
                                nextButton = Custom {
                                    Button(onClick = { showWizard.value = false }) {
                                        Text(i18n.commonConfirm())
                                    }
                                }
                            )
                        )

                        SettingsWizardRouter.Wrapper.None -> {}
                        is SettingsWizardRouter.Wrapper.PrivacySettings -> {
                            val viewModel = it.viewModel
                            add(
                                WizardStep(
                                    id = WIZARD_PRIVACY,
                                    title = { i18n.privacyTitle() },
                                    content = {
                                        val privacy = viewModel.collectAsState().value
                                        if (privacy != null) {
                                            Column {
                                                Setting(
                                                    text = i18n.privacyPresenceIsPublic(),
                                                    explanation = i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName),
                                                    value = privacy.presenceIsPublic.collectAsState().value,
                                                    toggle = { privacy.togglePresenceIsPublic() }
                                                )
                                                Setting(
                                                    text = i18n.privacyTypingIsPublic(),
                                                    explanation = i18n.privacyTypingIsPublicExplanation(),
                                                    value = privacy.typingIsPublic.collectAsState().value,
                                                    toggle = { privacy.toggleTypingIsPublic() })
                                                Setting(
                                                    text = i18n.privacyReadMarkerIsPublic(),
                                                    explanation = i18n.privacyReadMarkerIsPublicExplanation(),
                                                    value = privacy.readMarkerIsPublic.collectAsState().value,
                                                    toggle = { privacy.toggleReadMarkerIsPublic() })
                                            }
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
        Wizard(steps)
    }

}
