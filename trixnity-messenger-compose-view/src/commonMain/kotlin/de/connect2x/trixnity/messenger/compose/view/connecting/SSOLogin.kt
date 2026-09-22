package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardSection
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel

interface SSOLoginView {
    @Composable fun create(ssoLoginViewModel: SSOLoginViewModel)
}

@Composable
fun SSOLogin(ssoLoginViewModel: SSOLoginViewModel) {
    DI.get<SSOLoginView>().create(ssoLoginViewModel)
}

class SSOLoginViewImpl : SSOLoginView {
    @Composable
    override fun create(ssoLoginViewModel: SSOLoginViewModel) {
        val state = ssoLoginViewModel.addMatrixAccountState.collectAsState().value
        val waitForRedirect = ssoLoginViewModel.waitForRedirect.collectAsState().value
        val i18n = DI.get<I18nView>()

        WizardSection(contentSpacing = MaterialTheme.messengerDpConstants.middle) {
            if (waitForRedirect || state is AddMatrixAccountState.None) {
                Text(i18n.externalLogin(ssoLoginViewModel.providerName ?: "SSO"))
            }

            when (state) {
                AddMatrixAccountState.None -> {}
                AddMatrixAccountState.Connecting ->
                    ThemedProgressIndicator(Modifier.fillMaxWidth(), MaterialTheme.components.linearProgressIndicator)

                is AddMatrixAccountState.Failure -> Text(state.message, color = MaterialTheme.colorScheme.error)

                AddMatrixAccountState.Success -> Text(i18n.commonSuccess())
            }
        }
    }
}
