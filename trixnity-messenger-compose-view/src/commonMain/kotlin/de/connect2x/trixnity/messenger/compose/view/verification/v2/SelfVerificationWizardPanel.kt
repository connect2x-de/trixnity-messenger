package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardPanel
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel

@Composable
fun SelfVerificationWizardPanel(selfVerificationViewModel: SelfVerificationViewModel) {
    val i18n = DI.get<I18nView>()
    val isVerified = selfVerificationViewModel.isVerified.collectAsState().value
    if (isVerified == false) {
        WizardPanel(
            title = { i18n.deviceVerification() },
            subTitle = { "${i18n.commonAccount()}: ${selfVerificationViewModel.userId.full}" },
            content = { SelfVerificationSteps(selfVerificationViewModel) },
            wizardId = "SelfVerificationWizard",
        )
    }
}
