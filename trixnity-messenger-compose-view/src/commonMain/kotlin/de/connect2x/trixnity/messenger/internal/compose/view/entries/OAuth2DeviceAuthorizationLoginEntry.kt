package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.OAuth2DeviceAuthorizationLoginWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2DeviceAuthorizationLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModel

internal class OAuth2DeviceAuthorizationLoginEntry : NavigationEntry<OAuth2DeviceAuthorizationLoginRoute> {

    @Composable
    override fun Content(route: OAuth2DeviceAuthorizationLoginRoute) {
        OAuth2DeviceAuthorizationLoginWizardPanel(
            viewModel = rememberComponent<OAuth2DeviceAuthorizationLoginViewModel>()
        )
    }
}
