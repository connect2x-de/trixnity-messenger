package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.navigation.AppNavigationRail
import de.connect2x.trixnity.messenger.compose.view.navigation.AppNavigationRailItem
import de.connect2x.trixnity.messenger.compose.view.space.spaceNavigationItems
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openHome
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceListRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceNavigationRailViewModel

@TrixnityMessengerPrivateApi
interface NavigationRailView {
    @Composable fun Content(selected: SpaceListRouteMarker?)
}

@Composable
internal fun rememberNavigationRailView(
    componentFactory: CloseableComponentFactory,
    navigation: RouteNavigation,
): NavigationRailView {
    val component =
        remember(componentFactory) { componentFactory.create(SpaceNavigationRailViewModel::class, emptySet()) }
    DisposableEffect(component) { onDispose { component.close() } }
    return remember(component, navigation) { NavigationRailViewImpl(component.value, navigation) }
}

private class NavigationRailViewImpl(
    private val viewModel: SpaceNavigationRailViewModel,
    private val navigation: RouteNavigation,
) : NavigationRailView {
    @Composable
    override fun Content(selected: SpaceListRouteMarker?) {
        val i18n = DI.get<I18nView>()
        val state by viewModel.state.collectAsState()
        AppNavigationRail(
            header = {
                AppNavigationRailItem(
                    label = i18n.spacesAllRooms(),
                    selected = selected == null,
                    onClick = navigation.navigationCallback { openHome() },
                ) {
                    Icon(Icons.Default.Home, null)
                }
                ThemedHorizontalDivider(Modifier.width(40.dp))
            }
        ) {
            spaceNavigationItems(
                viewModel = viewModel,
                state = state,
                selectedSpace = (selected as? SpaceRoomListRoute)?.let { it.userId to it.spaceId },
                i18n = i18n,
            )
        }
    }
}
