package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.verification.AcceptSasStartViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelectVerificationMethodViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCancelledViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCompareViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRejectedViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRequestViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepSuccessViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepTimeoutViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Wrapper

@Composable
fun BoxScope.DeviceVerificationStepSwitch(
    viewModel: VerificationViewModel
) {
    Children(
        stack = viewModel.stack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is Wrapper.Request -> DeviceVerificationWizardRequest(child.viewModel)
            is Wrapper.Wait -> DeviceVerificationWizardWaitForOther(viewModel::cancel)
            is Wrapper.SelectVerificationMethod -> DeviceVerificationWizardSelectVerificationMethod(child.viewModel)
            is Wrapper.AcceptSasStart -> DeviceVerificationWizardAcceptSasStart(child.viewModel)
            is Wrapper.CompareEmojisOrNumbers -> DeviceVerificationWizardCompareEmojisOrNumbers(child.viewModel)
            is Wrapper.Success -> DeviceVerificationWizardSuccess(child.viewModel)

            is Wrapper.Rejected -> DeviceVerificationWizardRejected(child.viewModel)
            is Wrapper.Timeout -> DeviceVerificationWizardTimeout(child.viewModel)
            is Wrapper.Cancelled -> DeviceVerificationWizardCancelled(child.viewModel)
            is Wrapper.AcceptedByOtherClient -> Box {} // not applicable for device verifications
            is Wrapper.None -> Box {}
        }.let {}
    }
}

@Composable
fun DeviceVerificationWizardRequest(verificationStepRequestViewModel: VerificationStepRequestViewModel) {
    val i18n = DI.get<I18nView>()
    val deviceDisplayName = verificationStepRequestViewModel.ourDeviceDisplayName.collectAsState().value
    val theirDisplayName = verificationStepRequestViewModel.theirDisplayName.collectAsState().value

    val step = WizardStep(
        id = "DEVICE_VERIFICATION_WIZARD_REQUEST",
        title = { i18n.deviceVerification() },
        content = {
            Column {
                theirDisplayName?.let {
                    Text(i18n.deviceVerificationInitiatedBy(it))
                }
                Text(i18n.deviceVerificationToAccount(deviceDisplayName))
                Spacer(Modifier.size(20.dp))
            }
        },
        nextButton = {
            Custom {
                Button(verificationStepRequestViewModel::next, Modifier.buttonPointerModifier()) {
                    Text(i18n.commonNext().capitalize(Locale.current))
                }
            }
        }
    )
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardWaitForOther(cancelAction: (() -> Unit)? = null) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "DEVICE_VERIFICATION_WIZARD_WAIT",
        title = { i18n.deviceVerification() },
        content = {
            Column {
                DeviceVerificationWaitForOtherContent()
            }
        },
        additionalButton = {
            cancelAction?.let {
                Button(onClick = cancelAction, Modifier.buttonPointerModifier()) {
                    Text(i18n.commonCancel())
                }
            }
        }
    )
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardSelectVerificationMethod(selectVerificationMethodViewModel: SelectVerificationMethodViewModel) {
    val verificationMethods = selectVerificationMethodViewModel.verificationMethods
    val selectedVerificationMethod =
        remember { mutableStateOf(verificationMethods.firstOrNull()?.first) }

    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_SELECT",
        title = { i18n.deviceVerification() },
        content = {
            SelectVerificationMethodContent(selectVerificationMethodViewModel, selectedVerificationMethod)
        },
        additionalButton = {
            OkButton {
                selectedVerificationMethod.value?.let {
                    selectVerificationMethodViewModel.acceptVerificationMethod(
                        it
                    )
                }
            }
        })
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardAcceptSasStart(acceptSasStartViewModel: AcceptSasStartViewModel) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_ACCEPT",
        title = { i18n.deviceVerification() },
        content = {
            Column {
                Text(i18n.verificationStartEmoji())
            }
        },
        additionalButton = { OkButton(acceptSasStartViewModel::accept) }

    )
    Wizard(listOf(step))

}

@Composable
fun BoxScope.DeviceVerificationWizardCompareEmojisOrNumbers(verificationStepCompareViewModel: VerificationStepCompareViewModel) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_COMPARE",
        title = { i18n.deviceVerification() },
        content = {
            CompareEmojisOrNumbersContent(verificationStepCompareViewModel)
        },
        nextButton = {
            Custom {
                Button(
                    verificationStepCompareViewModel::accept,
                    Modifier.buttonPointerModifier().weight(1.0f, fill = false)
                ) {
                    Text(i18n.verificationMatch())
                }
            }
        },
        backButton = {
            Custom {
                Button(
                    verificationStepCompareViewModel::decline,
                    Modifier.buttonPointerModifier().weight(1.0f, fill = false),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(i18n.verificationNotMatch(), color = Color.White)
                }
            }
        }
    )
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardSuccess(verificationStepSuccessViewModel: VerificationStepSuccessViewModel) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_SUCCESS",
        title = { i18n.deviceVerification() },
        content = {
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
        },
        additionalButton = {
            OkButton(verificationStepSuccessViewModel::ok)
        })
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardRejected(
    verificationStepRejectedViewModel: VerificationStepRejectedViewModel,
) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_REJECTED",
        title = { i18n.deviceVerification() },
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VerificationRejectedContent(true)
            }
        },
        additionalButton = {
            OkButton(verificationStepRejectedViewModel::ok)
        }
    )
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardTimeout(
    verificationStepTimeoutViewModel: VerificationStepTimeoutViewModel,
) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_TIMEOUT",
        title = { i18n.deviceVerification() },
        content = {
            VerificationTimeoutContent(true)
        },
        additionalButton = {
            OkButton(verificationStepTimeoutViewModel::ok)
        }
    )
    Wizard(listOf(step))
}

@Composable
fun DeviceVerificationWizardCancelled(
    verificationStepCancelledViewModel: VerificationStepCancelledViewModel,
) {
    val i18n = DI.get<I18nView>()
    val step = WizardStep(
        id = "VERIFICATION_WIZARD_CANCELLED",
        title = { i18n.deviceVerification() },
        content = {
            VerificationCancelledContent(true)
        },
        additionalButton = { OkButton(verificationStepCancelledViewModel::ok) }
    )
    Wizard(listOf(step))
}

@Composable
private fun OkButton(onClick: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1.0f, fill = true))
        Button(onClick, Modifier.buttonPointerModifier()) {
            Text(i18n.commonOk())
        }
    }
}
