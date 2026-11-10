package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.PasswordLoginWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.root.PasswordLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel

internal class PasswordLoginEntry : NavigationEntry<PasswordLoginRoute> {
    override val metadata: Map<String, Any> = NavigationVisibility.Hidden.metadata()

    @Composable
    override fun Content(route: PasswordLoginRoute) {
        PasswordLoginWizardPanel(viewModel = rememberComponent<PasswordLoginViewModel>())
    }
}
