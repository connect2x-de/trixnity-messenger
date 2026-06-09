package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.LargeSpacer
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModel

interface OAuth2DeviceAuthorizationLoginView {
    @Composable fun create(viewModel: OAuth2DeviceAuthorizationLoginViewModel)
}

@Composable
fun OAuth2DeviceAuthorizationLogin(viewModel: OAuth2DeviceAuthorizationLoginViewModel) {
    DI.get<OAuth2DeviceAuthorizationLoginView>().create(viewModel)
}

class OAuth2DeviceAuthorizationLoginViewImpl : OAuth2DeviceAuthorizationLoginView {
    @Composable
    override fun create(viewModel: OAuth2DeviceAuthorizationLoginViewModel) {
        val state = viewModel.state.collectAsState().value
        val i18n = DI.get<I18nView>()

        Column(Modifier.defaultMinSize(minHeight = 20.dp)) {
            Text(i18n.loginWithOAuth2DeviceDescription(viewModel.serverUrl))
            LargeSpacer()
            when (state) {
                OAuth2DeviceAuthorizationLoginViewModel.State.ObtainCode -> {
                    ThemedProgressIndicator(Modifier.fillMaxWidth(), MaterialTheme.components.linearProgressIndicator)
                    SmallSpacer()
                    Text(i18n.loginWithOAuth2Waiting(viewModel.serverUrl))
                }
                is OAuth2DeviceAuthorizationLoginViewModel.State.CheckCode -> {
                    Card(Modifier.align(Alignment.CenterHorizontally)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = state.userCode,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                            )
                        }
                    }

                    SmallSpacer()

                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = state.uri,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }

                is OAuth2DeviceAuthorizationLoginViewModel.State.Failure ->
                    Text(state.message, color = MaterialTheme.colorScheme.error)

                OAuth2DeviceAuthorizationLoginViewModel.State.None,
                OAuth2DeviceAuthorizationLoginViewModel.State.Success -> {}
            }
        }
    }
}
