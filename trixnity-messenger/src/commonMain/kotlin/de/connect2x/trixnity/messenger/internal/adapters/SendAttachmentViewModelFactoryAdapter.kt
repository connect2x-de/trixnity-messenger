package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.SendAttachmentRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SendAttachmentViewModelFactoryAdapter(
    private val factory: SendAttachmentViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<SendAttachmentViewModel> {
    override fun create(parameters: ParametersHolder): SendAttachmentViewModel {
        val route = parameters.get<SendAttachmentRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("SendAttachment", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            file = route.fileDescriptor,
            selectedRoomId = route.roomId,
            onCloseAttachmentSendView = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
