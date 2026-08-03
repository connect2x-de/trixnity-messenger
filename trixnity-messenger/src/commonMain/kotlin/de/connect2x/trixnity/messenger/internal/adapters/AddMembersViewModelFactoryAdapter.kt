package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.extras.AddMembersRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class AddMembersViewModelFactoryAdapter(
    private val factory: AddMembersViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val potentialMembersViewModelFactory: PotentialMembersViewModelFactory,
) : ViewModelFactoryAdapter<AddMembersViewModel> {
    override fun create(parameters: ParametersHolder): AddMembersViewModel {
        val route = parameters.get<AddMembersRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("AddMembers", route.userId)
        val potentialMembersViewModelContext = viewModelContext.childContext("PotentialMembers")

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            potentialMembersViewModel =
                potentialMembersViewModelFactory.create(
                    viewModelContext = potentialMembersViewModelContext,
                    roomId = route.roomId,
                ),
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
