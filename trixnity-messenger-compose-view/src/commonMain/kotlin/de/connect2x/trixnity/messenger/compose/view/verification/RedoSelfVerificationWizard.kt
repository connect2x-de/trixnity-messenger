package de.connect2x.trixnity.messenger.compose.view.verification

import androidx.compose.foundation.layout.Column
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
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.CloseModalButton
import de.connect2x.trixnity.messenger.compose.view.common.RunningText
import de.connect2x.trixnity.messenger.compose.view.common.Wizard
import de.connect2x.trixnity.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.trixnity.messenger.compose.view.common.WizardStep
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel

interface RedoSelfVerificationWizardView {
    @Composable
    fun create(redoSelfVerificationViewModel: RedoSelfVerificationViewModel)
}

@Composable
fun RedoSelfVerificationWizard(redoSelfVerificationViewModel: RedoSelfVerificationViewModel) {
    DI.get<RedoSelfVerificationWizardView>().create(redoSelfVerificationViewModel)
}

class RedoSelfVerificationWizardViewImpl : RedoSelfVerificationWizardView {
    @Composable
    override fun create(redoSelfVerificationViewModel: RedoSelfVerificationViewModel) {
        val i18n = DI.get<I18nView>()
        val step = WizardStep(
            id = "REDO_SELF_VERIFICATION_WIZARD",
            title = { i18n.redoSelfVerificationTitle(redoSelfVerificationViewModel.userId) },
            content = {
                Column {
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
                }
            },
            additionalButton = {
                CloseModalButton(
                    redoSelfVerificationViewModel::close,
                    i18n.redoSelfVerificationContinueWithoutVerification(),
                )
            },
            nextButton = {
                Custom {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = redoSelfVerificationViewModel::startSelfVerification,
                    ) {
                        Text(i18n.redoSelfVerificationRedo())
                    }
                }
            }
        )
        Wizard(listOf(step))

    }
}
