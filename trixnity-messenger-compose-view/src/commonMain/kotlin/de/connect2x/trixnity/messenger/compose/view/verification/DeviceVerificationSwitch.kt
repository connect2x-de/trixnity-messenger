package de.connect2x.trixnity.messenger.compose.view.verification

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Wizard
import de.connect2x.trixnity.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.trixnity.messenger.compose.view.common.WizardStep
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.verification.SelectVerificationMethodViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCompareViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRequestViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Wrapper
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationMethod

@Composable
fun BoxScope.DeviceVerificationWizardStepSwitch(
    viewModel: VerificationViewModel
) {
    val i18n = DI.get<I18nView>()
    val selectedVerificationMethod =
        remember { mutableStateOf<VerificationMethod?>(null) }
    val childState = viewModel.stack.subscribeAsState()
    val step = WizardStep(
        id = "DEVICE_VERIFICATION_WIZARD",
        title = { i18n.deviceVerification() },
        content = {
            when (val child = childState.value.active.instance) {
                is Wrapper.Request -> DeviceVerificationWizardRequest(child.viewModel)
                is Wrapper.Wait -> DeviceVerificationWizardWaitForOther()
                is Wrapper.SelectVerificationMethod -> DeviceVerificationWizardSelectVerificationMethod(
                    child.viewModel,
                    selectedVerificationMethod
                )

                is Wrapper.AcceptSasStart -> DeviceVerificationWizardAcceptSasStart()
                is Wrapper.CompareEmojisOrNumbers -> DeviceVerificationWizardCompareEmojisOrNumbers(child.viewModel)
                is Wrapper.Success -> DeviceVerificationWizardSuccess()

                is Wrapper.Rejected -> DeviceVerificationWizardRejected()
                is Wrapper.Timeout -> DeviceVerificationWizardTimeout()
                is Wrapper.Cancelled -> DeviceVerificationWizardCancelled()
                // not applicable for device verifications
                is Wrapper.AcceptedByOtherClient -> Unit
                is Wrapper.None -> Unit
            }
        },
        nextButton = {
            Custom {
                when (val child = childState.value.active.instance) {
                    is Wrapper.Request ->
                        ThemedButton(
                            style = MaterialTheme.components.primaryButton,
                            onClick = child.viewModel::next,
                        ) {
                            Text(i18n.commonNext().capitalize(Locale.current))
                        }

                    is Wrapper.CompareEmojisOrNumbers ->
                        ThemedButton(
                            style = MaterialTheme.components.primaryButton,
                            onClick = child.viewModel::accept,
                            modifier = Modifier.weight(1.0f, fill = false)
                        ) {
                            Text(i18n.verificationMatch())
                        }

                    else -> {}
                }
            }
        },
        additionalButton = {
            when (val child = childState.value.active.instance) {
                is Wrapper.Wait ->
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { viewModel.cancel() },
                    ) {
                        Text(i18n.commonCancel())
                    }

                is Wrapper.SelectVerificationMethod -> {
                    OkButton {
                        selectedVerificationMethod.value?.let {
                            child.viewModel.acceptVerificationMethod(it)
                        }
                    }
                }

                is Wrapper.AcceptSasStart -> {
                    OkButton(child.viewModel::accept)
                }

                is Wrapper.Success -> {
                    OkButton(child.viewModel::ok)
                }

                is Wrapper.Rejected -> {
                    OkButton(child.viewModel::ok)
                }

                is Wrapper.Timeout -> {
                    OkButton(child.viewModel::ok)
                }

                is Wrapper.Cancelled -> {
                    OkButton(child.viewModel::ok)
                }

                else -> Unit
            }
        },
        backButton = {
            Custom {
                when (val child = childState.value.active.instance) {
                    is Wrapper.CompareEmojisOrNumbers ->
                        ThemedButton(
                            style = MaterialTheme.components.destructiveButton,
                            onClick = child.viewModel::decline,
                            modifier = Modifier.weight(1.0f, fill = false),
                        ) {
                            Text(i18n.verificationNotMatch())
                        }

                    else -> Unit
                }
            }
        }
    )
    Wizard(listOf(step), wizardId = "DeviceVerificationWizardStep")
}


@Composable
fun DeviceVerificationWizardRequest(verificationStepRequestViewModel: VerificationStepRequestViewModel) {
    val i18n = DI.get<I18nView>()
    val deviceDisplayName = verificationStepRequestViewModel.theirDeviceDisplayName.collectAsState().value
    val theirDisplayName = verificationStepRequestViewModel.theirDisplayName.collectAsState().value

    Column {
        theirDisplayName?.let {
            Text(i18n.deviceVerificationInitiatedBy(it))
        }
        Text(i18n.deviceVerificationToAccount(deviceDisplayName))
    }
}

@Composable
fun DeviceVerificationWizardWaitForOther() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DeviceVerificationWaitForOtherContent()
    }

}

@Composable
fun DeviceVerificationWizardSelectVerificationMethod(
    selectVerificationMethodViewModel: SelectVerificationMethodViewModel,
    selectedVerificationMethod: MutableState<VerificationMethod?>
) {
    SelectVerificationMethodContent(selectVerificationMethodViewModel, selectedVerificationMethod)
}

@Composable
fun DeviceVerificationWizardAcceptSasStart() {
    val i18n = DI.get<I18nView>()
    Column {
        Text(i18n.verificationStartEmoji())
    }
}

@Composable
fun BoxScope.DeviceVerificationWizardCompareEmojisOrNumbers(verificationStepCompareViewModel: VerificationStepCompareViewModel) {
    CompareEmojisOrNumbersContent(verificationStepCompareViewModel)
}

@Composable
fun DeviceVerificationWizardSuccess() {
    val i18n = DI.get<I18nView>()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.verificationSuccess())
            Icon(
                Icons.Default.CheckCircle,
                i18n.commonSuccess(),
                tint = MaterialTheme.messengerColors.success
            )
        }
    }
}

@Composable
fun DeviceVerificationWizardRejected(
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationRejectedContent(true)
    }
}

@Composable
fun DeviceVerificationWizardTimeout(
) {
    VerificationTimeoutContent(true)
}

@Composable
fun DeviceVerificationWizardCancelled(
) {
    VerificationCancelledContent(true)
}

@Composable
private fun OkButton(onClick: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1.0f, fill = true))
        ThemedButton(
            style = MaterialTheme.components.primaryButton,
            onClick = onClick,
        ) {
            Text(i18n.commonOk())
        }
    }
}
