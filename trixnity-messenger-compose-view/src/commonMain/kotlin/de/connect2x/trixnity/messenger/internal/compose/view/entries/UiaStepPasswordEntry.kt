package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.uia.UiaPasswordInput
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepPasswordRoute
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModel

internal class UiaStepPasswordEntry : NavigationEntry<UiaStepPasswordRoute> {

    override val metadata: Map<String, Any> = OverlayScene.overlay()

    @Composable
    override fun Content(route: UiaStepPasswordRoute) {
        UiaPasswordInput(uiaStepPasswordViewModel = rememberComponent<UiaStepPasswordViewModel>())
    }
}
