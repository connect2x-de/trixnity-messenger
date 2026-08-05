package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.uia.UiaDummyStep
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepDummyRoute
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModel

internal class UiaStepDummyEntry : NavigationEntry<UiaStepDummyRoute> {

    override val metadata: Map<String, Any> = OverlayScene.overlay()

    @Composable
    override fun Content(route: UiaStepDummyRoute) {
        UiaDummyStep(uiaStepDummyViewModel = rememberComponent<UiaStepDummyViewModel>())
    }
}
