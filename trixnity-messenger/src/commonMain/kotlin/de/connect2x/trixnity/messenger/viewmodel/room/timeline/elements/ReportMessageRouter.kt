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

private val log = KotlinLogging.logger {}

interface ReportMessageRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun showReportMessage()
    suspend fun closeReportMessage()

    sealed class Wrapper {
        data object None : Wrapper()
        class ReportMessageView(val viewModel: ReportMessageViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data object ReportMessage : Config()
    }

}

class ReportMessageRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onShowReportMessageDialog: () -> Unit,
    private val onDismiss: () -> Unit,
) : ReportMessageRouter {

    private val settingsNavigation = StackNavigation<Config>()
    override val stack =
        viewModelContext.childStack(
            source = settingsNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "reportMessageRouter",
            childFactory = ::createSettingsChild,
        )

    private fun createSettingsChild(
        settingsConfig: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (settingsConfig) {
            is Config.None -> Wrapper.None
            is Config.ReportMessage -> Wrapper.ReportMessageView(
                viewModelContext.get<ReportToMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    selectedRoomId = roomId,
                    onShowReportMessageDialog = onShowReportMessageDialog,
                    onMessageReportFinished = onDismiss,
                ),
            )

        }

    override suspend fun showReportMessage() {
        log.debug { "show ReportMessage Dialog" }
        settingsNavigation.bringToFrontSuspending(Config.ReportMessage)
    }

    override suspend fun closeReportMessage() {
        log.debug { "close ReportMessage Dialog" }
        settingsNavigation.popWhileSuspending { it != Config.None }
    }

}
