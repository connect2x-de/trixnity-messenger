package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ArchiveTextMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ArchiveTextMessageViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ArchiveMessageRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ArchiveMessageRouter.Config

private val log = KotlinLogging.logger {}

interface ArchiveMessageRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun showArchiveMessage(roomName: String)
    suspend fun closeArchiveMessage()

    sealed class Wrapper {
        data object None : Wrapper()
        class ArchiveMessageView(val viewModel: ArchiveTextMessageViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        data class ArchiveMessageConfiguration(val roomName: String) : Config()
    }

}

class ArchiveMessageRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onArchiveMessageDialogDismiss: () -> Unit,
) : ArchiveMessageRouter {

    private val archiveMessageNavigation = StackNavigation<Config>()

    override val stack = viewModelContext.childStack(
            source = archiveMessageNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "archiveMessageRouter",
            childFactory = ::createSettingsChild,
        )

    private fun createSettingsChild(
        archiveConfig: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (archiveConfig) {
            is Config.None -> Wrapper.None
            is Config.ArchiveMessageConfiguration -> Wrapper.ArchiveMessageView(
                viewModelContext.get<ArchiveTextMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    selectedRoomId = roomId,
                    roomName = archiveConfig.roomName,
                    onArchiveMessageDialogDismiss = onArchiveMessageDialogDismiss
                ),
            )
        }

    override suspend fun showArchiveMessage(roomName: String) {
        log.debug { "show ArchiveMessage Dialog $roomId" }
        archiveMessageNavigation.bringToFrontSuspending(Config.ArchiveMessageConfiguration(roomName = roomName))
    }

    override suspend fun closeArchiveMessage() {
        log.debug { "close ArchiveMessage Dialog" }
        archiveMessageNavigation.popWhileSuspending { it != Config.None }
    }

}
