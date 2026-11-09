package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.CloseModalButton
import de.connect2x.trixnity.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.wizard.NextButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.OkButton
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

@Composable
fun VerificationSelectVerificationMethod(verificationViewModel: VerificationViewModel) {
    val verificationMethods = verificationViewModel.verificationMethods.collectAsState().value
    val selectedVerificationMethod = remember { mutableStateOf(verificationMethods?.firstOrNull()?.first) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationSelectVerificationMethodContent(verificationViewModel, selectedVerificationMethod)
        MiddleSpacer()
        OkButton(
            { selectedVerificationMethod.value?.let { verificationViewModel.acceptVerificationMethod(it) } },
            enabled = selectedVerificationMethod.value != null,
        )
    }
}

@Composable
fun VerificationCancelled(verificationViewModel: VerificationViewModel, isDeviceVerification: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationCancelledContent(isDeviceVerification)
        if (isDeviceVerification) {
            MiddleSpacer()
            OkButton(verificationViewModel::ok)
        }
    }
}

@Composable
fun BoxScope.VerificationCompareEmojisOrNumbers(verificationViewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box { VerificationCompareEmojisOrNumbersContent(verificationViewModel) }
        Spacer(Modifier.size(20.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            ThemedButton(
                style = MaterialTheme.components.destructiveButton,
                onClick = verificationViewModel::declineEmojisOrNumbers,
                modifier = Modifier.weight(1.0f, fill = false),
            ) {
                Text(i18n.verificationNotMatch(), color = Color.White)
            }
            MiddleSpacer()
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = verificationViewModel::acceptEmojisOrNumbers,
            ) {
                Text(i18n.verificationMatch())
            }
        }
    }
}

@Composable
fun VerificationRejected(verificationViewModel: VerificationViewModel, isDeviceVerification: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationRejectedContent(isDeviceVerification)
        if (isDeviceVerification) {
            MiddleSpacer()
            OkButton(verificationViewModel::ok)
        }
    }
}

@Composable
fun VerificationTimeout(verificationViewModel: VerificationViewModel, isDeviceVerification: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationTimeoutContent(isDeviceVerification)
        if (isDeviceVerification) {
            MiddleSpacer()
            OkButton(verificationViewModel::ok)
        }
    }
}

@Composable
fun VerificationWaitForOther(cancelAction: (() -> Unit)? = null) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationWaitForOtherContent()
        MiddleSpacer()
        cancelAction?.let {
            MessengerModalButtonRow({
                CloseModalButton(caption = i18n.commonCancel(), closeModalAction = cancelAction)
            })
        }
    }
}

@Composable
fun VerificationSuccess(verificationViewModel: VerificationViewModel, isDeviceVerification: Boolean) {
    Column {
        VerificationSuccessContent(isDeviceVerification)
        MiddleSpacer()
        OkButton(verificationViewModel::ok)
    }
}

@Composable
fun VerificationAcceptSasStart(verificationViewModel: VerificationViewModel) {
    Column {
        VerificationAcceptSasStartContent()
        MiddleSpacer()
        OkButton(verificationViewModel::acceptSasVerification)
    }
}

@Composable
fun VerificationRequest(verificationViewModel: VerificationViewModel, isDeviceVerification: Boolean) {
    Column {
        VerificationRequestContent(verificationViewModel, isDeviceVerification)
        MiddleSpacer()
        NextButton(nextAction = verificationViewModel::acceptRequest)
    }
}

@Composable
fun VerificationAcceptedByOtherClient(verificationViewModel: VerificationViewModel) {
    VerificationAcceptedByOtherClientContent()
}
