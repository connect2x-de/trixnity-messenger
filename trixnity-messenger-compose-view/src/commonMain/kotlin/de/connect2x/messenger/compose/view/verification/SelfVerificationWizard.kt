package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
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
    fun create(selfVerificationViewModel: SelfVerificationViewModel)
}

@Composable
fun SelfVerificationWizard(selfVerificationViewModel: SelfVerificationViewModel) {
    DI.get<SelfVerificationWizardView>().create(selfVerificationViewModel)
}

class SelfVerificationWizardViewImpl : SelfVerificationWizardView {
    @Composable
    override fun create(selfVerificationViewModel: SelfVerificationViewModel) {
        selfVerificationWizard(selfVerificationViewModel)
    }

    @Composable
    private fun selfVerificationWizard(
        selfVerificationViewModel: SelfVerificationViewModel,
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
                        showHelp -> ShowVerificationHelpContent()
                        showPassphrase -> ShowPassphraseMethodContent(
                            selfVerificationViewModel, selectedPassphrase
                        )

                        showKey -> ShowRecoveryKeyMethodContent(
                            selfVerificationViewModel, selectedRecoveryKey
                        )

                        /* startCrossDevice.value -> {
                             Box { DeviceVerificationStepSwitch(verificationViewModel, true) }
                         }*/

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
                            selfVerificationViewModel.backToHelp()
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
