package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.settings.AvatarCutter
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.routes.AvatarCutterRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModel

internal class AvatarCutterEntry : NavigationEntry<AvatarCutterRoute> {

    override val metadata: Map<String, Any> = OverlayScene.overlay()

    @Composable
    override fun Content(route: AvatarCutterRoute) {
        AvatarCutter(avatarCutterViewModel = rememberComponent<AvatarCutterViewModel>())
    }
}
