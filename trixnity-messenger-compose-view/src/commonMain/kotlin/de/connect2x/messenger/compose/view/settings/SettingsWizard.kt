package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNextButton.*
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.verification.DeviceVerificationStepSwitch
import de.connect2x.messenger.compose.view.verification.ShowPasspraseMethodContent
import de.connect2x.messenger.compose.view.verification.ShowRecoveryKeyMethodContent
import de.connect2x.messenger.compose.view.verification.ShowSelfVerificationMethodsContent
import de.connect2x.messenger.compose.view.verification.ShowVerificationHelpContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.verification.SelfVerificationMethod

private val WIZARD_EXPLANATION = "SETTINGS_WIZARD_EXPLANATION"
private val WIZARD_NOTIFICATION = "SETTINGS_WIZARD_NOTIFICATION"
private val WIZARD_CONFIRM = "SETTINGS_WIZARD_CONFIRM"
private val WIZARD_PRIVACY = "SETTINGS_WIZARD_PRIVACY"
private val WIZARD_VERIFICATION = "SETTINGS_WIZARD_VERIFICATION"

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
                                    Button(
                                        modifier = Modifier.buttonPointerModifier(),
                                        onClick = { showWizard.value = false }) {
                                        Text(i18n.commonSkip())
                                    }
                                }
                            )
                        )

                        is SettingsWizardRouter.Wrapper.NotificationSettings -> {
                            val viewModel = it.viewModel
                            add(
                                WizardStep(
                                    id = WIZARD_NOTIFICATION,
                                    title = { i18n.commonNotifications() },
                                    content = {
                                        val notificationSettings = viewModel.collectAsState().value
                                        if (notificationSettings != null) {
                                            val enabledOnDevice =
                                                notificationSettings.enabledForThisDevice.collectAsState().value
                                            Column {
                                                Setting(
                                                    text = i18n.notificationsSettingsEnabledForThisDevice(),
                                                    value = enabledOnDevice,
                                                    toggle = { notificationSettings.toggleEnabledForThisDevice() }
                                                )
                                                MiddleSpacer()
                                                PlatformNotificationSettings(notificationSettings)
                                                MiddleSpacer()
                                                PlatformNotificationAccountSettings(notificationSettings)
                                            }
                                        }
                                    },
                                )
                            )
                        }

                        is SettingsWizardRouter.Wrapper.WizardConfirm -> add(
                            WizardStep(
                                id = WIZARD_CONFIRM,
                                title = { i18n.settingsWizardFinishSetupTitle() },
                                content = {
                                    Text(i18n.settingsWizardFinishSetup())
                                },
                                nextButton = Custom {
                                    Button(
                                        modifier = Modifier.buttonPointerModifier(),
                                        onClick = { showWizard.value = false }) {
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
                                            val publicPresence = privacy.presenceIsPublic.collectAsState().value
                                            val publicTyping = privacy.typingIsPublic.collectAsState().value
                                            val publicRead = privacy.readMarkerIsPublic.collectAsState().value
                                            Column {
                                                Setting(
                                                    text = i18n.privacyPresenceIsPublic(),
                                                    explanation = i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName),
                                                    value = publicPresence,
                                                    toggle = { privacy.togglePresenceIsPublic() }
                                                )
                                                Setting(
                                                    text = i18n.privacyTypingIsPublic(),
                                                    explanation = i18n.privacyTypingIsPublicExplanation(),
                                                    value = publicTyping,
                                                    toggle = { privacy.toggleTypingIsPublic() })
                                                Setting(
                                                    text = i18n.privacyReadMarkerIsPublic(),
                                                    explanation = i18n.privacyReadMarkerIsPublicExplanation(),
                                                    value = publicRead,
                                                    toggle = { privacy.toggleReadMarkerIsPublic() })
                                            }
                                        }
                                    }
                                )
                            )
                        }

                        is SettingsWizardRouter.Wrapper.WizardVerification -> {
                            val selfVerificationStateFlow = it.selfVerificationViewModel
                            val wrapper = it
                            val verificationFlow = it.verificationViewModel
                            val selected = mutableStateOf<SelfVerificationMethod?>(null)
                            val selectedPassphrase = mutableStateOf<String>("")
                            val selectedRecoveryKey = mutableStateOf<String>("")
                            val startCrossDevice = mutableStateOf(false)
                            add(
                                WizardStep(
                                    id = WIZARD_VERIFICATION,
                                    title = { "Verification" },
                                    content = {
                                        Column {
                                            val selfVerification = selfVerificationStateFlow.collectAsState().value
                                            if (selfVerification != null) {
                                                val account = selfVerification.userId
                                                Text(account.toString())
                                                val showHelp =
                                                    selfVerification.showVerificationHelp.collectAsState().value
                                                val methods = selfVerification.selfVerificationMethods.collectAsState()
                                                val showPassphrase =
                                                    selfVerification.showPassphraseMethod.collectAsState().value != null
                                                val showKey =
                                                    selfVerification.showRecoveryKeyMethod.collectAsState().value != null
                                                val verification = verificationFlow.collectAsState().value

                                                when {
                                                    showHelp -> ShowVerificationHelpContent()
                                                    showPassphrase -> ShowPasspraseMethodContent(
                                                        selfVerification,
                                                        selectedPassphrase
                                                    )

                                                    showKey -> ShowRecoveryKeyMethodContent(
                                                        selfVerification,
                                                        selectedRecoveryKey
                                                    )

                                                    startCrossDevice.value -> {
                                                        if (verification != null) {
                                                            Box { DeviceVerificationStepSwitch(verification) }
                                                        }
                                                    }

                                                    else -> ShowSelfVerificationMethodsContent(methods, selected)
                                                }
                                            }
                                        }
                                    },
                                    additionalButton = {
                                        val selfVerification = selfVerificationStateFlow.collectAsState().value
                                        if (selfVerification != null) {
                                            val cancelVerification = remember { mutableStateOf(false) }
                                            val showHelp = selfVerification.showVerificationHelp.collectAsState().value
                                            val showPassphrase =
                                                selfVerification.showPassphraseMethod.collectAsState().value != null
                                            val showKey =
                                                selfVerification.showRecoveryKeyMethod.collectAsState().value != null
                                            val enableButton =
                                                showHelp
                                                        || (showPassphrase && selectedPassphrase.value.isNotBlank())
                                                        || (showKey && selectedRecoveryKey.value.isNotBlank())
                                                        || selected.value != null
                                            Button(
                                                modifier = Modifier.buttonPointerModifier(enableButton),
                                                enabled = enableButton,
                                                onClick = {
                                                    when {
                                                        showHelp -> {
                                                            selfVerification.waitForAvailableVerificationMethods()
                                                        }

                                                        showPassphrase -> {
                                                            selfVerification.verifyWithPassphrase(selectedPassphrase.value)
                                                        }

                                                        showKey -> {
                                                            selfVerification.verifyWithRecoveryKey(selectedRecoveryKey.value)
                                                        }


                                                        else -> {
                                                            val selectedMethod = selected.value
                                                            println("Method is $selectedMethod")
                                                            if (selected.value is SelfVerificationMethod.CrossSignedDeviceVerification) {
                                                                wrapper.startCrossSigning()
                                                                startCrossDevice.value = true
                                                            }
                                                            selected.value?.let { selfVerification.launchVerification(it) }
                                                        }
                                                    }
                                                }) {
                                                Text(i18n.commonNext())
                                            }
                                        }
                                        else Button(onClick = { wrapper.startVerification() }) {
                                            Text(i18n.commonNext())
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
