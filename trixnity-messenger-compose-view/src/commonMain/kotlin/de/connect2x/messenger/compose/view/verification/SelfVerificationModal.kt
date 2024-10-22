package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.BackButton
import de.connect2x.messenger.compose.view.common.CloseModalButton
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.common.MessengerModalThreeButtonRow
import de.connect2x.messenger.compose.view.common.MoreInfo
import de.connect2x.messenger.compose.view.common.NextButton
import de.connect2x.messenger.compose.view.common.PasswordField
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.verification.SelfVerificationMethodsListEntries.SelectSelfVerificationMethod
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import net.folivo.trixnity.client.verification.SelfVerificationMethod

interface SelfVerificationModalView {
    @Composable
    fun create(selfVerificationViewModel: SelfVerificationViewModel)
}

@Composable
fun SelfVerificationModal(selfVerificationViewModel: SelfVerificationViewModel) {
    DI.get<SelfVerificationModalView>().create(selfVerificationViewModel)
}

class SelfVerificationModalViewImpl : SelfVerificationModalView {
    @Composable
    override fun create(selfVerificationViewModel: SelfVerificationViewModel) {
        val i18n = DI.get<I18nView>()
        val showVerificationHelp = selfVerificationViewModel.showVerificationHelp.collectAsState().value
        val showResetRecoveryWarning = selfVerificationViewModel.showResetRecoveryWarning.collectAsState().value
        val showPassphraseMethod = selfVerificationViewModel.showPassphraseMethod.collectAsState().value != null
        val showRecoveryKeyMethod = selfVerificationViewModel.showRecoveryKeyMethod.collectAsState().value != null

        MessengerModal(
            title = if (showResetRecoveryWarning) i18n.selfVerificationResetRecoveryWarningTitle(selfVerificationViewModel.userId)
            else i18n.selfVerificationTitle(selfVerificationViewModel.userId),
        ) {
            when {
                showVerificationHelp -> ShowVerificationHelp(selfVerificationViewModel)
                showResetRecoveryWarning -> ShowResetRecoveryWarning(selfVerificationViewModel)
                showPassphraseMethod -> ShowPassphraseMethod(selfVerificationViewModel)
                showRecoveryKeyMethod -> ShowRecoveryKeyMethod(selfVerificationViewModel)
                else -> ShowSelfVerificationMethods(selfVerificationViewModel)
            }
        }
    }
}

@Composable
fun ColumnScope.ShowVerificationHelp(selfVerificationViewModel: SelfVerificationViewModel) {
    MessengerModalContent {
        ShowVerificationHelpContent()
    }

    MessengerModalThreeButtonRow(
        next = { NextButton { selfVerificationViewModel.waitForAvailableVerificationMethods() } },
    )

}

@Composable
fun ColumnScope.ShowVerificationHelpContent() {
    val i18n = DI.get<I18nView>()
    Text(text = i18n.selfVerificationHelpOtherDevice())
    Text(text = i18n.selfVerificationHelpVerifyThis())
    Spacer(Modifier.size(20.dp))

    MoreInfo(i18n.selfVerificationHelpReasonTitle()) {
        RunningText(text = i18n.selfVerificationHelpReason1())
        RunningText(text = i18n.selfVerificationHelpReason2())
        RunningText(text = i18n.selfVerificationHelpReason3())
    }
}

@Composable
fun ColumnScope.ShowResetRecoveryWarning(selfVerificationViewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()

    val checked = remember { mutableStateOf(false) }

    MessengerModalContent {
        ShowResetRecoveryWarningContent(checked)
    }

    MessengerModalThreeButtonRow(
        next = {
            NextButton(
                text = i18n.resetProceed(),
                enabled = checked.value,
                nextAction = selfVerificationViewModel::resetRecovery,
            )
        },
        back = {
            BackButton { selfVerificationViewModel.backToHelp() }
        },
    )

}

@Composable
fun ColumnScope.ShowResetRecoveryWarningContent(checked: MutableState<Boolean>) {
    val i18n = DI.get<I18nView>()
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

open class SelfVerificationMethodsListEntries {
    data class SelectSelfVerificationMethod(val method: SelfVerificationMethod) : SelfVerificationMethodsListEntries()
    data object SelectResetRecoveryKey : SelfVerificationMethodsListEntries()
    data object SelectProceedWithoutVerification : SelfVerificationMethodsListEntries()
}

@Composable
fun ColumnScope.ShowSelfVerificationMethods(selfVerificationViewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    val selectedMethod = remember { mutableStateOf<SelfVerificationMethodsListEntries?>(null) }

    MessengerModalContent {
        ShowSelfVerificationMethodsContent(
            selfVerificationViewModel,
            selectedMethod
        )
    }

    MessengerModalThreeButtonRow(
        next = {
            NextButton(enabled = selectedMethod.value != null) {
                when (val selected = selectedMethod.value) {
                    is SelectSelfVerificationMethod -> selfVerificationViewModel.launchVerification(selected.method)
                    is SelfVerificationMethodsListEntries.SelectResetRecoveryKey -> selfVerificationViewModel.resetRecoveryWarning()
                    is SelfVerificationMethodsListEntries.SelectProceedWithoutVerification -> selfVerificationViewModel.close()
                }
            }
        },
        back = {
            BackButton(
                selfVerificationViewModel::backToHelp
            )
        },
    )
}

@Composable
fun ColumnScope.ShowSelfVerificationMethodsContent(
    selfVerificationViewModel: SelfVerificationViewModel,
    selectedVerificationMethod: MutableState<SelfVerificationMethodsListEntries?>,
) {
    val i18n = DI.get<I18nView>()
    val selfVerificationMethods = selfVerificationViewModel.selfVerificationMethods.collectAsState()
    val hasRecoveryResetOption = selfVerificationViewModel.hasResetRecoveryOption.collectAsState().value
    Text(i18n.selfVerificationMethodsTitle())
    Spacer(Modifier.size(10.dp))

    selfVerificationMethods.value.forEachIndexed { index, method ->
        when (method) {
            is SelfVerificationMethod.CrossSignedDeviceVerification -> {
                Column(Modifier.padding(top = if (index == 0) 0.dp else 20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedVerificationMethod.value = SelectSelfVerificationMethod(method)
                        }
                    ) {
                        RadioButton(
                            selected = selectedVerificationMethod.value is SelectSelfVerificationMethod
                                    && (selectedVerificationMethod.value as SelectSelfVerificationMethod).method == method,
                            onClick = {
                                selectedVerificationMethod.value = SelectSelfVerificationMethod(method)
                            },
                        )
                        Text(
                            text = i18n.selfVerificationMethodsOtherDevice(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Column(Modifier.padding(start = 48.dp)) {
                        Text(
                            text = i18n.selfVerificationMethodsOtherDeviceInfo(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            is SelfVerificationMethod.AesHmacSha2RecoveryKey -> {
                Column(Modifier.padding(top = if (index == 0) 0.dp else 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedVerificationMethod.value = SelectSelfVerificationMethod(method)
                        }
                    ) {
                        RadioButton(
                            selected = selectedVerificationMethod.value is SelectSelfVerificationMethod
                                    && (selectedVerificationMethod.value as SelectSelfVerificationMethod).method == method,
                            onClick = {
                                selectedVerificationMethod.value = SelectSelfVerificationMethod(method)
                            }
                        )
                        Text(
                            i18n.selfVerificationMethodsRecoveryKey(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Column(Modifier.padding(start = 48.dp)) {
                        Text(
                            text = i18n.selfVerificationMethodsRecoveryKeyInfo(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                }
            }

            is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> {
                Column(Modifier.padding(top = if (index == 0) 0.dp else 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedVerificationMethod.value = SelectSelfVerificationMethod(method) }
                    ) {
                        RadioButton(
                            selected = selectedVerificationMethod.value is SelectSelfVerificationMethod
                                    && (selectedVerificationMethod.value as SelectSelfVerificationMethod).method == method,
                            onClick = {
                                selectedVerificationMethod.value = SelectSelfVerificationMethod(method)
                            }
                        )
                        Text(
                            i18n.selfVerificationMethodsRecoveryPassphrase(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Column(Modifier.padding(start = 48.dp)) {
                        Text(
                            text = i18n.selfVerificationMethodsRecoveryPassphraseInfo(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            else -> Box {}
        }
    }
    if (hasRecoveryResetOption) {
        Column(Modifier.padding(top = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    selectedVerificationMethod.value = SelfVerificationMethodsListEntries.SelectResetRecoveryKey
                }
            ) {
                RadioButton(
                    selected = selectedVerificationMethod.value is SelfVerificationMethodsListEntries.SelectResetRecoveryKey,
                    onClick = {
                        selectedVerificationMethod.value = SelfVerificationMethodsListEntries.SelectResetRecoveryKey
                    }
                )
                Icon(Icons.Default.Warning, i18n.commonWarning())
                SmallSpacer()
                Text(
                    i18n.selfVerificationResetRecoveryWarningTitle(selfVerificationViewModel.userId),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Column(Modifier.padding(start = 48.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("${i18n.selfVerificationResetRecoveryKey()}. ")
                        pushStyle(SpanStyle(fontWeight = Bold))
                        append("${i18n.commonWarning().capitalize(Locale.current)}: ")
                        append(i18n.selfVerificationResetRecoveryKeyDescription())
                        pop()
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

        }
    }
    Column(Modifier.padding(top = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                selectedVerificationMethod.value = SelfVerificationMethodsListEntries.SelectProceedWithoutVerification
            }
        ) {
            RadioButton(
                selected = selectedVerificationMethod.value is SelfVerificationMethodsListEntries.SelectProceedWithoutVerification,
                onClick = {
                    selectedVerificationMethod.value = SelfVerificationMethodsListEntries.SelectProceedWithoutVerification
                }
            )
            Icon(Icons.Default.Warning, i18n.commonWarning())
            SmallSpacer()
            Text(
                i18n.redoSelfVerificationContinueWithoutVerification(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Column(Modifier.padding(start = 48.dp)) {
            Text(
                text = i18n.redoSelfVerificationDoItLater(),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ColumnScope.ShowPassphraseMethod(
    selfVerificationViewModel: SelfVerificationViewModel,
) {
    val i18n = DI.get<I18nView>()
    val passphrase = remember { mutableStateOf("") }

    MessengerModalContent {
        ShowPassphraseMethodContent(selfVerificationViewModel, passphrase)
    }

    MessengerModalButtonRow(
        { BackButton { selfVerificationViewModel.backToChoose() } },
        {
            NextButton(enabled = passphrase.value.isNotBlank(), text = i18n.commonVerify()) {
                selfVerificationViewModel.verifyWithPassphrase(passphrase.value)
            }
        }
    )
}

@Composable
fun ColumnScope.ShowPassphraseMethodContent(
    selfVerificationViewModel: SelfVerificationViewModel,
    passphrase: MutableState<String>
) {
    val passphraseWrong = selfVerificationViewModel.passphraseWrong.collectAsState()
    val error = selfVerificationViewModel.error.collectAsState()
    val i18n = DI.get<I18nView>()
    Text(i18n.selfVerificationMethodsRecoveryPassphraseTitle())
    Text(buildAnnotatedString {
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
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

@Composable
fun ColumnScope.ShowRecoveryKeyMethod(
    selfVerificationViewModel: SelfVerificationViewModel
) {
    val i18n = DI.get<I18nView>()
    val recoveryKey = remember { mutableStateOf("") }

    MessengerModalContent {
        ShowRecoveryKeyMethodContent(selfVerificationViewModel, recoveryKey)
    }

    MessengerModalButtonRow(
        { BackButton { selfVerificationViewModel.backToChoose() } },
        {
            NextButton(enabled = recoveryKey.value.isNotBlank(), text = i18n.commonVerify()) {
                selfVerificationViewModel.verifyWithRecoveryKey(recoveryKey.value)
            }
        }
    )
}

@Composable
fun ColumnScope.ShowRecoveryKeyMethodContent(
    selfVerificationViewModel: SelfVerificationViewModel,
    recoveryKey: MutableState<String>
) {
    val i18n = DI.get<I18nView>()
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
