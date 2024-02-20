package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportToMessageViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter.Config
import net.folivo.trixnity.core.model.EventId

private val log = KotlinLogging.logger {}

interface ReportMessageRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun showReportMessage(eventId: EventId)
    suspend fun closeReportMessage()

    sealed class Wrapper {
        data object None : Wrapper()
        class ReportMessageView(
            val viewModel: ReportMessageViewModel
        ) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data class ReportMessage(val eventId: EventId) : Config()
    }

}

class ReportMessageRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onShowReportMessageDialog: (EventId) -> Unit,
    private val onReportMessageDialogDismiss: (EventId) -> Unit,
) : ReportMessageRouter {

    private val reportMessageNavigation = StackNavigation<Config>()
    override val stack =
        viewModelContext.childStack(
            source = reportMessageNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "reportMessageRouter",
            childFactory = ::createSettingsChild,
        )

    private fun createSettingsChild(
        reportConfig: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (reportConfig) {
            is Config.None -> Wrapper.None
            is Config.ReportMessage -> Wrapper.ReportMessageView(
                viewModelContext.get<ReportToMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    eventId = reportConfig.eventId,
                    selectedRoomId = roomId,
                    onShowReportMessageDialog = onShowReportMessageDialog,
                    onMessageReportFinished = onReportMessageDialogDismiss,
                ),
            )

        }

    override suspend fun showReportMessage(eventId: EventId) {
        log.debug { "show ReportMessage Dialog $eventId" }
        reportMessageNavigation.bringToFrontSuspending(Config.ReportMessage(eventId))
    }

    override suspend fun closeReportMessage() {
        log.debug { "close ReportMessage Dialog" }
        reportMessageNavigation.popWhileSuspending { it != Config.None }
    }

}
