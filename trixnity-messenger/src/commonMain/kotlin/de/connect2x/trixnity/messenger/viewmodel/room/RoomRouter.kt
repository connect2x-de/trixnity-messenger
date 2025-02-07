package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.settings.OpenAvatarCutterCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface RoomRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun openRoom(userId: UserId, roomId: RoomId)
    suspend fun closeRoom()
    fun isShown(): Boolean

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data class View(val userId: UserId, val roomId: String) : Config()
    }

    sealed class Wrapper {
        data class View(val viewModel: RoomViewModel) : Wrapper()
        data object None : Wrapper()
    }
}

class RoomRouterImpl(
    private val viewModelContext: ViewModelContext,
    private val onOpenRoom: (UserId, RoomId) -> Unit,
    private val onCloseRoom: () -> Unit,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenAvatarCutter: OpenAvatarCutterCallback,
) : RoomRouter {

    private val roomNavigation = StackNavigation<Config>()
    override val stack: Value<ChildStack<Config, Wrapper>> =
        viewModelContext.childStack(
            source = roomNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "RoomRouter",
            childFactory = ::createRoomChild,
        )

    private fun createRoomChild(
        roomConfig: Config,
        componentContext: ComponentContext,
    ): Wrapper =
        when (roomConfig) {
            is Config.None -> Wrapper.None
            is Config.View -> Wrapper.View(
                viewModelContext.get<RoomViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, roomConfig.userId),
                    selectedRoomId = RoomId(roomConfig.roomId),
                    onOpenRoom = onOpenRoom,
                    onCloseRoom = onCloseRoom,
                    onOpenMention = onOpenMention,
                    onOpenAvatarCutter = onOpenAvatarCutter,
                ).also {
                    log.debug { "::: created viewModel for ${roomConfig.userId}" }
                }
            )
        }

    override suspend fun openRoom(userId: UserId, roomId: RoomId) {
        log.debug { "show room: $roomId" }
        roomNavigation.bringToFrontSuspending(Config.View(userId, roomId.full))
    }

    override suspend fun closeRoom() {
        roomNavigation.popWhileSuspending { it !is Config.None }
    }

    override fun isShown(): Boolean =
        when (stack.value.active.configuration) {
            is Config.View -> true
            is Config.None -> false
        }
}
