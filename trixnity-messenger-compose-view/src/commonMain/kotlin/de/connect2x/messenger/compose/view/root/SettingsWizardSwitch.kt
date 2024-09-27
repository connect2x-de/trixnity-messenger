package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.settings.SettingsWizard
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel

@Composable
fun SettingsWizardSwitch(mainViewModel: MainViewModel) {
    Box {
        SettingsWizard(mainViewModel.settingsWizardRouterSteps)
    }
}
