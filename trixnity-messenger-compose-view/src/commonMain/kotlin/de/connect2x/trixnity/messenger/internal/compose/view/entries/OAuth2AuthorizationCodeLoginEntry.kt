package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.OAuth2AuthorizationCodeLoginWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2AuthorizationCodeLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel

internal class OAuth2AuthorizationCodeLoginEntry : NavigationEntry<OAuth2AuthorizationCodeLoginRoute> {
    override val metadata: Map<String, Any> = NavigationVisibility.Hidden.metadata()

    @Composable
    override fun Content(route: OAuth2AuthorizationCodeLoginRoute) {
        OAuth2AuthorizationCodeLoginWizardPanel(viewModel = rememberComponent<OAuth2AuthorizationCodeLoginViewModel>())
    }
}
