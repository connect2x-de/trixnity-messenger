package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.export.ExportRoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.export.ExportRoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ExportRoomRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ExportRoomRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface ExportRoomRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun showExportRoom(roomName: String)
    suspend fun closeExportRoom()

    sealed class Wrapper {
        data object None : Wrapper()
        class ExportRoom(val viewModel: ExportRoomViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        data class ExportRoom(val roomName: String) : Config()
    }

}

class ExportRoomRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : ExportRoomRouter {

    private val navigation = StackNavigation<Config>()

    override val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "exportRoomRouter",
        childFactory = ::createSettingsChild,
    )

    private fun createSettingsChild(
        config: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None
            is Config.ExportRoom -> Wrapper.ExportRoom(
                viewModelContext.get<ExportRoomViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    roomId = roomId,
                    roomName = config.roomName,
                    onBack = onBack
                ),
            )
        }

    override suspend fun showExportRoom(roomName: String) {
        log.debug { "show ExportRoom $roomId" }
        navigation.bringToFrontSuspending(Config.ExportRoom(roomName = roomName))
    }

    override suspend fun closeExportRoom() {
        log.debug { "close ExportRoom" }
        navigation.popWhileSuspending { it != Config.None }
    }

}
