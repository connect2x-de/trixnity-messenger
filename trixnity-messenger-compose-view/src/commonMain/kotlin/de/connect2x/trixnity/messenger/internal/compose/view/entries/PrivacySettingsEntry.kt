package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.settings.PrivacySettings
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.roomlist.PrivacySettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModel

internal class PrivacySettingsEntry : NavigationEntry<PrivacySettingsRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.left() + ThreePaneScene.left()

    @Composable
    override fun Content(route: PrivacySettingsRoute) {
        PrivacySettings(privacySettingsViewModel = rememberComponent<PrivacySettingsAllAccountsViewModel>())
    }
}
