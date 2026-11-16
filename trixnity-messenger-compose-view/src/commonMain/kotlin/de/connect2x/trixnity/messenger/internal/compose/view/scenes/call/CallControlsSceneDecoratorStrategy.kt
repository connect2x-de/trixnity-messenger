package de.connect2x.trixnity.messenger.internal.compose.view.scenes.call

import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallSession
import de.connect2x.trixnity.messenger.compose.view.room.call.CallControlsView
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane.SinglePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScenePlaceholder
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.viewmodel.room.call.CallControlsViewModel
import kotlinx.coroutines.flow.StateFlow

@TrixnityMessengerCallApi
internal class CallControlsSceneDecoratorStrategy<T : Any>(
    private val controls: StateFlow<CallSession?>,
    private val navigation: GetRouteNavigation,
    private val view: CallControlsView,
    private val viewModel: CallControlsViewModel,
    private val emptyPlaceholder: TwoPaneScenePlaceholder?,
) : SceneDecoratorStrategy<T> {
    companion object {
        private val log =
            Logger(
                "de.connect2x.trixnity.messenger.internal.compose.view.scenes.call.CallControlsSceneDecoratorStrategy"
            )
    }

    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        return when (scene) {
            is SinglePaneScene,
            is TwoPaneScene,
            is ThreePaneScene -> CallControlsScene(scene, controls, navigation, view, viewModel, emptyPlaceholder)
            is OverlayScene -> scene
            else -> {
                log.warn { "Call controls are not supported for scene ${scene::class}" }
                scene
            }
        }
    }
}
