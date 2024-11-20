package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.settings.AccountSetupWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter

@Composable
fun AccountSetupSwitch(mainViewModel: MainViewModel) {
    Box {
        Children(mainViewModel.accountSetupRouterStack) {
            when (it.instance) {
                is AccountSetupRouter.Wrapper.ShowAccountSetup -> AccountSetupWizard(it.instance as AccountSetupRouter.Wrapper.ShowAccountSetup)
                else -> Box {}
            }
        }
    }
}
