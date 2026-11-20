package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.logic.OpenMentionLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceSettingsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SpaceSettingsViewModelFactoryAdapter(
    private val factory: SpaceSettingsViewModelFactory,
    private val detailsFactory: RoomSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val openMentionLogic: OpenMentionLogic,
) : ViewModelFactoryAdapter<SpaceSettingsViewModel> {
    override fun create(parameters: ParametersHolder): SpaceSettingsViewModel {
        val route = parameters.get<SpaceSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("SpaceSettings", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            createDetails = { context ->
                detailsFactory.createWithNavigation(context, route.spaceId, routeNavigation, openMentionLogic)
            },
        )
    }
}
