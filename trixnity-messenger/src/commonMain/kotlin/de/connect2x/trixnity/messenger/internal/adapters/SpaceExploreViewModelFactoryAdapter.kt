package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceItem
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceExploreViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceExploreViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SpaceExploreViewModelFactoryAdapter(
    private val factory: SpaceExploreViewModelFactory,
    private val navigation: RouteNavigation,
) : ViewModelFactoryAdapter<SpaceExploreViewModel> {
    override fun create(parameters: ParametersHolder): SpaceExploreViewModel {
        val route = parameters.get<SpaceExploreRoute>()
        val context = parameters.get<ViewModelContext>().childContext("SpaceExplore")
        return factory.create(
            viewModelContext = context,
            userId = route.userId,
            spaceId = route.spaceId,
            onSelect = navigation.navigationCallback { item -> openSpaceItem(item, showExplore = true) },
            onBack = navigation.navigationCallback { pop(route) },
            onOpenSettings =
                navigation.navigationCallback {
                    clear<ExtrasRouteMarker>()
                    push(RoomSettingsRoute(route.userId, route.spaceId))
                },
        )
    }
}
