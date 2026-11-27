package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.removeCall
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.parameter.ParametersHolder

@TrixnityMessengerCallApi
internal class RoomCallViewModelFactoryAdapter(
    private val factory: RoomCallViewModelFactory,
    private val navigation: RouteNavigation,
) : ViewModelFactoryAdapter<RoomCallViewModel> {
    override fun create(parameters: ParametersHolder): RoomCallViewModel {
        val route = parameters.get<RoomCallRoute>()
        val context = parameters.get<ViewModelContext>().childContext("RoomCall")
        val viewModel =
            factory.create(
                viewModelContext = context,
                target = route.target,
                onBack = navigation.navigationCallback { removeCall(route.target) },
                onShowChat = navigation.navigationCallback { openRoom(route.target.userId, route.target.roomId) },
            )
        context.registerBackCallback(
            BackCallback(
                enabled =
                    navigation.routes
                        .map { it.lastOrNull() == route }
                        .stateIn(
                            context.coroutineScope,
                            SharingStarted.Eagerly,
                            navigation.routes.value.lastOrNull() == route,
                        ),
                onBack = viewModel::back,
            )
        )
        return viewModel
    }
}
