package de.connect2x.trixnity.messenger.internal.compose.view.entries.verification

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.VerificationCompareEmojisOrNumbersWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponentForKey
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationCompareEmojisOrNumbersRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

internal class VerificationCompareEmojisOrNumbersEntry : NavigationEntry<VerificationCompareEmojisOrNumbersRoute> {
    override fun metadata(route: VerificationCompareEmojisOrNumbersRoute): Map<String, Any> {
        return VerificationEntry.sharingContext(route.userId).toMetadata()
    }

    @Composable
    override fun Content(route: VerificationCompareEmojisOrNumbersRoute) {
        VerificationCompareEmojisOrNumbersWizardPanel(
            rememberComponentForKey<VerificationViewModel>(VerificationEntry.Companion.VerificationComponentStoreKey)
        )
    }
}
