package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import de.connect2x.trixnity.messenger.compose.view.navigation.LocalAppNavigationRailVisible
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedVerticalDivider
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceListRouteMarker

internal data class NavigationRailScene<T : Any>(
    val paneScene: Scene<T>,
    val rail: NavigationRailView,
    val selected: SpaceListRouteMarker?,
) : Scene<T> {
    companion object {
        fun selection(route: SpaceListRouteMarker) = metadata { put(SelectionKey, route) }
    }

    data object SelectionKey : NavMetadataKey<SpaceListRouteMarker>

    // Keep rail scroll and avatar subscriptions when panes change; NavigationView disables scene transitions.
    override val key: Any = Unit

    override val entries: List<NavEntry<T>> = paneScene.entries

    override val previousEntries: List<NavEntry<T>> = paneScene.previousEntries

    override val metadata: Map<String, Any> = paneScene.metadata

    override val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalAppNavigationRailVisible provides true) {
            Row(Modifier.fillMaxSize()) {
                rail.Content(selected)
                ThemedVerticalDivider(Modifier.fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    key(paneScene::class, paneScene.key) { paneScene.content() }
                }
            }
        }
    }
}
