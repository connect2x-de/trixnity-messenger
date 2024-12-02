package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel

interface DeviceVerificationWizardView {
    @Composable
    fun create(verificationViewModel: VerificationViewModel)
}

@Composable
fun DeviceVerificationWizard(verificationViewModel: VerificationViewModel) {
    DI.get<DeviceVerificationWizardView>().create(verificationViewModel)
}

class DeviceVerificationWizardViewImpl : DeviceVerificationWizardView {
    @Composable
    override fun create(verificationViewModel: VerificationViewModel) {
        val i18n = DI.get<I18nView>()
        val step = WizardStep(
            id = "DEVICE-VERIFICATION-WIZARD-VERIFICATION",
            title = { i18n.deviceVerification() },
            content = {
                Box(Modifier.fillMaxWidth()) {
                    DeviceVerificationStepSwitch(verificationViewModel, true)
                }
            },
            additionalButton = {
                Button(modifier = Modifier.buttonPointerModifier(),
                    onClick = { verificationViewModel.cancel() }) {
                    Text(i18n.commonCancel())
                }
            }
        )
        Wizard(listOf(step))
    }
}
