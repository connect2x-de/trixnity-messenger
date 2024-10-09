package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNextButton.*
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.verification.DeviceVerificationStepSwitch
import de.connect2x.messenger.compose.view.verification.ShowPasspraseMethodContent
import de.connect2x.messenger.compose.view.verification.ShowRecoveryKeyMethodContent
import de.connect2x.messenger.compose.view.verification.ShowSelfVerificationMethodsContent
import de.connect2x.messenger.compose.view.verification.ShowVerificationHelpContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardConfirm
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardPrivacySettings
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardVerification
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper
import net.folivo.trixnity.client.verification.SelfVerificationMethod

private val WIZARD_EXPLANATION = "SETTINGS_WIZARD_EXPLANATION"
private val WIZARD_NOTIFICATION = "SETTINGS_WIZARD_NOTIFICATION"
private val WIZARD_CONFIRM = "SETTINGS_WIZARD_CONFIRM"
private val WIZARD_PRIVACY = "SETTINGS_WIZARD_PRIVACY"
private val WIZARD_VERIFICATION = "SETTINGS_WIZARD_VERIFICATION"

interface AdditionalSettingsWizardStep {
    fun <T : Wrapper> create(wrapper: T): WizardStep
}

class AdditionalSettingsWizardStepImpl() : AdditionalSettingsWizardStep {
    override fun <T : Wrapper> create(wrapper: T): WizardStep {
        throw IllegalArgumentException("Creating a SettingsWizard step with ${wrapper::class} is unsupported and requires an implementation")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWizard(list: List<Wrapper>) {
    val di = DI.current
    val i18n = di.get<I18nView>()

    val steps = remember {
        mutableListOf<WizardStep>().apply {
            list.forEach {
                when (it) {
                    is WizardExplanation -> add(wizardStepExplanation(it, i18n))

                    is WizardNotificationSettings -> add(wizardStepNotification(it, i18n))

                    is WizardConfirm -> add(wizardStepConfirmation(it, i18n))

                    is WizardPrivacySettings -> add(wizardStepPrivacy(it, i18n))

                    is WizardVerification -> add(wizardStepVerification(it, i18n))

                    Wrapper.None -> {}
                    else -> add(di.get<AdditionalSettingsWizardStep>().create(it))
                }
            }
        }
    }
    Wizard(steps)
}

private fun wizardStepExplanation(wrapper: WizardExplanation, i18n: I18nView): WizardStep {
    return WizardStep(id = WIZARD_EXPLANATION,
        title = { "${i18n.commonWelcome()} ${wrapper.userId.collectAsState().value?.localpart}" },
        content = { Text(i18n.settingsWizardExplanationMessage()) },
        additionalButton = {
            Button(modifier = Modifier.buttonPointerModifier(), onClick = { wrapper.onWizardClose() }) {
                Text(i18n.commonSkip())
            }
        })
}

private fun wizardStepNotification(wrapper: WizardNotificationSettings, i18n: I18nView): WizardStep {
    val viewModel = wrapper.viewModel
    return WizardStep(
        id = WIZARD_NOTIFICATION,
        title = { i18n.commonNotifications() },
        content = {
            val notificationSettings = viewModel.collectAsState().value
            if (notificationSettings != null) {
                val enabledOnDevice = notificationSettings.enabledForThisDevice.collectAsState().value
                Column {
                    Setting(text = i18n.notificationsSettingsEnabledForThisDevice(),
                        value = enabledOnDevice,
                        toggle = { notificationSettings.toggleEnabledForThisDevice() })
                    MiddleSpacer()
                    PlatformNotificationSettings(notificationSettings)
                    MiddleSpacer()
                    PlatformNotificationAccountSettings(notificationSettings)
                }
            }
        },
    )
}

private fun wizardStepConfirmation(wrapper: WizardConfirm, i18n: I18nView): WizardStep {
    val wrapper = wrapper
    return WizardStep(id = WIZARD_CONFIRM, title = { i18n.settingsWizardFinishSetupTitle() }, content = {
        Text(i18n.settingsWizardFinishSetup())
    }, nextButton = Custom {
        Button(modifier = Modifier.buttonPointerModifier(), onClick = { wrapper.onWizardClose() }) {
            Text(i18n.commonConfirm())
        }
    })
}

private fun wizardStepPrivacy(wrapper: WizardPrivacySettings, i18n: I18nView): WizardStep {
    val viewModel = wrapper.viewModel
    return WizardStep(id = WIZARD_PRIVACY, title = { i18n.privacyTitle() }, content = {
        val privacy = viewModel.collectAsState().value
        if (privacy != null) {
            val di = DI.current
            val publicPresence = privacy.presenceIsPublic.collectAsState().value
            val publicTyping = privacy.typingIsPublic.collectAsState().value
            val publicRead = privacy.readMarkerIsPublic.collectAsState().value
            Column {
                Setting(text = i18n.privacyPresenceIsPublic(),
                    explanation = i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName),
                    value = publicPresence,
                    toggle = { privacy.togglePresenceIsPublic() })
                Setting(text = i18n.privacyTypingIsPublic(),
                    explanation = i18n.privacyTypingIsPublicExplanation(),
                    value = publicTyping,
                    toggle = { privacy.toggleTypingIsPublic() })
                Setting(text = i18n.privacyReadMarkerIsPublic(),
                    explanation = i18n.privacyReadMarkerIsPublicExplanation(),
                    value = publicRead,
                    toggle = { privacy.toggleReadMarkerIsPublic() })
            }
        }
    })
}

private fun wizardStepVerification(wrapper: WizardVerification, i18n: I18nView): WizardStep {
    val selfVerificationStateFlow = wrapper.selfVerificationViewModel
    val verificationFlow = wrapper.verificationViewModel
    val isVerified = wrapper.isVerified
    val selected = mutableStateOf<SelfVerificationMethod?>(null)
    val selectedPassphrase = mutableStateOf<String>("")
    val selectedRecoveryKey = mutableStateOf<String>("")
    val startCrossDevice = mutableStateOf(false)
    return WizardStep(id = WIZARD_VERIFICATION, title = { "Verification" }, content = {
        Column {
            val isVerified = isVerified.collectAsState().value
            val selfVerification = selfVerificationStateFlow.collectAsState().value
            if (selfVerification != null && !isVerified) {
                val account = selfVerification.userId
                Text(account.toString())
                val showHelp = selfVerification.showVerificationHelp.collectAsState().value
                val methods = selfVerification.selfVerificationMethods.collectAsState()
                val showPassphrase = selfVerification.showPassphraseMethod.collectAsState().value != null
                val showKey = selfVerification.showRecoveryKeyMethod.collectAsState().value != null
                val verification = verificationFlow.collectAsState().value

                when {
                    showHelp -> ShowVerificationHelpContent()
                    showPassphrase -> ShowPasspraseMethodContent(
                        selfVerification, selectedPassphrase
                    )

                    showKey -> ShowRecoveryKeyMethodContent(
                        selfVerification, selectedRecoveryKey
                    )

                    startCrossDevice.value -> {
                        if (verification != null) {
                            Box { DeviceVerificationStepSwitch(verification) }
                        }
                    }

                    else -> ShowSelfVerificationMethodsContent(methods, selected)
                }
            } else if (isVerified) {
                Column {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(50.dp),
                        contentDescription = i18n.commonSuccess(),
                        tint = MaterialTheme.messengerColors.success
                    )
                    SmallSpacer()
                    Text(i18n.verificationVerifiedDevice())

                }
            } else {
                val verificationStarted = remember { mutableStateOf(false) }
                if (!verificationStarted.value) {
                    wrapper.startVerification()
                    verificationStarted.value = true
                }
            }
        }
    }, additionalButton = {
        val selfVerification = selfVerificationStateFlow.collectAsState().value
        val isVerified = isVerified.collectAsState().value
        if (selfVerification != null && !isVerified) {
            val showHelp = selfVerification.showVerificationHelp.collectAsState().value
            val showPassphrase = selfVerification.showPassphraseMethod.collectAsState().value != null
            val showKey = selfVerification.showRecoveryKeyMethod.collectAsState().value != null
            val enableButton =
                showHelp || (showPassphrase && selectedPassphrase.value.isNotBlank()) || (showKey && selectedRecoveryKey.value.isNotBlank()) || selected.value != null
            Button(modifier = Modifier.buttonPointerModifier(enableButton), enabled = enableButton, onClick = {
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
    })
}
