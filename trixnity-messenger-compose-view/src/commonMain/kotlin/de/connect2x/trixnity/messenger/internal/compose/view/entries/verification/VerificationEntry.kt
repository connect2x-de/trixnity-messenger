package de.connect2x.trixnity.messenger.internal.compose.view.entries.verification

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreContext
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreKey
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

internal class VerificationEntry : NavigationEntry<VerificationRoute> {

    override fun metadata(route: VerificationRoute): Map<String, Any> {
        return sharingContext(route.userId).toMetadata() + NavigationVisibility.Hidden.metadata()
    }

    @Composable
    override fun Content(route: VerificationRoute) {
        rememberComponent<VerificationViewModel>()
    }

    companion object {
        data object VerificationComponentStoreKey : ComponentStoreKey

        fun sharingContext(userId: UserId): ComponentStoreContext {
            return ComponentStoreNavEntryDecorator.context(VerificationComponentStoreKey, VerificationRoute(userId))
        }
    }
}
