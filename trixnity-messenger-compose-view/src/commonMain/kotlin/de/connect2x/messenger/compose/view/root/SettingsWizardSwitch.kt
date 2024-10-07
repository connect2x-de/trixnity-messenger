package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.settings.SettingsWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter

@Composable
fun SettingsWizardSwitch(mainViewModel: MainViewModel) {
    Box {
        Children(mainViewModel.settingsWizardRouterStack) {
            when (it.instance) {
                is SettingsWizardRouter.Wrapper.ShowWizard -> SettingsWizard((it.instance as SettingsWizardRouter.Wrapper.ShowWizard).steps)
                else -> Box{}
            }
        }
    }
}
