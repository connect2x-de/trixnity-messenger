package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.ReportMessageRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportToMessageViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class ReportMessageViewModelFactoryAdapter(
    private val factory: ReportToMessageViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<ReportMessageViewModel> {
    override fun create(parameters: ParametersHolder): ReportMessageViewModel {
        val route = parameters.get<ReportMessageRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("ReportMessage", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            eventId = route.eventId,
            onShowReportMessageDialog =
                routeNavigation.navigationCallback { roomId, eventId ->
                    replace(ReportMessageRoute(route.userId, roomId, eventId))
                },
            onMessageReportFinished = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
