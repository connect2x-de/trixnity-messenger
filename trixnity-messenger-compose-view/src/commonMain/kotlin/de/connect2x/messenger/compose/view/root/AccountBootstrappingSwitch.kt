package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.settings.AccountBootstrappingWizard
import de.connect2x.messenger.compose.view.verification.BootstrapWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingRouter

@Composable
fun AccountBootstrappingSwitch(mainViewModel: MainViewModel) {
    Box {
        Children(mainViewModel.accountBootstrappingRouterStack) {
            when (it.instance) {
                is AccountBootstrappingRouter.Wrapper.ShowBootstrap -> AccountBootstrappingWizard(it.instance as AccountBootstrappingRouter.Wrapper.ShowBootstrap)
                is AccountBootstrappingRouter.Wrapper.ShowAccountBootstrap -> BootstrapWizard((it.instance as AccountBootstrappingRouter.Wrapper.ShowAccountBootstrap).viewModel)
                else -> Box {}
            }
        }
    }
}
