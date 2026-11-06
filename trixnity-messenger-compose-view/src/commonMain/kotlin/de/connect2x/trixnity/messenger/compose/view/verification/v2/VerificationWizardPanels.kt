package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationMethod
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.wizard.NextButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.OkButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardPanel
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

@Composable
fun VerificationRequestWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationRequestContent(viewModel, true) },
        buttons = { NextButton { viewModel.acceptRequest() } },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationSelectVerificationMethodWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.current.get<I18nView>()
    val selectedVerificationMethod = remember { mutableStateOf<VerificationMethod?>(null) }
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationSelectVerificationMethodContent(viewModel, selectedVerificationMethod) },
        buttons = {
            val selectedVerificationMethod = selectedVerificationMethod.value
            OkButton(
                { selectedVerificationMethod?.let { viewModel.acceptVerificationMethod(selectedVerificationMethod) } },
                selectedVerificationMethod != null,
            )
        },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationAcceptSasStartWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()

    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationAcceptSasStartContent() },
        buttons = { OkButton(onClick = { viewModel.acceptSasVerification() }) },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationCancelledWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationCancelledContent(true) },
        buttons = { OkButton(viewModel::ok) },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationCompareEmojisOrNumbersWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { Box { VerificationCompareEmojisOrNumbersContent(viewModel) } },
        buttons = {
            ThemedButton(
                style = MaterialTheme.components.destructiveButton,
                onClick = viewModel::declineEmojisOrNumbers,
            ) {
                Text(i18n.verificationNotMatch())
            }
            NextButton(text = i18n.verificationMatch(), nextAction = viewModel::acceptEmojisOrNumbers)
        },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationRejectedWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationRejectedContent(true) },
        buttons = { OkButton(viewModel::ok) },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationTimeoutWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationTimeoutContent(true) },
        buttons = { OkButton(onClick = viewModel::ok) },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationWaitForOtherWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationWaitForOtherContent() },
        buttons = {
            ThemedButton(style = MaterialTheme.components.primaryButton, onClick = { viewModel.cancel() }) {
                Text(i18n.commonCancel())
            }
        },
        wizardId = "Verification",
    )
}

@Composable
fun VerificationSuccessWizardPanel(viewModel: VerificationViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.deviceVerification() },
        content = { VerificationSuccessContent(true) },
        buttons = { OkButton(onClick = viewModel::ok) },
        wizardId = "Verification",
    )
}
