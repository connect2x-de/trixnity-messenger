package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.SSOLoginWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.SSOLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel

internal class SSOLoginEntry : NavigationEntry<SSOLoginRoute> {

    @Composable
    override fun Content(route: SSOLoginRoute) {
        SSOLoginWizardPanel(viewModel = rememberComponent<SSOLoginViewModel>())
    }
}
