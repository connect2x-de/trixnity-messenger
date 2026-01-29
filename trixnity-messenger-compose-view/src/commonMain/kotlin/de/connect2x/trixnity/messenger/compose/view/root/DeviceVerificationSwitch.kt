package de.connect2x.trixnity.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.trixnity.messenger.compose.view.verification.DeviceVerificationWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter

@Composable
fun DeviceVerificationSwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.deviceVerificationRouterStack) {
        when (val child = it.instance) {
            is VerificationRouter.Wrapper.Verification -> DeviceVerificationWizard(child.viewModel)
            is VerificationRouter.Wrapper.None -> Box {}
        }
    }
}
