package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.client.verification.SelfVerificationMethod
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.ThemedLoadingButton
import de.connect2x.trixnity.messenger.compose.view.common.ThemedLoadingIconButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardSection
import de.connect2x.trixnity.messenger.compose.view.form.AutofillButton
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.isWeb
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.OutlinedTextFieldWithToolbar
import de.connect2x.trixnity.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel

@Composable
fun SelfVerificationSteps(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    val availableSelfVerificationMethods by viewModel.availableSelfVerificationMethods.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SelfVerificationExplanation()
        if (availableSelfVerificationMethods == null) {
            WizardSection {
                LoadingSpinner()
                Text(i18n.selfVerificationWaitingForMethods())
            }
        } else {
            VerifyWithOtherDevice(viewModel)
            RecoveryKey(viewModel)
            PasswordPhrase(viewModel)
            ResetOptions(viewModel)
        }
    }
}

@Composable
fun SelfVerificationExplanation() {
    val i18n = DI.get<I18nView>()
    val expandedExplanation = remember { mutableStateOf(false) }
    WizardSection {
        ExpandableSection(
            heading = { Text(i18n.selfVerificationHelpReasonTitle()) },
            icon = Icons.Outlined.Info,
            expanded = expandedExplanation,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(i18n.selfVerificationHelpVerifyThis())
                Text(i18n.selfVerificationHelpReason1())
                Text(i18n.selfVerificationHelpReason2())
                Text(i18n.selfVerificationHelpReason3())
            }
        }
    }
}

@Composable
fun VerifyWithOtherDevice(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()

    WizardSection {
        Text(
            i18n.selfVerificationMethodsOtherDevice().capitalize(Locale.current),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.selfVerificationMethodsOtherDeviceInfo(), modifier = Modifier.weight(1.0f, fill = true))
            SmallSpacer()
            FilledIconButton(
                onClick = { viewModel.verifyWithOtherDevice() },
                modifier = Modifier.buttonPointerModifier(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun RecoveryKey(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    val selfVerificationMethods by viewModel.availableSelfVerificationMethods.collectAsState()

    if (selfVerificationMethods?.any { it is SelfVerificationMethod.AesHmacSha2RecoveryKey } == true) {
        var recoveryKey by viewModel.recoveryKey.collectAsTextFieldValueState()
        val recoveryKeyWrong by viewModel.recoveryKeyWrong.collectAsState()
        val error by viewModel.error.collectAsState()
        val recoveryKeyVerificationInProgress by viewModel.recoveryKeyVerificationInProgress.collectAsState()

        WizardSection {
            Text(
                i18n.selfVerificationMethodsRecoveryKey().capitalize(Locale.current),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
            ) {
                OutlinedTextFieldWithToolbar(
                    recoveryKey,
                    onValueChange = { recoveryKey = it },
                    label = { Text(i18n.commonRecoveryKey()) },
                    placeholder = { Text("#### ".repeat(11) + "####", color = Color.LightGray) },
                    modifier = Modifier.weight(1.0f, fill = true),
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                        ),
                    minLines = 2,
                    maxLines = 2,
                    isError = recoveryKeyWrong,
                    supportingText = {
                        if (recoveryKeyWrong)
                            Text(
                                i18n.selfVerificationMethodsRecoveryKeyWrong(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                    },
                )
                if (Platform.current.isWeb) {
                    AutofillButton(onUsernameChange = {}, onPasswordChange = { recoveryKey = TextFieldValue(it) })
                }
                ThemedLoadingIconButton(
                    onClick = { viewModel.verifyWithRecoveryKey() },
                    isLoading = recoveryKeyVerificationInProgress,
                    enabled = recoveryKeyVerificationInProgress.not(),
                    style = MaterialTheme.components.primaryIconButton,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                }
            }
            error?.let {
                MiddleSpacer()
                ErrorView(it)
            }
        }
    }
}

@Composable
fun PasswordPhrase(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    val selfVerificationMethods by viewModel.availableSelfVerificationMethods.collectAsState()

    if (
        selfVerificationMethods?.any { it is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase } == true
    ) {

        var passphrase by viewModel.passphrase.collectAsTextFieldValueState()
        val passphraseWrong by viewModel.passphraseWrong.collectAsState()
        val error by viewModel.error.collectAsState()
        val passphraseVerificationInProgress by viewModel.passphraseVerificationInProgress.collectAsState()

        WizardSection {
            Text(
                i18n.selfVerificationMethodsRecoveryPassphrase().capitalize(Locale.current),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
            ) {
                OutlinedTextFieldWithToolbar(
                    passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(i18n.commonRecoveryPassphrase()) },
                    modifier = Modifier.weight(1.0f, fill = true),
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                        ),
                    maxLines = 2,
                    isError = passphraseWrong,
                    supportingText = {
                        if (passphraseWrong)
                            Text(
                                i18n.selfVerificationMethodsRecoveryPassphraseWrong(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                    },
                )
                if (Platform.current.isWeb) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AutofillButton(onUsernameChange = {}, onPasswordChange = { passphrase = TextFieldValue(it) })
                    }
                }
                ThemedLoadingButton(
                    onClick = {},
                    isLoading = passphraseVerificationInProgress,
                    enabled = passphraseVerificationInProgress.not(),
                    style = MaterialTheme.components.primaryButton.copy(contentPadding = PaddingValues(0.dp)),
                ) {
                    FilledIconButton(
                        onClick = { viewModel.verifyWithPassphrase() },
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                    }
                }
            }
            error?.let {
                MiddleSpacer()
                ErrorView(it)
            }
        }
    }
}

@Composable
fun ResetOptions(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardSection {
        val expandedResetOptions = remember { mutableStateOf(false) }
        ExpandableSection(
            heading = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, tint = Color.Red, contentDescription = null)
                    SmallSpacer()
                    Text(i18n.commonOtherOptions())
                }
            },
            expanded = expandedResetOptions,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                NoVerification(viewModel)
                ResetAccount(viewModel)
            }
        }
    }
}

@Composable
fun NoVerification(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedSurface(
        style =
            SurfaceStyle.default(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(20.dp),
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)) {
            Text(
                i18n.redoSelfVerificationContinueWithoutVerification(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1.0f, fill = true),
                ) {
                    Text(text = i18n.redoSelfVerificationDoItLater())
                    Text(i18n.redoSelfVerificationWarning1())
                    Text(i18n.redoSelfVerificationWarning2())
                    Text(i18n.redoSelfVerificationWarning3())
                }
                SmallSpacer()
                FilledIconButton(
                    onClick = { viewModel.continueWithoutVerification() },
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.buttonPointerModifier(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ResetAccount(viewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    var resetChecked by remember { mutableStateOf(false) }
    ThemedSurface(
        style =
            SurfaceStyle.default(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(20.dp),
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)) {
            Text(
                i18n.selfVerificationResetRecoveryKey(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red,
            )
            Text(i18n.selfVerificationResetRecoveryKeyDescription())
            Text(i18n.resetWarningIsPermanent())
            MiddleSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(i18n.resetWarningAcknowledge(), modifier = Modifier.weight(1.0f, fill = true))
                SmallSpacer()
                ThemedSwitch(checked = resetChecked, onCheckedChange = { resetChecked = !resetChecked })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(i18n.resetProceed(), modifier = Modifier.weight(1.0f, fill = true))
                SmallSpacer()
                FilledIconButton(
                    onClick = { viewModel.resetRecovery() },
                    enabled = resetChecked,
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.buttonPointerModifier(enabled = resetChecked),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
