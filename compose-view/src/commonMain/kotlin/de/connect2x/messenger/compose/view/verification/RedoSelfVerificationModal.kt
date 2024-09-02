package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.CloseModalButton
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.NextButton
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel

interface RedoSelfVerificationModalView {
    @Composable
    fun create(redoSelfVerificationViewModel: RedoSelfVerificationViewModel)
}

@Composable
fun RedoSelfVerificationModal(redoSelfVerificationViewModel: RedoSelfVerificationViewModel) {
    DI.current.get<RedoSelfVerificationModalView>().create(redoSelfVerificationViewModel)
}

class RedoSelfVerificationModalViewImpl : RedoSelfVerificationModalView {
    @Composable
    override fun create(redoSelfVerificationViewModel: RedoSelfVerificationViewModel) {
        val i18n = DI.current.get<I18nView>()
        MessengerModal(
            redoSelfVerificationViewModel::close,
            i18n.redoSelfVerificationTitle(redoSelfVerificationViewModel.userId),
        ) {
            Row(Modifier.padding(bottom = 10.dp)) {
                Icon(
                    Icons.Default.Warning,
                    i18n.commonWarning(),
                    modifier = Modifier.padding(end = 10.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    i18n.bootstrapRecoveryKeyAttention(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            RunningText(i18n.redoSelfVerificationWarning1())
            RunningText(i18n.redoSelfVerificationWarning2())
            RunningText(i18n.redoSelfVerificationWarning3())
            Spacer(Modifier.size(10.dp))
            RunningText(i18n.redoSelfVerificationDoIt())
            RunningText(i18n.redoSelfVerificationDoItLater())
            Spacer(Modifier.size(20.dp))
            MessengerModalButtonRow(
                {
                    CloseModalButton(
                        redoSelfVerificationViewModel::close,
                        i18n.redoSelfVerificationContinueWithoutVerification(),
                    )
                },
                {
                    NextButton(
                        text = i18n.redoSelfVerificationRedo(),
                        nextAction = redoSelfVerificationViewModel::startSelfVerification,
                    )
                })
        }
    }
}
