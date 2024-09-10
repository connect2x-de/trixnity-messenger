package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel

interface DeviceVerificationModalView {
    @Composable
    fun create(verificationViewModel: VerificationViewModel)
}

@Composable
fun DeviceVerificationModal(verificationViewModel: VerificationViewModel) {
    DI.current.get<DeviceVerificationModalView>().create(verificationViewModel)
}

class DeviceVerificationModalViewImpl : DeviceVerificationModalView {
    @Composable
    override fun create(verificationViewModel: VerificationViewModel) {
        val i18n = DI.current.get<I18nView>()
        // TODO: Close the modal instead of cancelling the current process here.
        MessengerModal(verificationViewModel::cancel, i18n.deviceVerificationTitle()) {
            Box(Modifier.fillMaxWidth()) {
                DeviceVerificationStepSwitch(verificationViewModel)
            }
        }
    }
}
