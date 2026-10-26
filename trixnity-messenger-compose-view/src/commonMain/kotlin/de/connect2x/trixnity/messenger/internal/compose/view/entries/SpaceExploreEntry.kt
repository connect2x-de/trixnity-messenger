package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import de.connect2x.trixnity.messenger.compose.view.common.LocalAccountColorsEnabled
import de.connect2x.trixnity.messenger.compose.view.space.SpaceExploreView
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail.NavigationRailScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceExploreViewModel

internal class SpaceExploreEntry : NavigationEntry<SpaceExploreRoute> {
    override val metadata: Map<String, Any> = TwoPaneScene.right() + NavigationVisibility.Always.metadata()

    override fun metadata(route: SpaceExploreRoute): Map<String, Any> =
        metadata + NavigationRailScene.selection(SpaceRoomListRoute(route.userId, route.spaceId))

    @Composable
    override fun Content(route: SpaceExploreRoute) {
        CompositionLocalProvider(LocalAccountColorsEnabled provides false) {
            SpaceExploreView(rememberComponent<SpaceExploreViewModel>())
        }
    }
}
