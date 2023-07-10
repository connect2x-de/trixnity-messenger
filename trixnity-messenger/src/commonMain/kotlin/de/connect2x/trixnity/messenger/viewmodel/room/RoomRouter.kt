package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.RoomConfig
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.RoomWrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface RoomRouter {
    val roomStack: Value<ChildStack<RoomConfig, RoomWrapper>>
    suspend fun closeRoom()
    suspend fun showRoom(accountName: String, roomId: RoomId)
    fun isShown(): Boolean

    sealed class RoomConfig : Parcelable {
        @Parcelize
        object None : RoomConfig()

        @Parcelize
        data class View(val accountName: String, val roomId: String) : RoomConfig() // String to make it parcelizable
    }

    sealed class RoomWrapper {
        data class View(val roomViewModel: RoomViewModel) : RoomWrapper()
        object None : RoomWrapper()
    }
}

class RoomRouterImpl(
    private val viewModelContext: ViewModelContext,
    private val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onCloseRoom: () -> Unit,
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, accountName: String) -> Unit,
) : RoomRouter {

    private val roomNavigation = StackNavigation<RoomConfig>()
    override val roomStack: Value<ChildStack<RoomConfig, RoomWrapper>> =
        viewModelContext.childStack(
            source = roomNavigation,
            initialConfiguration = RoomConfig.None,
            key = "RoomRouter",
            childFactory = ::createRoomChild,
        )

    private fun createRoomChild(
        roomConfig: RoomConfig,
        componentContext: ComponentContext
    ): RoomWrapper =
        when (roomConfig) {
            is RoomConfig.None -> RoomWrapper.None
            is RoomConfig.View -> RoomWrapper.View(
                viewModelContext.get<RoomViewModelFactory>().newRoomViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext, roomConfig.accountName),
                    selectedRoomId = RoomId(roomConfig.roomId),
                    isBackButtonVisible = isBackButtonVisible,
                    onRoomBack = onCloseRoom,
                    onOpenModal = onOpenModal,
                ).also {
                    log.debug { "::: created viewModel for ${roomConfig.accountName}" }
                }
            )
        }

    override suspend fun showRoom(accountName: String, roomId: RoomId) {
        log.debug { "show room: $roomId" }
        roomNavigation.bringToFrontSuspending(RoomConfig.View(accountName, roomId.full))
    }

    override suspend fun closeRoom() {
        roomNavigation.popWhileSuspending { it !is RoomConfig.None }
    }

    override fun isShown(): Boolean =
        when (roomStack.value.active.configuration) {
            is RoomConfig.View -> true
            is RoomConfig.None -> false
        }
}