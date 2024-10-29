package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.verification.CrossSigningBootstrapWizard
import de.connect2x.messenger.compose.view.verification.RedoSelfVerificationModal
import de.connect2x.messenger.compose.view.verification.SelfVerificationWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter

@Composable
fun SelfVerificationSwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.selfVerificationStack) {
        when (val child = it.instance) {
            is SelfVerificationRouter.Wrapper.View -> SelfVerificationWizard(child.viewModel)
            is SelfVerificationRouter.Wrapper.RedoSelfVerification -> RedoSelfVerificationModal(child.viewModel)
            is SelfVerificationRouter.Wrapper.CrossSigningBootstrap -> CrossSigningBootstrapWizard(child.viewModel)
            is SelfVerificationRouter.Wrapper.None -> Box {}
        }
    }
}
