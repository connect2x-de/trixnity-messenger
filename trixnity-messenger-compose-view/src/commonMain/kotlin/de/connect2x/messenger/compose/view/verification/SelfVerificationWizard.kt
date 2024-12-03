package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MoreInfo
import de.connect2x.messenger.compose.view.common.PasswordField
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.messenger.compose.view.verification.SelfVerificationMethodsListEntries.SelectSelfVerificationMethod
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

open class SelfVerificationMethodsListEntries {
    data class SelectSelfVerificationMethod(val method: SelfVerificationMethod) : SelfVerificationMethodsListEntries()
    data object SelectResetRecoveryKey : SelfVerificationMethodsListEntries()
    data object SelectProceedWithoutVerification : SelfVerificationMethodsListEntries()
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

        data object SelfVerificationWizardVerificationConfirmation :
            SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_VERIFICATION_CONFIRMATION")
    }

    @Composable
    private fun selfVerificationWizard(selfVerificationViewModel: SelfVerificationViewModel, showHelpScreen: Boolean) {
        val stepList = listOf<SelfVerificationWizardStep>(
            SelfVerificationWizardStep.SelfVerificationWizardHelp,
            SelfVerificationWizardStep.SelfVerificationWizardMethods,
            SelfVerificationWizardStep.SelfVerificationWizardRecoveryKey,
            SelfVerificationWizardStep.SelfVerificationWizardPassphrase,
            SelfVerificationWizardStep.SelfVerificationWizardResetRecoveryKeyConfirmation,
            SelfVerificationWizardStep.SelfVerificationWizardVerificationConfirmation
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

                        is SelfVerificationWizardStep.SelfVerificationWizardVerificationConfirmation -> this.add(
                            selfVerificationWizardVerificationConfirmationStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.SelfVerificationWizardVerificationConfirmation,
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
        val isVerified = selfVerificationViewModel.isVerified
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                val isVerified = isVerified.collectAsState().value
                if (isVerified == false) {
                    Column {
                        Text(text = i18n.selfVerificationHelpOtherDevice())
                        Text(text = i18n.selfVerificationHelpVerifyThis())
                        Spacer(Modifier.size(20.dp))
                        MoreInfo(i18n.selfVerificationHelpReasonTitle()) {
                            RunningText(text = i18n.selfVerificationHelpReason1())
                            RunningText(text = i18n.selfVerificationHelpReason2())
                            RunningText(text = i18n.selfVerificationHelpReason3())
                        }
                    }
                }
            },
            nextButton = {
                Custom {
                    val isVerified = isVerified.collectAsState().value
                    if (isVerified == true) {
                        currentStepId.value =
                            SelfVerificationWizardStep.SelfVerificationWizardVerificationConfirmation.stepId
                    }
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
                    val selfVerificationMethods = selfVerificationViewModel.selfVerificationMethods.collectAsState()
                    val methodsLoaded = selfVerificationViewModel.verificationMethodsLoaded.collectAsState().value
                    if (methodsLoaded) {
                        Text(i18n.selfVerificationMethodsTitle())
                    } else {
                        Text(i18n.selfVerificationWaitingForMethods())
                        LoadingSpinner()
                    }
                    Spacer(Modifier.size(10.dp))

                    selfVerificationMethods.value.forEachIndexed { _, method ->
                        when (method) {
                            is SelfVerificationMethod.CrossSignedDeviceVerification -> {
                                EntryContainer(
                                    header = {
                                        Text(
                                            text = i18n.selfVerificationMethodsOtherDevice(),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    },
                                    description = { Text(i18n.selfVerificationMethodsOtherDeviceInfo()) },
                                    onClick = { selectedMethod.value = SelectSelfVerificationMethod(method) },
                                    selected = selectedMethod.value == SelectSelfVerificationMethod(method)
                                )
                            }

                            is SelfVerificationMethod.AesHmacSha2RecoveryKey -> {
                                EntryContainer(
                                    header = {
                                        Text(
                                            i18n.selfVerificationMethodsRecoveryKey(),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    },
                                    description = { Text(i18n.selfVerificationMethodsRecoveryKeyInfo()) },
                                    onClick = { selectedMethod.value = SelectSelfVerificationMethod(method) },
                                    selected = selectedMethod.value == SelectSelfVerificationMethod(method)
                                )
                            }

                            is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> {
                                EntryContainer(
                                    header = {
                                        Text(
                                            i18n.selfVerificationMethodsRecoveryPassphrase(),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    },
                                    description = { Text(i18n.selfVerificationMethodsRecoveryPassphraseInfo()) },
                                    onClick = { selectedMethod.value = SelectSelfVerificationMethod(method) },
                                    selected = selectedMethod.value == SelectSelfVerificationMethod(method)
                                )
                            }

                            else -> Box {}
                        }
                    }
                    if (methodsLoaded) {
                        EntryContainer(
                            header = {
                                Icon(
                                    Icons.Default.Warning,
                                    i18n.commonWarning(),
                                    Modifier.size(16.dp),
                                    MaterialTheme.colorScheme.error
                                )
                                SmallSpacer()
                                Text(
                                    i18n.selfVerificationResetRecoveryWarningTitle(selfVerificationViewModel.userId),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            description = {
                                Text(buildAnnotatedString {
                                    append("${i18n.selfVerificationResetRecoveryKey()}. ")
                                    pushStyle(SpanStyle(fontWeight = Bold))
                                    append("${i18n.commonWarning().capitalize(Locale.current)}: ")
                                    append(i18n.selfVerificationResetRecoveryKeyDescription())
                                    pop()
                                })
                            },
                            onClick = {
                                selectedMethod.value = SelfVerificationMethodsListEntries.SelectResetRecoveryKey
                            },
                            selected = selectedMethod.value == SelfVerificationMethodsListEntries.SelectResetRecoveryKey
                        )
                    }
                    EntryContainer(
                        header = {
                            Icon(
                                Icons.Default.Warning,
                                i18n.commonWarning(),
                                Modifier.size(16.dp),
                                MaterialTheme.colorScheme.error
                            )
                            SmallSpacer()
                            Text(
                                i18n.redoSelfVerificationContinueWithoutVerification(),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        description = {
                            Text(
                                text = i18n.redoSelfVerificationDoItLater(),
                            )
                        },
                        onClick = {
                            selectedMethod.value = SelfVerificationMethodsListEntries.SelectProceedWithoutVerification
                        },
                        selected = selectedMethod.value == SelfVerificationMethodsListEntries.SelectProceedWithoutVerification
                    )
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

                                is SelectSelfVerificationMethod -> {
                                    selfVerificationViewModel.launchVerification((selectedMethod.value as SelectSelfVerificationMethod).method)
                                    when ((selectedMethod.value as SelectSelfVerificationMethod).method) {
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
        val recoveryKey = mutableStateOf("")
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                Column {
                    val recoveryKeyWrong = selfVerificationViewModel.recoveryKeyWrong.collectAsState()
                    val error = selfVerificationViewModel.error.collectAsState()
                    Text(i18n.selfVerificationMethodsRecoveryKeyTitle())
                    Spacer(Modifier.size(10.dp))

                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = recoveryKey.value,
                            onValueChange = { recoveryKey.value = it },
                            modifier = Modifier.weight(1.0f, fill = true),
                            isError = recoveryKeyWrong.value,
                            placeholder = { Text("#### ".repeat(11) + "####", color = Color.LightGray) },
                            label = { Text(i18n.commonRecoveryKey()) })
                    }
                    if (recoveryKeyWrong.value) {
                        Box(Modifier.fillMaxWidth()) {
                            Text(
                                text = i18n.selfVerificationMethodsRecoveryKeyWrong(),
                                modifier = Modifier.align(Alignment.CenterEnd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    error.value?.let { Spacer(Modifier.size(20.dp)); ErrorView(it) }
                }
            },
            nextButton = {
                Custom {
                    Button(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.verifyWithRecoveryKey(recoveryKey.value)
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
        val passphrase = mutableStateOf("")
        return WizardStep(
            id = step.stepId,
            title = { i18n.deviceVerification() },
            content = {
                Column {
                    val passphraseWrong = selfVerificationViewModel.passphraseWrong.collectAsState()
                    val error = selfVerificationViewModel.error.collectAsState()
                    Text(i18n.selfVerificationMethodsRecoveryPassphraseTitle())
                    Text(buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = Bold))
                        append("${i18n.bootstrapRecoveryKeyAttention()}:")
                        pop()
                        append(i18n.selfVerificationMethodsRecoveryPassphraseWarning())
                    })
                    Spacer(Modifier.size(10.dp))
                    PasswordField(
                        password = passphrase,
                        onPasswordChange = { passphrase.value = it },
                        label = { Text(i18n.commonRecoveryPassphrase()) },
                    )
                    if (passphraseWrong.value) {
                        Box(Modifier.fillMaxWidth()) {
                            Text(
                                text = i18n.selfVerificationMethodsRecoveryPassphraseWrong(),
                                modifier = Modifier.align(Alignment.CenterEnd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    error.value?.let { Spacer(Modifier.size(20.dp)); ErrorView(it) }
                }
            },
            nextButton = {
                Custom {
                    Button(
                        modifier = Modifier.buttonPointerModifier(),
                        onClick = {
                            selfVerificationViewModel.verifyWithPassphrase(passphrase.value)
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
                    Text(text = "${i18n.resetWarningIsPermanent()} ${i18n.resetWarningLostAccessAndReVerify()} ${i18n.resetWarningLastResort()}")

                    Spacer(Modifier.size(10.dp))

                    Row(
                        Modifier.fillMaxWidth().clickable { checked.value = !checked.value },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked.value,
                            onCheckedChange = { checked.value = it })
                        Spacer(Modifier.size(10.dp))
                        Text(i18n.resetWarningAcknowledge())
                    }
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

    private fun selfVerificationWizardVerificationConfirmationStep(
        selfVerificationViewModel: SelfVerificationViewModel,
        step: SelfVerificationWizardStep,
        i18n: I18nView
    ): WizardStep {
        return WizardStep(
            id = step.stepId, title = { i18n.deviceVerification() },
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(50.dp),
                        contentDescription = i18n.commonSuccess(),
                        tint = MaterialTheme.messengerColors.success
                    )
                    SmallSpacer()
                    Text(i18n.verificationSuccessThisDevice(), modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            },
            nextButton = {
                Custom {
                    Button(
                        onClick = {
                            selfVerificationViewModel.backToChoose()
                            selfVerificationViewModel.close()
                        },
                        modifier = Modifier.buttonPointerModifier()
                    ) {
                        Text(i18n.commonNext())
                    }

                }
            },
            backButton = {
                Custom {
                    OutlinedButton(
                        onClick = { selfVerificationViewModel.close() },
                        modifier = Modifier.buttonPointerModifier()
                    ) {
                        Text(i18n.commonBack())
                    }
                }
            }
        )
    }

    @Composable
    private fun EntryContainer(
        header: @Composable RowScope.() -> Unit,
        description: @Composable () -> Unit,
        onClick: () -> Unit,
        selected: Boolean,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            onClick = onClick,
            modifier = Modifier.padding(vertical = MaterialTheme.messengerDpConstants.small).buttonPointerModifier()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .padding(MaterialTheme.messengerDpConstants.small)
            ) {
                RadioButton(onClick = onClick, selected = selected)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) {
                        ProvideTextStyle(
                            MaterialTheme.typography.titleSmall
                        ) {
                            header()
                        }
                    }
                    SmallSpacer()
                    ProvideTextStyle(
                        MaterialTheme.typography.bodySmall
                    ) {
                        description()
                    }
                }
            }
        }
    }


}
