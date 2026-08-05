package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.uia.UiaRegistrationToken
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepRegistrationTokenRoute
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModel

internal class UiaStepRegistrationTokenEntry : NavigationEntry<UiaStepRegistrationTokenRoute> {

    override val metadata: Map<String, Any> = OverlayScene.overlay()

    @Composable
    override fun Content(route: UiaStepRegistrationTokenRoute) {
        UiaRegistrationToken(uiaStepRegistrationTokenViewModel = rememberComponent<UiaStepRegistrationTokenViewModel>())
    }
}
