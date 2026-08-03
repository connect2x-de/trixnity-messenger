package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.extras.TimelineElementDevInfoRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementDevInfoViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementDevInfoViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class TimelineElementDevInfoViewModelFactoryAdapter(
    private val factory: TimelineElementDevInfoViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<TimelineElementDevInfoViewModel> {
    override fun create(parameters: ParametersHolder): TimelineElementDevInfoViewModel {
        val route = parameters.get<TimelineElementDevInfoRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("TimelineElementDevInfo", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            eventId = route.eventId,
            roomId = route.roomId,
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
