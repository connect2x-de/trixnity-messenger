package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2LoginViewModel

interface OAuth2LoginView {
    @Composable
    fun create(oAuth2LoginViewModel: OAuth2LoginViewModel)
}

@Composable
fun OAuth2Login(oAuth2LoginViewModel: OAuth2LoginViewModel) {
    DI.get<OAuth2LoginView>().create(oAuth2LoginViewModel)
}

class OAuth2LoginViewImpl : OAuth2LoginView {
    @Composable
    override fun create(oAuth2LoginViewModel: OAuth2LoginViewModel) {
        val state = oAuth2LoginViewModel.state.collectAsState().value
        val type = oAuth2LoginViewModel.type
        val i18n = DI.get<I18nView>()

        Box(Modifier.defaultMinSize(minHeight = 20.dp)) {
            when (state) {
                OAuth2LoginViewModel.State.None -> {
                    val text = when (type) {
                        OAuth2LoginViewModel.Type.LOGIN -> i18n.loginWithOAuth2Description(oAuth2LoginViewModel.serverUrl)
                        OAuth2LoginViewModel.Type.REGISTER -> i18n.registerWithOAuth2Description(oAuth2LoginViewModel.serverUrl)
                    }
                    Text(text)
                }

                OAuth2LoginViewModel.State.StartLogin,
                OAuth2LoginViewModel.State.WaitingForRedirect,
                OAuth2LoginViewModel.State.ResumeLogin -> {
                    Text(i18n.externalLogin("OAuth2"))
                    SmallSpacer()
                    ThemedProgressIndicator(
                        Modifier.fillMaxWidth(),
                        MaterialTheme.components.linearProgressIndicator
                    )
                }

                is OAuth2LoginViewModel.State.Failure ->
                    Text(state.message, color = MaterialTheme.colorScheme.error)

                OAuth2LoginViewModel.State.Success -> {}
            }
        }
    }
}
