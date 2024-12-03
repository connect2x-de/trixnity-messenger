package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.CloseModalButton
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
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

// FIXME all configurable?

@Composable
fun DeviceVerificationRequest(verificationStepRequestViewModel: VerificationStepRequestViewModel) {
    val i18n = DI.get<I18nView>()
    val deviceDisplayName = verificationStepRequestViewModel.deviceDisplayName.collectAsState().value
    val theirDisplayName = verificationStepRequestViewModel.theirDisplayName.collectAsState().value

    Column {
        theirDisplayName?.let {
            Text(i18n.deviceVerificationInitiatedBy(it))
        }
        Text(i18n.deviceVerificationToAccount(deviceDisplayName))
        Spacer(Modifier.size(20.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1.0f, fill = true))
            Button(verificationStepRequestViewModel::next, Modifier.buttonPointerModifier()) {
                Text(i18n.commonNext().capitalize(Locale.current))
            }
        }
    }
}

@Composable
fun DeviceVerificationWaitForOther(cancelAction: (() -> Unit)? = null) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoadingSpinner()
        Spacer(Modifier.size(20.dp))
        Text(i18n.verificationWait())
        cancelAction?.let {
            MessengerModalButtonRow(
                {
                    CloseModalButton(
                        caption = i18n.commonCancel(),
                        closeModalAction = cancelAction,
                    )
                }
            )
        }
    }
}

@Composable
fun SelectVerificationMethod(selectVerificationMethodViewModel: SelectVerificationMethodViewModel) {
    val verificationMethods = selectVerificationMethodViewModel.verificationMethods
    val selectedVerificationMethod =
        remember { mutableStateOf(verificationMethods.firstOrNull()?.first) }
    Column {
        verificationMethods.forEach { (verificationMethod, explanation) ->
            if (selectVerificationMethodViewModel.hasSelection) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedVerificationMethod.value = verificationMethod }
                ) {
                    RadioButton(
                        selected = selectedVerificationMethod.value == verificationMethod,
                        onClick = { selectedVerificationMethod.value = verificationMethod },
                    )
                    Text(
                        text = explanation,
                    )
                }
            } else {
                Text(
                    text = explanation,
                )
            }
        }
        Spacer(Modifier.size(20.dp))
        OkButton { selectedVerificationMethod.value?.let { selectVerificationMethodViewModel.acceptVerificationMethod(it) } }
    }
}

@Composable
fun AcceptSasStart(acceptSasStartViewModel: AcceptSasStartViewModel) {
    val i18n = DI.get<I18nView>()
    Column {
        Text(i18n.verificationStartEmoji())
        Spacer(Modifier.size(20.dp))
        OkButton(acceptSasStartViewModel::accept)
    }

}

@Composable
fun BoxScope.CompareEmojisOrNumbers(verificationStepCompareViewModel: VerificationStepCompareViewModel) {
    val i18n = DI.get<I18nView>()
    val emojis = verificationStepCompareViewModel.emojis
    val decimals = verificationStepCompareViewModel.decimals
    Column(Modifier.fillMaxWidth().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        if (emojis.isNotEmpty()) {
            Text(i18n.verificationEmojiComparison())
            Spacer(Modifier.size(20.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    emojis.take(4).map { Emoji(it, this@BoxWithConstraints.maxWidth / 4) }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    emojis.drop(4).map { Emoji(it, this@BoxWithConstraints.maxWidth / 4) }
                }
            }
        } else {
            Text(i18n.verificationNumberComparison())
            Spacer(Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                decimals.take(2).map { Number(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                decimals.drop(2).map { Number(it) }
            }
        }
        Spacer(Modifier.size(20.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                verificationStepCompareViewModel::decline,
                Modifier.buttonPointerModifier().weight(1.0f, fill = false),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(i18n.verificationNotMatch(), color = Color.White)
            }
            Spacer(Modifier.size(20.dp))
            Button(
                verificationStepCompareViewModel::accept,
                Modifier.buttonPointerModifier().weight(1.0f, fill = false)
            ) {
                Text(i18n.verificationMatch())
            }
        }
    }
}

@Composable
fun DeviceVerificationSuccess(verificationStepSuccessViewModel: VerificationStepSuccessViewModel) {
    val i18n = DI.get<I18nView>()
    val verifiedDeviceName = verificationStepSuccessViewModel.verifiedDeviceName.collectAsState().value
    val verifyingDeviceName = verificationStepSuccessViewModel.verifyingDeviceName.collectAsState().value ?: i18n.commonUnknown()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.verificationSuccess(verifiedDeviceName, verifyingDeviceName))
            Icon(Icons.Default.CheckCircle, i18n.commonSuccess(), tint = MaterialTheme.messengerColors.success)
        }
        Spacer(Modifier.size(20.dp))
        OkButton(verificationStepSuccessViewModel::ok)
    }
}

@Composable
fun VerificationRejected(
    verificationStepRejectedViewModel: VerificationStepRejectedViewModel,
    deviceVerification: Boolean = true,
    isEmbedded: Boolean = false
) {
    val i18n = DI.get<I18nView>()
    Column {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Cancel,
                i18n.commonCancelled(),
                Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(text = i18n.verificationRejected(if (deviceVerification) i18n.deviceVerification() else i18n.userVerification()))
        }
        if (deviceVerification && !isEmbedded) {
            Spacer(Modifier.size(20.dp))
            OkButton(verificationStepRejectedViewModel::ok)
        }
    }
}

@Composable
fun VerificationTimeout(
    verificationStepTimeoutViewModel: VerificationStepTimeoutViewModel,
    deviceVerification: Boolean = true,
    isEmbedded: Boolean = false
) {
    val i18n = DI.get<I18nView>()
    Column {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Cancel,
                i18n.commonCancelled(),
                Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(i18n.verificationTimeout(if (deviceVerification) i18n.deviceVerification() else i18n.userVerification()))
        }
        if (deviceVerification && !isEmbedded) {
            Spacer(Modifier.size(20.dp))
            OkButton(verificationStepTimeoutViewModel::ok)
        }
    }
}

@Composable
fun VerificationCancelled(
    verificationStepCancelledViewModel: VerificationStepCancelledViewModel,
    deviceVerification: Boolean = true,
    isEmbedded: Boolean = false
) {
    val i18n = DI.get<I18nView>()
    Column {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Cancel,
                i18n.commonCancelled(),
                Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(text = i18n.verificationCancelled(if (deviceVerification) i18n.deviceVerification() else i18n.userVerification()))
        }
        if (deviceVerification && !isEmbedded) {
            Spacer(Modifier.size(20.dp))
            OkButton(verificationStepCancelledViewModel::ok)
        }
    }
}

@Composable
private fun Emoji(emoji: Pair<String, Map<String, String?>>, maxWidth: Dp) {
    val i18n = DI.get<I18nView>()
    Column(
        Modifier
            .padding(10.dp)
            .sizeIn(maxWidth = maxWidth - 10.dp), // - padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji.first, style = MaterialTheme.typography.bodyLarge)
        Text(
            emoji.second[Locale.current.language.lowercase()] ?: i18n.commonUnknown(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Number(number: Int) {
    Text(number.toString(), fontSize = 28.sp)
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
