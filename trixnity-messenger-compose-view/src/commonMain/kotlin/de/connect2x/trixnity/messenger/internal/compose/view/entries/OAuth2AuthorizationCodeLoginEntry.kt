package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.ConnectingWizard
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2AuthorizationCodeLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel

internal class OAuth2AuthorizationCodeLoginEntry : NavigationEntry<OAuth2AuthorizationCodeLoginRoute> {

    @Composable
    override fun Content(route: OAuth2AuthorizationCodeLoginRoute) {
        ConnectingWizard(viewModel = rememberComponent<OAuth2AuthorizationCodeLoginViewModel>())
    }
}
