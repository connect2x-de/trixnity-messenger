package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.RunningText
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.wizard.NextButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardPanel
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel

@Composable
fun RedoSelfVerificationWizardPanel(redoSelfVerificationViewModel: RedoSelfVerificationViewModel) {
    val i18n = DI.current.get<I18nView>()
    WizardPanel(
        title = { i18n.redoSelfVerificationTitle(redoSelfVerificationViewModel.userId) },
        content = {
            Column {
                Row(Modifier.padding(bottom = MaterialTheme.messengerDpConstants.small)) {
                    Icon(
                        Icons.Default.Warning,
                        i18n.commonWarning(),
                        modifier = Modifier.padding(end = MaterialTheme.messengerDpConstants.small),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(i18n.bootstrapRecoveryKeyAttention(), style = MaterialTheme.typography.titleMedium)
                }
                RunningText(i18n.redoSelfVerificationWarning1())
                RunningText(i18n.redoSelfVerificationWarning2())
                RunningText(i18n.redoSelfVerificationWarning3())
                SmallSpacer()
                RunningText(i18n.redoSelfVerificationDoIt())
                RunningText(i18n.redoSelfVerificationDoItLater())
            }
        },
        buttons = {
            ThemedButton(
                onClick = redoSelfVerificationViewModel::close,
                style = MaterialTheme.components.destructiveButton,
            ) {
                Text(i18n.redoSelfVerificationContinueWithoutVerification())
            }
            NextButton(
                text = i18n.redoSelfVerificationRedo(),
                nextAction = redoSelfVerificationViewModel::startSelfVerification,
            )
        },
        wizardId = "RedoSelfVerification",
    )
}
