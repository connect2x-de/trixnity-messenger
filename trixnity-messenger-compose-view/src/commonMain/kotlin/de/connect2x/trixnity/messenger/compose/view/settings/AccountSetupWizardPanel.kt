package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.wizard.NextButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardPanel
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel

@Composable
fun AccountSetupSettingsWizardPanel(viewModel: AccountSetupViewModel) {
    val i18n = DI.get<I18nView>()
    WizardPanel(
        title = { i18n.commonWelcome() },
        subTitle = { "${i18n.commonAccount()}: ${viewModel.initialSettingsSetupViewModel.account.full}" },
        content = { AccountSetupWizardStep(viewModel) },
        buttons = {
            NextButton {
                viewModel.startVerification()
                viewModel.closeAccountSetup()
            }
        },
        wizardId = "AccountSetup",
    )
}
