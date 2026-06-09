package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel

interface OAuth2AuthorizationCodeLoginView {
    @Composable fun create(oAuth2AuthorizationCodeLoginViewModel: OAuth2AuthorizationCodeLoginViewModel)
}

@Composable
fun OAuth2AuthorizationCodeLogin(oAuth2AuthorizationCodeLoginViewModel: OAuth2AuthorizationCodeLoginViewModel) {
    DI.get<OAuth2AuthorizationCodeLoginView>().create(oAuth2AuthorizationCodeLoginViewModel)
}

class OAuth2AuthorizationCodeLoginViewImpl : OAuth2AuthorizationCodeLoginView {
    @Composable
    override fun create(oAuth2AuthorizationCodeLoginViewModel: OAuth2AuthorizationCodeLoginViewModel) {
        val state = oAuth2AuthorizationCodeLoginViewModel.state.collectAsState().value
        val type = oAuth2AuthorizationCodeLoginViewModel.type
        val i18n = DI.get<I18nView>()

        Column(Modifier.defaultMinSize(minHeight = 20.dp)) {
            when (state) {
                OAuth2AuthorizationCodeLoginViewModel.State.None -> {
                    val text =
                        when (type) {
                            OAuth2AuthorizationCodeLoginViewModel.Type.LOGIN ->
                                i18n.loginWithOAuth2Description(oAuth2AuthorizationCodeLoginViewModel.serverUrl)
                            OAuth2AuthorizationCodeLoginViewModel.Type.REGISTER ->
                                i18n.registerWithOAuth2Description(oAuth2AuthorizationCodeLoginViewModel.serverUrl)
                        }
                    Text(text)
                }

                OAuth2AuthorizationCodeLoginViewModel.State.StartLogin,
                OAuth2AuthorizationCodeLoginViewModel.State.WaitingForRedirect,
                OAuth2AuthorizationCodeLoginViewModel.State.ResumeLogin -> {
                    ThemedProgressIndicator(Modifier.fillMaxWidth(), MaterialTheme.components.linearProgressIndicator)
                    SmallSpacer()
                    Text(i18n.loginWithOAuth2Waiting(oAuth2AuthorizationCodeLoginViewModel.serverUrl))
                }

                is OAuth2AuthorizationCodeLoginViewModel.State.Failure ->
                    Text(state.message, color = MaterialTheme.colorScheme.error)

                OAuth2AuthorizationCodeLoginViewModel.State.Success -> {}
            }
        }
    }
}
