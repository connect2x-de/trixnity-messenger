package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.closeRoom
import de.connect2x.trixnity.messenger.internal.navigation.openHome
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceBrowser
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceListRouteMarker

@Composable
internal fun SpaceNavigationBar() {
    val navigation = DI.get<RouteNavigation>()
    val i18n = DI.get<I18nView>()
    val routes by navigation.routes.collectAsState()
    val spaceSelected = routes.any { it is SpaceListRouteMarker }
    val itemColors = NavigationBarItemDefaults.colors(selectedTextColor = MaterialTheme.colorScheme.onSurface)
    NavigationBar {
        NavigationBarItem(
            colors = itemColors,
            selected = !spaceSelected,
            onClick = {
                navigation.updateNavigation {
                    closeRoom()
                    openHome()
                }
            },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text(i18n.spacesNavigationHome()) },
        )
        NavigationBarItem(
            colors = itemColors,
            selected = spaceSelected,
            onClick = {
                navigation.updateNavigation {
                    closeRoom()
                    openSpaceBrowser()
                }
            },
            icon = { Icon(Icons.Default.Workspaces, null) },
            label = { Text(i18n.spacesTitle()) },
        )
    }
}
