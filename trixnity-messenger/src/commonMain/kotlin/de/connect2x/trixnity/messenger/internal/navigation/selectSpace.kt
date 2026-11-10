package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceListRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserItem

@TrixnityMessengerPrivateApi
fun RouteNavigationScope.openHome() {
    clearSpaceSelection()
}

@TrixnityMessengerPrivateApi
fun RouteNavigationScope.openSpaceBrowser() {
    clearSpaceSelection()
    push(SpaceBrowserRoute)
}

internal fun RouteNavigationScope.selectSpace(route: SpaceRoomListRoute, showExplore: Boolean) {
    clearSpaceSelection()
    push(route)
    if (showExplore) openSpaceExplore(route.userId, route.spaceId)
}

private fun RouteNavigationScope.clearSpaceSelection() {
    clear<SpaceExploreRoute>()
    clear<ExtrasRouteMarker>()
    items = items.filterNot { it is RoomListRouteMarker && it != RoomListRoute }
}

private fun RouteNavigationScope.openSpaceInHierarchy(route: SpaceRoomListRoute, showExplore: Boolean) {
    clear<SpaceExploreRoute>()
    clear<ExtrasRouteMarker>()
    when (val index = items.indexOf(route)) {
        -1 -> push(route)
        else -> {
            val history = items.take(index + 1)
            val details = items.drop(index + 1).filterNot { it is SpaceListRouteMarker }
            items = history + details
        }
    }
    if (showExplore) openSpaceExplore(route.userId, route.spaceId)
}

@TrixnityMessengerPrivateApi
fun RouteNavigationScope.openSpaceExplore(userId: UserId, spaceId: RoomId) {
    closeRoom()
    push(SpaceExploreRoute(userId, spaceId))
}

internal fun RouteNavigationScope.openSpaceItem(item: SpaceBrowserItem, showExplore: Boolean) {
    when {
        !item.joined -> return
        item.isSpace -> openSpaceInHierarchy(SpaceRoomListRoute(item.userId, item.roomId), showExplore)
        else -> openRoom(item.userId, item.roomId)
    }
}

internal fun RouteNavigationScope.selectSpaceItem(item: SpaceBrowserItem) {
    when {
        item.joined -> selectSpace(SpaceRoomListRoute(item.userId, item.roomId), showExplore = true)
        item.invited -> {
            clearSpaceSelection()
            openSpaceExplore(item.userId, item.roomId)
        }
    }
}

internal fun RouteNavigationScope.selectSpaceAccount(source: SpaceRoomListRoute, userId: UserId, showExplore: Boolean) {
    if (items.lastOrNull { it is RoomListRouteMarker } != source) return
    closeRoom()
    selectSpace(SpaceRoomListRoute(userId, source.spaceId), showExplore)
}

internal fun RouteNavigationScope.returnHomeForUnavailableSpace(source: SpaceRoomListRoute) {
    if (items.lastOrNull { it is RoomListRouteMarker } != source) return
    closeRoom()
    openHome()
}

internal fun RouteNavigationScope.removeSpace(route: SpaceRoomListRoute) {
    // Keep settings alive so an in-flight forget action can finish.
    items = items.filterNot { it == route || it == SpaceExploreRoute(route.userId, route.spaceId) }
}
