package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel.Config.None
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel.Config.SendAttachmentView
import kotlinx.coroutines.launch
import org.koin.core.component.get

internal interface SendAttachmentRouter {
    val stack: Value<ChildStack<TimelineViewModel.Config, TimelineViewModel.Wrapper>>

    suspend fun closeAttachmentSendView()

    suspend fun showAttachmentSendView(file: FileDescriptor)
}

internal class SendAttachmentRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
) : SendAttachmentRouter {

    private val navigation = StackNavigation<TimelineViewModel.Config>()

    override val stack =
        viewModelContext.childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = None,
            handleBackButton = true,
            childFactory = ::createChild,
            key = "sendAttachmentRouter",
        )

    private fun createChild(
        config: TimelineViewModel.Config,
        componentContext: ComponentContext,
    ): TimelineViewModel.Wrapper =
        when (config) {
            is None -> TimelineViewModel.Wrapper.None
            is SendAttachmentView ->
                TimelineViewModel.Wrapper.View(
                    viewModelContext
                        .get<SendAttachmentViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("SendAttachmentView", componentContext),
                            file = config.file,
                            selectedRoomId = roomId,
                            onCloseAttachmentSendView = ::onCloseAttachmentSendView,
                        )
                )
        }

    private fun onCloseAttachmentSendView() {
        viewModelContext.coroutineScope.launch { closeAttachmentSendView() }
    }

    override suspend fun closeAttachmentSendView() {
        navigation.popWhileSuspending { it !is None }
    }

    override suspend fun showAttachmentSendView(file: FileDescriptor) {
        navigation.pushSuspending(SendAttachmentView(file))
    }
}
