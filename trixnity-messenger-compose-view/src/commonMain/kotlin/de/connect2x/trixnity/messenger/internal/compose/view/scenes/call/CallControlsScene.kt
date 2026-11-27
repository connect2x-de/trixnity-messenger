package de.connect2x.trixnity.messenger.internal.compose.view.scenes.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallSession
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.compose.view.room.call.CallControlsView
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane.SinglePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScenePlaceholder
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.viewmodel.room.call.CallControlsViewModel
import kotlinx.coroutines.flow.StateFlow

@TrixnityMessengerCallApi
internal data class CallControlsScene<T : Any>(
    val paneScene: Scene<T>,
    private val controls: StateFlow<CallSession?>,
    private val navigation: GetRouteNavigation,
    val view: CallControlsView,
    val viewModel: CallControlsViewModel,
    val emptyPlaceholder: TwoPaneScenePlaceholder?,
) : Scene<T> by paneScene {
    companion object {
        fun expandedCall(target: CallTarget): Map<String, Any> {
            return metadata { put(ExpandedCallTargetKey, target) }
        }

        internal fun showCompactControls(target: CallTarget?, foreground: Route?, entries: List<NavEntry<*>>): Boolean {
            if (target == null) return false
            // Navigation can already point at the call while the previous scene is still composed.
            // This results in the call controls quickly flashing when a call is started. Maybe there is a better way to
            // fix this.
            if (foreground is RoomCallRoute && foreground.target == target) return false
            return entries.none { it.metadata[ExpandedCallTargetKey] == target }
        }
    }

    private data object ExpandedCallTargetKey : NavMetadataKey<CallTarget>

    override val key: Any = CallControlsScene::class to paneScene.key

    // Nav3 caches scene calculation. Observe the registry in content so call changes remain visible.
    override val content: @Composable () -> Unit = {
        val session by controls.collectAsState()
        val routes by navigation.routes.collectAsState()
        val target = session?.target
        val compactControls: @Composable () -> Unit = {
            if (showCompactControls(target, routes.lastOrNull(), paneScene.entries)) {
                view.create(viewModel)
            }
        }
        when (paneScene) {
            is TwoPaneScene ->
                TwoPaneScene(
                        leftEntry = paneScene.entries.first().withCallControls(compactControls),
                        rightEntry = paneScene.entries.getOrNull(1),
                        previousEntries = paneScene.previousEntries,
                        emptyPlaceholder = emptyPlaceholder,
                    )
                    .content()
            is ThreePaneScene ->
                ThreePaneScene(
                        leftEntry = paneScene.entries[0].withCallControls(compactControls),
                        middleEntry = paneScene.entries[1],
                        rightEntry = paneScene.entries[2],
                        previousEntries = paneScene.previousEntries,
                    )
                    .content()
            is SinglePaneScene -> CallControlsPane(compactControls) { paneScene.content() }
            else -> paneScene.content()
        }
    }
}

@TrixnityMessengerCallApi
private fun <T : Any> NavEntry<T>.withCallControls(controls: @Composable () -> Unit): NavEntry<T> {
    return NavEntry(navEntry = this) { CallControlsPane(controls) { Content() } }
}

@Composable
@TrixnityMessengerCallApi
private fun CallControlsPane(controls: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        controls()
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}
