package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Wrapper

@Composable
fun BoxScope.DeviceVerificationStepSwitch(
    viewModel: VerificationViewModel,
) {
    Children(
        stack = viewModel.stack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is Wrapper.Request -> DeviceVerificationRequest(child.viewModel)
            is Wrapper.Wait -> DeviceVerificationWaitForOther(viewModel::cancel)
            is Wrapper.SelectVerificationMethod -> SelectVerificationMethod(child.viewModel)
            is Wrapper.AcceptSasStart -> AcceptSasStart(child.viewModel)
            is Wrapper.CompareEmojisOrNumbers -> CompareEmojisOrNumbers(child.viewModel)
            is Wrapper.Success -> DeviceVerificationSuccess(child.viewModel)
            is Wrapper.Rejected -> VerificationRejected(child.viewModel)
            is Wrapper.Timeout -> VerificationTimeout(child.viewModel)
            is Wrapper.Cancelled -> VerificationCancelled(child.viewModel)
            is Wrapper.AcceptedByOtherClient -> Box {} // not applicable for device verifications
            is Wrapper.None -> Box {}
        }.let {}
    }
}
