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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.BackButton
import de.connect2x.messenger.compose.view.common.CloseModalButton
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.common.MoreInfo
import de.connect2x.messenger.compose.view.common.NextButton
import de.connect2x.messenger.compose.view.common.PasswordField
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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
            selfVerificationViewModel::close,
            if (showResetRecoveryWarning) i18n.selfVerificationResetRecoveryWarningTitle(selfVerificationViewModel.userId)
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
    val i18n = DI.get<I18nView>()
    MessengerModalContent {
        Text(text = i18n.selfVerificationHelpOtherDevice())
        Text(text = i18n.selfVerificationHelpVerifyThis())
        Spacer(Modifier.size(20.dp))

        MoreInfo(i18n.selfVerificationHelpReasonTitle()) {
            RunningText(text = i18n.selfVerificationHelpReason1())
            RunningText(text = i18n.selfVerificationHelpReason2())
            RunningText(text = i18n.selfVerificationHelpReason3())
        }
    }

    MessengerModalButtonRow(
        {
            OutlinedButton(
                onClick = selfVerificationViewModel::resetRecoveryWarning,
                modifier = Modifier.buttonPointerModifier(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(i18n.selfVerificationResetRecoveryKey().capitalize(Locale.current))
            }
        },
        {
            CloseModalButton(
                selfVerificationViewModel::close,
                i18n.redoSelfVerificationContinueWithoutVerification(),
            )
        },
        { NextButton { selfVerificationViewModel.waitForAvailableVerificationMethods() } })
}

@Composable
fun ColumnScope.ShowResetRecoveryWarning(selfVerificationViewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()

    var checked by remember { mutableStateOf(false) }

    MessengerModalContent {
        Text(text = "${i18n.resetWarningIsPermanent()} ${i18n.resetWarningLostAccessAndReVerify()} ${i18n.resetWarningLastResort()}")

        Spacer(Modifier.size(10.dp))

        Row(
            Modifier.fillMaxWidth().clickable { checked = !checked },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it })
            Spacer(Modifier.size(10.dp))
            Text(i18n.resetWarningAcknowledge())
        }
    }

    MessengerModalButtonRow(
        {
            BackButton { selfVerificationViewModel.backToHelp() }
        },
        {
            CloseModalButton(
                selfVerificationViewModel::close,
                i18n.redoSelfVerificationContinueWithoutVerification(),
            )
        },
        {
            NextButton(
                text = i18n.resetProceed(),
                enabled = checked,
                nextAction = selfVerificationViewModel::resetRecovery,
            )
        })
}

@Composable
fun ColumnScope.ShowSelfVerificationMethods(selfVerificationViewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    val selfVerificationMethods = selfVerificationViewModel.selfVerificationMethods.collectAsState()
    val selectedVerificationMethod = remember { mutableStateOf<SelfVerificationMethod?>(null) }

    MessengerModalContent {
        Text(i18n.selfVerificationMethodsTitle())
        Spacer(Modifier.size(10.dp))

        selfVerificationMethods.value.forEachIndexed { index, method ->
            when (method) {
                is SelfVerificationMethod.CrossSignedDeviceVerification -> {
                    Column(Modifier.padding(top = if (index == 0) 0.dp else 20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedVerificationMethod.value = method }
                        ) {
                            RadioButton(
                                selected = selectedVerificationMethod.value == method,
                                onClick = { selectedVerificationMethod.value = method },
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
                            modifier = Modifier.clickable { selectedVerificationMethod.value = method }
                        ) {
                            RadioButton(
                                selected = selectedVerificationMethod.value == method,
                                onClick = { selectedVerificationMethod.value = method }
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
                            modifier = Modifier.clickable { selectedVerificationMethod.value = method }
                        ) {
                            RadioButton(
                                selected = selectedVerificationMethod.value == method,
                                onClick = { selectedVerificationMethod.value = method }
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
    }

    MessengerModalButtonRow(
        {
            BackButton(
                selfVerificationViewModel::backToHelp
            )
        },
        {
            CloseModalButton(
                selfVerificationViewModel::close,
                i18n.redoSelfVerificationContinueWithoutVerification(),
            )
        },
        {
            NextButton(enabled = selectedVerificationMethod.value != null) {
                selectedVerificationMethod.value?.let { selfVerificationViewModel.launchVerification(it) }
            }
        }
    )
}

@Composable
fun ColumnScope.ShowPassphraseMethod(
    selfVerificationViewModel: SelfVerificationViewModel,
) {
    val i18n = DI.get<I18nView>()
    val passphraseWrong = selfVerificationViewModel.passphraseWrong.collectAsState()
    val error = selfVerificationViewModel.error.collectAsState()
    val passphrase = remember { mutableStateOf("") }

    MessengerModalContent {
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
fun ColumnScope.ShowRecoveryKeyMethod(
    selfVerificationViewModel: SelfVerificationViewModel
) {
    val i18n = DI.get<I18nView>()
    val recoveryKeyWrong = selfVerificationViewModel.recoveryKeyWrong.collectAsState()
    val error = selfVerificationViewModel.error.collectAsState()
    val recoveryKey = remember { mutableStateOf("") }

    MessengerModalContent {
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

    MessengerModalButtonRow(
        { BackButton { selfVerificationViewModel.backToChoose() } },
        {
            NextButton(enabled = recoveryKey.value.isNotBlank(), text = i18n.commonVerify()) {
                selfVerificationViewModel.verifyWithRecoveryKey(recoveryKey.value)
            }
        }
    )
}
