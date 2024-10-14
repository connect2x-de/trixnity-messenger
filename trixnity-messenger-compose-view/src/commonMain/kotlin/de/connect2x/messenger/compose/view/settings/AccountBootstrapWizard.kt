package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import de.connect2x.messenger.compose.view.common.WizardButtons
import de.connect2x.messenger.compose.view.common.WizardNavigationButton.*
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.verification.DeviceVerificationStepSwitch
import de.connect2x.messenger.compose.view.verification.ShowPasspraseMethodContent
import de.connect2x.messenger.compose.view.verification.ShowRecoveryKeyMethodContent
import de.connect2x.messenger.compose.view.verification.ShowSelfVerificationMethodsContent
import de.connect2x.messenger.compose.view.verification.ShowVerificationHelpContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardConfirm
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardPrivacySettings
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardVerification
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingRouter.Wrapper
import net.folivo.trixnity.client.verification.SelfVerificationMethod

private val WIZARD_EXPLANATION = "ACCOUNT_BOOTSTRAP_WIZARD_EXPLANATION"
private val WIZARD_NOTIFICATION = "ACCOUNT_BOOTSTRAP_WIZARD_NOTIFICATION"
private val WIZARD_CONFIRM = "ACCOUNT_BOOTSTRAP_WIZARD_CONFIRM"
private val WIZARD_PRIVACY = "ACCOUNT_BOOTSTRAP_WIZARD_PRIVACY"
private val WIZARD_VERIFICATION = "ACCOUNT_BOOTSTRAP_WIZARD_VERIFICATION"

interface AdditionalAccountBootstrappingWizardStep {
    fun <T : Wrapper> create(wrapper: T): WizardStep
}

class AdditionalAccountBootstrappingWizardStepImpl() : AdditionalAccountBootstrappingWizardStep {
    override fun <T : Wrapper> create(wrapper: T): WizardStep {
        throw IllegalArgumentException("Creating an AccountBootstrappingWizard step with ${wrapper::class} is unsupported and requires an implementation")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBootstrappingWizard(showBootstrapWrapper: Wrapper.ShowBootstrap) {
    val di = DI.current
    val i18n = di.get<I18nView>()

    val list = showBootstrapWrapper.viewModel.steps
    val steps = remember {
        mutableListOf<WizardStep>().apply {
            list.forEach {
                when (it) {
                    is WizardExplanation -> add(wizardStepExplanation(
                        it, i18n
                    ) { showBootstrapWrapper.viewModel.closeWizard() })

                    is WizardNotificationSettings -> add(wizardStepNotification(it, i18n))

                    is WizardConfirm -> add(wizardStepConfirmation(
                        i18n
                    ) { showBootstrapWrapper.viewModel.closeWizard() })

                    is WizardPrivacySettings -> add(wizardStepPrivacy(it, i18n))

                    is WizardVerification -> add(wizardStepVerification(it, i18n))

                    Wrapper.None -> {}

                    else -> add(di.get<AdditionalAccountBootstrappingWizardStep>().create(it))
                }
            }
        }
    }
    Wizard(steps)
}

private fun wizardStepExplanation(wrapper: WizardExplanation, i18n: I18nView, closeWizard: () -> Unit): WizardStep {
    return WizardStep(id = WIZARD_EXPLANATION,
        title = { "${i18n.commonWelcome()} ${wrapper.userId.localpart}" },
        content = { Text(i18n.accountBootstrappingWizardExplanationMessage()) },
        additionalButton = {
            Button(modifier = Modifier.buttonPointerModifier(), onClick = { closeWizard() }) {
                Text(i18n.commonSkip())
            }
        })
}

private fun wizardStepNotification(wrapper: WizardNotificationSettings, i18n: I18nView): WizardStep {
    val notificationSettings = wrapper.viewModel
    return WizardStep(
        id = WIZARD_NOTIFICATION,
        title = { i18n.commonNotifications() },
        content = {
            val enabledOnDevice = notificationSettings.enabledForThisDevice.collectAsState().value
            Column {
                Setting(text = i18n.notificationsSettingsEnabledForThisDevice(),
                    value = enabledOnDevice,
                    toggle = { notificationSettings.toggleEnabledForThisDevice() })
                MiddleSpacer()
                PlatformNotificationSettings(notificationSettings, enabledOnDevice)
                MiddleSpacer()
                PlatformNotificationAccountSettings(notificationSettings, enabledOnDevice)
            }
        },
    )
}

private fun wizardStepConfirmation(i18n: I18nView, closeWizard: () -> Unit): WizardStep {
    return WizardStep(id = WIZARD_CONFIRM, title = { i18n.accountBootstrappingWizardFinishSetupTitle() }, content = {
        Text(i18n.accountBootstrappingWizardFinishSetup())
    }, nextButton = {
        Custom {
            Button(modifier = Modifier.buttonPointerModifier(), onClick = { closeWizard() }) {
                Text(i18n.commonConfirm())
            }
        }
    })
}

private fun wizardStepPrivacy(wrapper: WizardPrivacySettings, i18n: I18nView): WizardStep {
    val privacySettings = wrapper.viewModel
    return WizardStep(id = WIZARD_PRIVACY, title = { i18n.privacyTitle() }, content = {
        val di = DI.current
        val publicPresence = privacySettings.presenceIsPublic.collectAsState().value
        val publicTyping = privacySettings.typingIsPublic.collectAsState().value
        val publicRead = privacySettings.readMarkerIsPublic.collectAsState().value
        Column {
            Setting(text = i18n.privacyPresenceIsPublic(),
                explanation = i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName),
                value = publicPresence,
                toggle = { privacySettings.togglePresenceIsPublic() })
            Setting(text = i18n.privacyReadMarkerIsPublic(),
                explanation = i18n.privacyReadMarkerIsPublicExplanation(),
                value = publicRead,
                toggle = { privacySettings.toggleReadMarkerIsPublic() })
            Setting(text = i18n.privacyTypingIsPublic(),
                explanation = i18n.privacyTypingIsPublicExplanation(),
                value = publicTyping,
                toggle = { privacySettings.toggleTypingIsPublic() })
        }
    })
}

private fun wizardStepVerification(wrapper: WizardVerification, i18n: I18nView): WizardStep {
    val selfVerification = wrapper.selfVerificationViewModel
    val verification = wrapper.verificationViewModel
    val isVerified = wrapper.isVerified
    val selected = mutableStateOf<SelfVerificationMethod?>(null)
    val selectedPassphrase = mutableStateOf<String>("")
    val selectedRecoveryKey = mutableStateOf<String>("")
    val startCrossDevice = mutableStateOf(false)
    return WizardStep(id = WIZARD_VERIFICATION, title = { i18n.deviceVerification() }, content = {
        Column {
            val isVerified = isVerified.collectAsState().value
            if (isVerified == false) {
                val showHelp = selfVerification.showVerificationHelp.collectAsState().value
                val methods = selfVerification.selfVerificationMethods.collectAsState()
                val showPassphrase = selfVerification.showPassphraseMethod.collectAsState().value != null
                val showKey = selfVerification.showRecoveryKeyMethod.collectAsState().value != null

                when {
                    showHelp -> ShowVerificationHelpContent()
                    showPassphrase -> ShowPasspraseMethodContent(
                        selfVerification, selectedPassphrase
                    )

                    showKey -> ShowRecoveryKeyMethodContent(
                        selfVerification, selectedRecoveryKey
                    )

                    startCrossDevice.value -> {
                        Box { DeviceVerificationStepSwitch(verification) }
                    }

                    else -> ShowSelfVerificationMethodsContent(methods, selected)
                }
            } else if (isVerified == true) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(50.dp),
                        contentDescription = i18n.commonSuccess(),
                        tint = MaterialTheme.messengerColors.success
                    )
                    SmallSpacer()
                    Text(i18n.verificationSucessThisDevice())

                }
            } else if (isVerified == false) {
                val verificationStarted = remember { mutableStateOf(false) }
                if (!verificationStarted.value) {
                    selected.value?.let { wrapper.startVerification(it) }
                    verificationStarted.value = true
                }
            }
        }
    }, additionalButton = {
        val isVerified = isVerified.collectAsState().value
        if (isVerified == false) {
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
                        selected.value?.let { wrapper.startVerification(it) }
                    }
                }
            }) {
                Text(i18n.commonNext())
            }
        }
    }, buttonOrder = {
        if (isVerified.collectAsState().value == true) Triple(
            WizardButtons.AdditionalButton, WizardButtons.BackButton, WizardButtons.NextButton
        ) else Triple(
            WizardButtons.NextButton, WizardButtons.BackButton, WizardButtons.AdditionalButton
        )
    }, nextButton = {
        Standard(content = {
            if (isVerified.collectAsState().value == true) {
                Text(i18n.commonNext())
            } else {
                Text(i18n.commonSkip())
            }
        })
    }, backButton = {
        val showPassphrase = selfVerification.showPassphraseMethod.collectAsState().value != null
        val showKey = selfVerification.showRecoveryKeyMethod.collectAsState().value != null

        if (showPassphrase || showKey) {
            Custom(button = {
                Button(onClick = {
                    selfVerification.backToChoose()
                }) {
                    Text(i18n.commonBack())
                }
            })
        }
        else Standard()
    })
}
