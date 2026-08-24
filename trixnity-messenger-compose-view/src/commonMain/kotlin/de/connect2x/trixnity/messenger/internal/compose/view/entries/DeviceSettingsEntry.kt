package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.settings.DeviceSettings
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.roomlist.DeviceSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsAllAccountsViewModel

internal class DeviceSettingsEntry : NavigationEntry<DeviceSettingsRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.left() + ThreePaneScene.left()

    @Composable
    override fun Content(route: DeviceSettingsRoute) {
        DeviceSettings(deviceSettingsViewModel = rememberComponent<DeviceSettingsAllAccountsViewModel>())
    }
}
