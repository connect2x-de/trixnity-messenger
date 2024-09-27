package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.settings.SettingsWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper

@Composable
fun SettingsWizardSwitch(mainViewModel: MainViewModel) {
    Children(mainViewModel.settingsWizardRouterStack) {
        when (val child = it.instance) {
            is Wrapper.WizardExplanation -> SettingsWizard(child)
            is Wrapper.None -> Box {}
            is Wrapper.NotificationSettings -> SettingsWizard(child)
            }
        }
    }
