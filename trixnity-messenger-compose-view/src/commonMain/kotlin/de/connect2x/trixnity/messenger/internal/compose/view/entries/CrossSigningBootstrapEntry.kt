package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreContext
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreKey
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel

internal class CrossSigningBootstrapEntry : NavigationEntry<CrossSigningBootstrapRoute> {
    @Composable
    override fun Content(route: CrossSigningBootstrapRoute) {
        rememberComponent<CrossSigningBootstrapViewModel>()
    }

    companion object {
        data object CrossSigningBootstrapComponentStoreKey : ComponentStoreKey

        fun sharingContext(userId: UserId): ComponentStoreContext {
            return ComponentStoreNavEntryDecorator.context(
                CrossSigningBootstrapComponentStoreKey,
                CrossSigningBootstrapRoute(userId),
            )
        }
    }
}
