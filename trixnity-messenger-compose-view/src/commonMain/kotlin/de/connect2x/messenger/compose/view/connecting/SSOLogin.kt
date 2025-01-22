package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel

interface SSOLoginView {
    @Composable
    fun create(ssoLoginViewModel: SSOLoginViewModel)
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

        if (waitForRedirect) {
            Text(i18n.externalLogin(ssoLoginViewModel.providerName ?: "SSO"))
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.defaultMinSize(minHeight = 20.dp)) {
            when (state) {
                AddMatrixAccountState.None -> {}
                AddMatrixAccountState.Connecting -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is AddMatrixAccountState.Failure ->
                    Text(state.message, color = MaterialTheme.colorScheme.error)

                AddMatrixAccountState.Success -> {}
            }
        }
    }
}
