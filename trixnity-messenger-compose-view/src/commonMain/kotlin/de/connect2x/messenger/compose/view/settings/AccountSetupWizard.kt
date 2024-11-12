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
import androidx.compose.material3.OutlinedButton
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
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton.*
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.verification.DeviceVerificationStepSwitch
import de.connect2x.messenger.compose.view.verification.SelfVerificationMethodsListEntries
import de.connect2x.messenger.compose.view.verification.ShowPassphraseMethodContent
import de.connect2x.messenger.compose.view.verification.ShowRecoveryKeyMethodContent
import de.connect2x.messenger.compose.view.verification.ShowResetRecoveryWarningContent
import de.connect2x.messenger.compose.view.verification.ShowSelfVerificationMethodsContent
import de.connect2x.messenger.compose.view.verification.ShowVerificationHelpContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel
import net.folivo.trixnity.client.verification.SelfVerificationMethod


open class AccountSetupWizardStep(val stepId: String) {
    data object ExplanationStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_EXPLANATION")
    data object VerificationStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_VERIFICATION")
    data object PrivacySettingsStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_PRIVACY")
    data object NotificationSettingsStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_NOTIFICATION")
    data object ConfirmationStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_CONFIRM")
}

interface AccountSetupWizardStepList {
    val steps: List<AccountSetupWizardStep>
}

class AccountSetupWizardStepListImpl : AccountSetupWizardStepList {
    override val steps = listOf(
        AccountSetupWizardStep.ExplanationStep,
        AccountSetupWizardStep.VerificationStep,
        AccountSetupWizardStep.PrivacySettingsStep,
        AccountSetupWizardStep.NotificationSettingsStep,
        AccountSetupWizardStep.ConfirmationStep
    )
}


interface AdditionalAccountSetupWizardStep {
    fun <T : Any> create(viewModel: T, step: AccountSetupWizardStep): WizardStep
}

class AdditionalAccountSetupWizardStepImpl() : AdditionalAccountSetupWizardStep {
    override fun <T : Any> create(
        viewModel: T,
        step: AccountSetupWizardStep
    ): WizardStep {
        throw IllegalArgumentException("Creating an AccountSetupWizard step with ${step::class} is unsupported and requires an implementation")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetupWizard(showAccountBootstrapWrapper: Wrapper.ShowAccountSetup) {
    val di = DI.current
    val i18n = di.get<I18nView>()

    val viewModel = showAccountBootstrapWrapper.viewModel
    val list = di.get<AccountSetupWizardStepList>().steps
    val steps = remember {
        mutableListOf<WizardStep>().apply {
            list.forEach {
                when (it) {
                    is AccountSetupWizardStep.ExplanationStep -> add(wizardStepExplanation(viewModel, it, i18n))

                    is AccountSetupWizardStep.NotificationSettingsStep -> add(
                        wizardStepNotification(
                            viewModel,
                            it,
                            i18n
                        )
                    )

                    is AccountSetupWizardStep.ConfirmationStep -> add(
                        wizardStepConfirmation(
                            viewModel,
                            it,
                            i18n
                        )
                    )

                    is AccountSetupWizardStep.PrivacySettingsStep -> add(wizardStepPrivacy(viewModel, it, i18n))

                    is AccountSetupWizardStep.VerificationStep -> add(
                        wizardStepVerification(
                            viewModel,
                            it,
                            i18n
                        )
                    )

                    else -> add(di.get<AdditionalAccountSetupWizardStep>().create(viewModel, it))
                }
            }
        }
    }
    Wizard(steps)
}

private fun wizardStepExplanation(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {

    return WizardStep(id = step.stepId,
        title = { "${i18n.commonWelcome()} ${viewModel.userId.localpart}" },
        content = { Text(i18n.accountSetupWizardExplanationMessage()) },
        additionalButton = {
            Button(modifier = Modifier.buttonPointerModifier(), onClick = { viewModel.closeAccountSetup() }) {
                Text(i18n.commonSkip())
            }
        })
}

private fun wizardStepNotification(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    val notificationSettingsViewModel = viewModel.notificationSettingsViewModel
    return WizardStep(
        id = step.stepId,
        title = { i18n.commonNotifications() },
        content = {
            val enabledOnDevice = notificationSettingsViewModel.enabledForThisDevice.collectAsState().value
            Column {
                Setting(text = i18n.notificationsSettingsEnabledForThisDevice(),
                    value = enabledOnDevice,
                    toggle = { notificationSettingsViewModel.toggleEnabledForThisDevice() })
                MiddleSpacer()
                PlatformNotificationSettings(notificationSettingsViewModel, enabledOnDevice)
                MiddleSpacer()
                PlatformNotificationAccountSettings(notificationSettingsViewModel, enabledOnDevice)
            }
        },
    )
}

private fun wizardStepConfirmation(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    return WizardStep(id = step.stepId, title = { i18n.accountSetupWizardFinishSetupTitle() }, content = {
        Text(i18n.accountSetupWizardFinishSetup())
    }, nextButton = {
        Custom {
            Button(modifier = Modifier.buttonPointerModifier(), onClick = { viewModel.closeAccountSetup() }) {
                Text(i18n.commonConfirm())
            }
        }
    })
}

private fun wizardStepPrivacy(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    val privacySettingsViewModel = viewModel.privacySettingsViewModel
    return WizardStep(id = step.stepId, title = { i18n.privacyTitle() }, content = {
        val di = DI.current
        val publicPresence = privacySettingsViewModel.presenceIsPublic.collectAsState().value
        val publicTyping = privacySettingsViewModel.typingIsPublic.collectAsState().value
        val publicRead = privacySettingsViewModel.readMarkerIsPublic.collectAsState().value
        Column {
            Setting(text = i18n.privacyPresenceIsPublic(),
                explanation = i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName),
                value = publicPresence,
                toggle = { privacySettingsViewModel.togglePresenceIsPublic() })
            Setting(text = i18n.privacyReadMarkerIsPublic(),
                explanation = i18n.privacyReadMarkerIsPublicExplanation(),
                value = publicRead,
                toggle = { privacySettingsViewModel.toggleReadMarkerIsPublic() })
            Setting(text = i18n.privacyTypingIsPublic(),
                explanation = i18n.privacyTypingIsPublicExplanation(),
                value = publicTyping,
                toggle = { privacySettingsViewModel.toggleTypingIsPublic() })
        }
    })
}

private fun wizardStepVerification(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    val verificationViewModel = viewModel.verificationViewModel
    val selfVerificationViewModel = viewModel.selfVerificationViewModel
    val isVerified = viewModel.isVerified
    val selectedMethod = mutableStateOf<SelfVerificationMethodsListEntries?>(null)
    val selectedPassphrase = mutableStateOf<String>("")
    val selectedRecoveryKey = mutableStateOf<String>("")
    val checkedRecoveryResetWarning = mutableStateOf<Boolean>(false)
    val startCrossDevice = mutableStateOf(false)
    return WizardStep(id = step.stepId, title = { i18n.deviceVerification() }, content = {
        Column {
            val isVerified = isVerified.collectAsState().value
            if (isVerified == false) {
                val showHelp = selfVerificationViewModel.showVerificationHelp.collectAsState().value
                val showPassphrase = selfVerificationViewModel.showPassphraseMethod.collectAsState().value != null
                val showKey = selfVerificationViewModel.showRecoveryKeyMethod.collectAsState().value != null
                val showResetRecoveryKeyWarning =
                    selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value

                when {
                    showHelp -> ShowVerificationHelpContent()
                    showPassphrase -> ShowPassphraseMethodContent(
                        selfVerificationViewModel, selectedPassphrase
                    )

                    showKey -> ShowRecoveryKeyMethodContent(
                        selfVerificationViewModel, selectedRecoveryKey
                    )

                    startCrossDevice.value -> {
                        Box { DeviceVerificationStepSwitch(verificationViewModel, true) }
                    }

                    showResetRecoveryKeyWarning -> {
                        ShowResetRecoveryWarningContent(checkedRecoveryResetWarning)
                    }

                    else -> ShowSelfVerificationMethodsContent(
                        selfVerificationViewModel,
                        selectedMethod,
                    )
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
                    Text(i18n.verificationSuccessThisDevice())

                }
            } else {
                LoadingSpinner(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }, nextButton = {
        val isVerified = isVerified.collectAsState().value
        val showHelp = selfVerificationViewModel.showVerificationHelp.collectAsState().value

        if (isVerified == true || (!showHelp && selectedMethod.value is SelfVerificationMethodsListEntries.SelectProceedWithoutVerification)) {
            if (startCrossDevice.value) {
                viewModel.closeCrossDeviceVerification()
            }
            Standard()
        } else {
            Custom {
                val showPassphrase = selfVerificationViewModel.showPassphraseMethod.collectAsState().value != null
                val showKey = selfVerificationViewModel.showRecoveryKeyMethod.collectAsState().value != null
                val showResetRecoveryWarning = selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value
                val enableButton =
                    !startCrossDevice.value
                            && (showHelp || (showPassphrase && selectedPassphrase.value.isNotBlank())
                            || (showKey && selectedRecoveryKey.value.isNotBlank())
                            || (selectedMethod.value is SelfVerificationMethodsListEntries.SelectResetRecoveryKey && !showResetRecoveryWarning)
                            || (showResetRecoveryWarning && checkedRecoveryResetWarning.value
                            || selectedMethod.value is SelfVerificationMethodsListEntries.SelectSelfVerificationMethod))
                Button(modifier = Modifier.buttonPointerModifier(enableButton), enabled = enableButton, onClick = {
                    when {
                        showHelp -> {
                            selfVerificationViewModel.waitForAvailableVerificationMethods()
                        }

                        showPassphrase -> {
                            selfVerificationViewModel.verifyWithPassphrase(selectedPassphrase.value)
                        }

                        showKey -> {
                            selfVerificationViewModel.verifyWithRecoveryKey(selectedRecoveryKey.value)
                        }

                        showResetRecoveryWarning -> {
                            if (checkedRecoveryResetWarning.value) {
                                selfVerificationViewModel.resetRecovery()
                            }
                        }

                        selectedMethod.value is SelfVerificationMethodsListEntries.SelectResetRecoveryKey -> {
                            selfVerificationViewModel.resetRecoveryWarning()
                        }

                        selectedMethod.value is SelfVerificationMethodsListEntries.SelectSelfVerificationMethod -> {
                            val selectedVerificationMethod =
                                (selectedMethod.value as SelfVerificationMethodsListEntries.SelectSelfVerificationMethod).method
                            if (selectedVerificationMethod is SelfVerificationMethod.CrossSignedDeviceVerification) {
                                startCrossDevice.value = true
                            }
                            selfVerificationViewModel.launchVerification((selectedMethod.value as SelfVerificationMethodsListEntries.SelectSelfVerificationMethod).method)
                        }
                    }
                }) {
                    Text(i18n.commonNext())
                }
            }
        }
    }, backButton = {
        val showPassphrase = selfVerificationViewModel.showPassphraseMethod.collectAsState().value != null
        val showKey = selfVerificationViewModel.showRecoveryKeyMethod.collectAsState().value != null
        val showResetRecoveryKeyWarning = selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value
        val showHelp = selfVerificationViewModel.showVerificationHelp.collectAsState().value

        if (showPassphrase || showKey || showResetRecoveryKeyWarning) {
            Custom(button = {
                OutlinedButton(onClick = {
                    selfVerificationViewModel.backToChoose()
                }) {
                    Text(i18n.commonBack())
                }
            })
        } else if (!showHelp) {
            Custom(button = {
                OutlinedButton(onClick = {
                    selfVerificationViewModel.backToHelp()
                }) {
                    Text(i18n.commonBack())
                }
            })
        } else {
            if (startCrossDevice.value) {
                startCrossDevice.value = false
                viewModel.closeCrossDeviceVerification()
            }
            Standard()
        }
    })
}
