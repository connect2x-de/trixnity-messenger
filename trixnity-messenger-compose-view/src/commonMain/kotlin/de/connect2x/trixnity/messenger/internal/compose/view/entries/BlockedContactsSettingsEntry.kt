package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.settings.BlockedContactsSettings
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.roomlist.BlockedContactsSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModel

internal class BlockedContactsSettingsEntry : NavigationEntry<BlockedContactsSettingsRoute> {

    @Composable
    override fun Content(route: BlockedContactsSettingsRoute) {
        BlockedContactsSettings(
            blockedContactsSettingsViewModel = rememberComponent<BlockedContactsSettingsViewModel>()
        )
    }
}
