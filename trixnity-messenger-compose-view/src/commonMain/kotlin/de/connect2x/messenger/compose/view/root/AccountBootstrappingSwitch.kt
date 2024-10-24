package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.settings.AccountBootstrappingWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrapRouter

@Composable
fun AccountBootstrappingSwitch(mainViewModel: MainViewModel) {
    Box {
        Children(mainViewModel.accountBootstrapRouterStack) {
            when (it.instance) {
                is AccountBootstrapRouter.Wrapper.ShowAccountBootstrap -> AccountBootstrappingWizard(it.instance as AccountBootstrapRouter.Wrapper.ShowAccountBootstrap)
                else -> Box {}
            }
        }
    }
}
