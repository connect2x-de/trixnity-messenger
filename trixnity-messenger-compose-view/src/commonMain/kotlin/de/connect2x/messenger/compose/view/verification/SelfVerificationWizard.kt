package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import net.folivo.trixnity.client.verification.SelfVerificationMethod

interface SelfVerificationWizardView {
    @Composable
    fun create(selfVerificationViewModel: SelfVerificationViewModel, showHelpScreen: Boolean)
}

@Composable
fun SelfVerificationWizard(selfVerificationViewModel: SelfVerificationViewModel, showHelpScreen: Boolean = true) {
    DI.get<SelfVerificationWizardView>().create(selfVerificationViewModel, showHelpScreen)
}

class SelfVerificationWizardViewImpl : SelfVerificationWizardView {
    @Composable
    override fun create(selfVerificationViewModel: SelfVerificationViewModel, showHelpScreen: Boolean) {
        selfVerificationWizard(selfVerificationViewModel, showHelpScreen)
    }

    open class SelfVerificationWizardStep(val stepId: String) {
        data object SelfVerificationWizardHelp : SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_HELP")
        data object SelfVerificationWizardMethods : SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_METHODS")
        data object SelfVerificationWizardRecoveryKey :
            SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_RECOVERY_KEY")

        data object SelfVerificationWizardPassphrase :
            SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_PASSPHRASE")

        data object SelfVerificationWizardResetRecoveryKeyConfirmation :
            SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_RESET_RECOVERY_KEY_CONFIRM")
    }

    @Composable
    private fun selfVerificationWizard(selfVerificationViewModel: SelfVerificationViewModel, showHelpScreen: Boolean) {
        val stepList = listOf<SelfVerificationWizardStep>(
            SelfVerificationWizardStep.SelfVerificationWizardHelp,
            SelfVerificationWizardStep.SelfVerificationWizardMethods,
            SelfVerificationWizardStep.SelfVerificationWizardRecoveryKey,
            SelfVerificationWizardStep.SelfVerificationWizardPassphrase,
            SelfVerificationWizardStep.SelfVerificationWizardResetRecoveryKeyConfirmation
        )
        val i18n = DI.get<I18nView>()

        val steps = remember {
            mutableListOf<WizardStep>().apply {
                stepList.forEach {
                    when (it) {
                        is SelfVerificationWizardStep.SelfVerificationWizardHelp -> this.add(
                            selfVerificationWizardHelpStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.SelfVerificationWizardHelp,
                                i18n
                            )
                        )

                        is SelfVerificationWizardStep.SelfVerificationWizardMethods -> this.add(
                            selfVerificationWizardMethodStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.SelfVerificationWizardMethods, i18n
                            )
                        )

                        is SelfVerificationWizardStep.SelfVerificationWizardRecoveryKey -> this.add(
                            selfVerificationWizardRecoveryKeyStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.SelfVerificationWizardRecoveryKey,
                                i18n
                            )
                        )

                        is SelfVerificationWizardStep.SelfVerificationWizardPassphrase -> this.add(
                            selfVerificationWizardPassphraseStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.SelfVerificationWizardPassphrase,
                                i18n
                            )
                        )

                        is SelfVerificationWizardStep.SelfVerificationWizardResetRecoveryKeyConfirmation -> this.add(
                            selfVerificationWizardResetRecoveryKeyConfirmationStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.SelfVerificationWizardResetRecoveryKeyConfirmation,
                                i18n
                            )
                        )
                    }
                }
            }
        }
        Wizard(steps)
    }

    private fun selfVerificationWizardHelpStep(
        selfVerificationViewModel: SelfVerificationViewModel,
        step: SelfVerificationWizardStep,
        i18n: I18nView
    ): WizardStep {
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                Column {
                    ShowVerificationHelpContent()
                }
            },
            nextButton = {
                Custom {
                    Button(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.waitForAvailableVerificationMethods()
                            nextStep?.let { currentStepId.value = it }
                        }) {
                        Text(i18n.commonNext())
                    }
                }
            },
            backButton = {
                Custom {
                    OutlinedButton(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.close()
                        }) {
                        Text(i18n.commonBack())
                    }
                }
            }
        )
    }

    private fun selfVerificationWizardMethodStep(
        selfVerificationViewModel: SelfVerificationViewModel,
        step: SelfVerificationWizardStep,
        i18n: I18nView
    ): WizardStep {
        val selectedMethod = mutableStateOf<SelfVerificationMethodsListEntries?>(null)
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                Column {
                    ShowSelfVerificationMethodsContent(selfVerificationViewModel, selectedMethod)
                }
            },
            backButton = {
                Custom {
                    OutlinedButton(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.backToHelp()
                            previousStep?.let { currentStepId.value = it }

                        }) {
                        Text(i18n.commonBack())
                    }
                }
            },
            nextButton = {
                Custom {
                    Button(
                        modifier = Modifier.buttonPointerModifier(),
                        enabled = selectedMethod.value != null,
                        onClick = {
                            when (selectedMethod.value) {
                                is SelfVerificationMethodsListEntries.SelectProceedWithoutVerification -> {
                                    selfVerificationViewModel.close()
                                }

                                is SelfVerificationMethodsListEntries.SelectSelfVerificationMethod -> {
                                    selfVerificationViewModel.launchVerification((selectedMethod.value as SelfVerificationMethodsListEntries.SelectSelfVerificationMethod).method)
                                    when ((selectedMethod.value as SelfVerificationMethodsListEntries.SelectSelfVerificationMethod).method) {
                                        is SelfVerificationMethod.AesHmacSha2RecoveryKey -> {
                                            currentStepId.value =
                                                SelfVerificationWizardStep.SelfVerificationWizardRecoveryKey.stepId
                                        }

                                        is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> {
                                            currentStepId.value =
                                                SelfVerificationWizardStep.SelfVerificationWizardPassphrase.stepId
                                        }

                                        is SelfVerificationMethod.CrossSignedDeviceVerification -> {

                                        }
                                    }
                                }

                                is SelfVerificationMethodsListEntries.SelectResetRecoveryKey -> {
                                    selfVerificationViewModel.resetRecoveryWarning()
                                    currentStepId.value =
                                        SelfVerificationWizardStep.SelfVerificationWizardResetRecoveryKeyConfirmation.stepId
                                }
                            }

                        }) {
                        Text(i18n.commonNext())
                    }
                }
            },
        )
    }

    private fun selfVerificationWizardRecoveryKeyStep(
        selfVerificationViewModel: SelfVerificationViewModel,
        step: SelfVerificationWizardStep,
        i18n: I18nView
    ): WizardStep {
        val selectedKey = mutableStateOf("")
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                Column {
                    ShowRecoveryKeyMethodContent(selfVerificationViewModel, selectedKey)
                }
            },
            nextButton = {
                Custom {
                    Button(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.verifyWithRecoveryKey(selectedKey.value)
                        }) {
                        Text(i18n.commonNext())
                    }
                }
            },
            backButton = {
                Custom {
                    OutlinedButton(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.backToChoose()
                            currentStepId.value = SelfVerificationWizardStep.SelfVerificationWizardMethods.stepId
                        }) {
                        Text(i18n.commonBack())
                    }
                }
            }
        )
    }

    private fun selfVerificationWizardPassphraseStep(
        selfVerificationViewModel: SelfVerificationViewModel,
        step: SelfVerificationWizardStep,
        i18n: I18nView
    ): WizardStep {
        val selectedPassphrase = mutableStateOf("")
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                Column {
                    ShowPassphraseMethodContent(selfVerificationViewModel, selectedPassphrase)
                }
            },
            nextButton = {
                Custom {
                    Button(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.verifyWithPassphrase(selectedPassphrase.value)
                        }) {
                        Text(i18n.commonNext())
                    }
                }
            },
            backButton = {
                Custom {
                    OutlinedButton(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.backToChoose()
                            currentStepId.value = SelfVerificationWizardStep.SelfVerificationWizardMethods.stepId
                        }) {
                        Text(i18n.commonBack())
                    }
                }
            }
        )
    }

    private fun selfVerificationWizardResetRecoveryKeyConfirmationStep(
        selfVerificationViewModel: SelfVerificationViewModel,
        step: SelfVerificationWizardStep,
        i18n: I18nView
    ): WizardStep {
        val checked = mutableStateOf<Boolean>(false)
        return WizardStep(
            id = step.stepId, title = { i18n.deviceVerification() },
            content = {
                Column {
                    ShowResetRecoveryWarningContent(checked)
                }
            },
            backButton = {
                Custom {
                    OutlinedButton(onClick = {
                        currentStepId.value = SelfVerificationWizardStep.SelfVerificationWizardMethods.stepId
                    }, modifier = Modifier.buttonPointerModifier()) {
                        Text(i18n.commonBack())
                    }
                }
            },
            nextButton = {
                Custom {
                    Button(
                        onClick = {
                            selfVerificationViewModel.resetRecovery()
                        },
                        modifier = Modifier.buttonPointerModifier(),
                        enabled = checked.value
                    ) {
                        Text(i18n.commonNext())
                    }
                }
            }
        )
    }

    @Composable
    private fun selfVerificationWizar(
        selfVerificationViewModel: SelfVerificationViewModel,
        showHelpScreen: Boolean
    ) {
        val i18n = DI.get<I18nView>()
        val selfVerificationViewModel = selfVerificationViewModel
        val selectedMethod = mutableStateOf<SelfVerificationMethodsListEntries?>(null)
        val selectedPassphrase = mutableStateOf<String>("")
        val selectedRecoveryKey = mutableStateOf<String>("")
        val checkedRecoveryResetWarning = mutableStateOf<Boolean>(false)
        val startCrossDevice = mutableStateOf(false)
        val step =
            WizardStep(id = "SELF-VERIFICATION-WIZARD-VERIFICATION", title = { i18n.deviceVerification() }, content = {
                Column {
                    val showHelp = selfVerificationViewModel.showVerificationHelp.collectAsState().value
                    val showPassphrase = selfVerificationViewModel.showPassphraseMethod.collectAsState().value != null
                    val showKey = selfVerificationViewModel.showRecoveryKeyMethod.collectAsState().value != null
                    val showResetRecoveryKeyWarning =
                        selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value

                    when {
                        showHelp -> {
                            if (!showHelpScreen) selfVerificationViewModel.waitForAvailableVerificationMethods()
                            ShowVerificationHelpContent()
                        }

                        showPassphrase -> ShowPassphraseMethodContent(
                            selfVerificationViewModel, selectedPassphrase
                        )

                        showKey -> ShowRecoveryKeyMethodContent(
                            selfVerificationViewModel, selectedRecoveryKey
                        )

                        showResetRecoveryKeyWarning -> {
                            ShowResetRecoveryWarningContent(checkedRecoveryResetWarning)
                        }

                        else -> ShowSelfVerificationMethodsContent(
                            selfVerificationViewModel,
                            selectedMethod,
                        )
                    }
                }
            }, nextButton = {
                val showHelp = selfVerificationViewModel.showVerificationHelp.collectAsState().value

                if (!showHelp && selectedMethod.value is SelfVerificationMethodsListEntries.SelectProceedWithoutVerification) {
                    if (startCrossDevice.value) {
                        startCrossDevice.value = false
                    }
                    Custom { Button(onClick = { selfVerificationViewModel.close() }) { Text(i18n.commonNext()) } }
                } else {
                    Custom {
                        val showPassphrase =
                            selfVerificationViewModel.showPassphraseMethod.collectAsState().value != null
                        val showKey = selfVerificationViewModel.showRecoveryKeyMethod.collectAsState().value != null
                        val showResetRecoveryWarning =
                            selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value
                        val enableButton =
                            !startCrossDevice.value
                                    && (showHelp || (showPassphrase && selectedPassphrase.value.isNotBlank())
                                    || (showKey && selectedRecoveryKey.value.isNotBlank())
                                    || (selectedMethod.value is SelfVerificationMethodsListEntries.SelectResetRecoveryKey && !showResetRecoveryWarning)
                                    || (showResetRecoveryWarning && checkedRecoveryResetWarning.value
                                    || selectedMethod.value is SelfVerificationMethodsListEntries.SelectSelfVerificationMethod))
                        Button(
                            modifier = Modifier.buttonPointerModifier(enableButton),
                            enabled = enableButton,
                            onClick = {
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
                val showResetRecoveryKeyWarning =
                    selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value
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
                            if (showHelpScreen) selfVerificationViewModel.backToHelp()
                            else selfVerificationViewModel.close()
                        }) {
                            Text(i18n.commonBack())
                        }
                    })
                } else {
                    if (startCrossDevice.value) {
                        startCrossDevice.value = false
                    }
                    Custom {
                        OutlinedButton(onClick = { selfVerificationViewModel.close() }) {
                            Text(i18n.commonBack())
                        }
                    }
                }
            })
        Wizard(listOf(step))
    }
}
