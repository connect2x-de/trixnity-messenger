package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.settings.OpenAvatarCutterCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import org.koin.core.component.get

interface RoomRouter {
    val stack: Value<ChildStack<Config, Wrapper>>

    suspend fun openRoom(userId: UserId, roomId: RoomId, via: Set<String>? = null)

    suspend fun closeRoom()

    fun isShown(): Boolean

    @Serializable
    sealed class Config {
        @Serializable data object None : Config()

        @Serializable data class View(val userId: UserId, val roomId: String) : Config()

        @Serializable
        data class JoinRoomAction(val userId: UserId, val roomId: String, val via: Set<String>?) : Config()
    }

    sealed class Wrapper {
        data class View(val viewModel: RoomViewModel) : Wrapper()

        data class JoinRoomAction(val viewModel: JoinRoomActionViewModel) : Wrapper()

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
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.room.RoomRouterImpl")
    }

    private val roomNavigation = StackNavigation<Config>()
    override val stack: Value<ChildStack<Config, Wrapper>> =
        viewModelContext.childStack(
            source = roomNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "RoomRouter",
            childFactory = ::createRoomChild,
        )

    private fun createRoomChild(roomConfig: Config, componentContext: ComponentContext): Wrapper =
        when (roomConfig) {
            is Config.None -> Wrapper.None
            is Config.View ->
                Wrapper.View(
                    viewModelContext
                        .get<RoomViewModelFactory>()
                        .create(
                            viewModelContext =
                                viewModelContext.childContext("Room", componentContext, roomConfig.userId),
                            selectedRoomId = RoomId(roomConfig.roomId),
                            onOpenRoom = onOpenRoom,
                            onCloseRoom = onCloseRoom,
                            onOpenMention = onOpenMention,
                            onOpenAvatarCutter = onOpenAvatarCutter,
                        )
                        .also { log.debug { "::: created viewModel for ${roomConfig.userId}" } }
                )

            is Config.JoinRoomAction ->
                Wrapper.JoinRoomAction(
                    viewModelContext
                        .get<JoinRoomActionViewModelFactory>()
                        .create(
                            viewModelContext.childContext("RoomJoinAction", componentContext, roomConfig.userId),
                            roomId = RoomId(roomConfig.roomId),
                            via = roomConfig.via,
                            onOpenRoom = { onOpenRoom(roomConfig.userId, it) },
                            onDismiss = onCloseRoom,
                        )
                        .also {
                            log.debug {
                                "::: created viewModel for ${roomConfig.userId} room join confirm (room ${roomConfig.roomId}"
                            }
                        }
                )
        }

    override suspend fun openRoom(userId: UserId, roomId: RoomId, via: Set<String>?) {
        val matrixClient = viewModelContext.getMatrixClient(userId)
        val memberState = matrixClient.room.getById(roomId).firstOrNull()?.membership
        when {
            memberState == Membership.JOIN || memberState == Membership.LEAVE || memberState == Membership.BAN -> {
                log.debug { "show room: $roomId" }
                roomNavigation.bringToFrontSuspending(Config.View(userId, roomId.full))
            }

            // TODO show preview of timeline when room is world_readable

            else -> {
                roomNavigation.bringToFrontSuspending(Config.JoinRoomAction(userId, roomId.full, via))
            }
        }
    }

    override suspend fun closeRoom() {
        roomNavigation.popWhileSuspending { it !is Config.None }
    }

    override fun isShown(): Boolean =
        when (stack.value.active.configuration) {
            is Config.View -> true
            is Config.None -> false
            is Config.JoinRoomAction -> true
        }
}
