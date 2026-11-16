package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallSignallingFactory
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModelImpl
import org.koin.core.parameter.ParametersHolder

@TrixnityMessengerCallApi
internal class RoomCallViewModelFactoryAdapter(
    private val routeNavigation: RouteNavigation,
    private val signallingFactory: CallSignallingFactory,
) : ViewModelFactoryAdapter<RoomCallViewModel> {
    override fun create(parameters: ParametersHolder): RoomCallViewModel {
        val route = parameters.get<RoomCallRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RoomCall", route.userId)
        return RoomCallViewModelImpl(
            viewModelContext = viewModelContext,
            onBack = routeNavigation.navigationCallback { pop(route) },
            signalling =
                signallingFactory.create(
                    coroutineScope = viewModelContext.coroutineScope,
                    matrixClient = viewModelContext.matrixClient,
                    roomId = route.roomId,
                    onEnded = routeNavigation.navigationCallback { pop(route) },
                ),
        )
    }
}
