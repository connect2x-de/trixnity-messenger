package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
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
        Box {
            DeviceVerificationWizardStepSwitch(verificationViewModel)
        }

    }
}
