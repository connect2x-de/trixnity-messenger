package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.MessageMetadata
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.None
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.AddMembers
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.ExportRoom
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface ExtrasRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    fun isShown(): Boolean
    suspend fun back()
    suspend fun closeExtrasRouter()
    suspend fun openRoomSettings(roomId: RoomId)
    suspend fun openAddMembers(roomId: RoomId)
    suspend fun openExportRoom(roomId: RoomId)
    suspend fun openMessageMetadata(eventId: EventId, roomId: RoomId)

    sealed class Wrapper {
        data object None : Wrapper()
        class RoomSettings(val viewModel: RoomSettingsViewModel) : Wrapper()
        class AddMember(val viewModel: AddMembersViewModel) : Wrapper()
        class ExportRoom(val viewModel: ExportRoomViewModel) : Wrapper()
        class MessageMetadata(val viewModel: MessageMetadataViewModel) : Wrapper()
    }

    @Serializable
    sealed interface Config {

        @Serializable
        sealed interface RoomSettings : Config {

            @Serializable
            data class Main(val roomId: RoomId) : RoomSettings

            @Serializable
            data class AddMembers(val roomId: RoomId) : RoomSettings

            @Serializable
            data class ExportRoom(val roomId: RoomId) : RoomSettings
        }

        @Serializable
        data class MessageMetadata(val eventId: EventId, val roomId: RoomId) : Config

        @Serializable
        data object None : Config
    }
}

class ExtrasRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val onCloseRoom: () -> Unit,
    private val onOpenAvatarCutter: OpenAvatarCutterCallback,
) : ExtrasRouter {

    private val extrasNavigation = StackNavigation<Config>()
    override val stack = viewModelContext.childStack(
        source = extrasNavigation,
        serializer = Config.serializer(),
        initialConfiguration = None,
        key = "ExtrasRouter",
        childFactory = ::createSettingsChild,
    )

    override fun isShown(): Boolean =
        stack.value.active.configuration !is None

    override suspend fun back() {
        val config = stack.value.active.configuration
        extrasNavigation.popSuspending { log.debug { "closed $config ${it.toSuccessString()}" } }
    }

    override suspend fun closeExtrasRouter() {
        val config = None
        extrasNavigation.replaceAllSuspending(config)
    }

    override suspend fun openRoomSettings(roomId: RoomId) {
        val config = RoomSettings.Main(roomId)
        extrasNavigation.replaceAllSuspending(config) {
            log.debug { "opened room settings for room: $roomId" }
        }
    }

    override suspend fun openAddMembers(roomId: RoomId) {
        if (stack.value.items.find { it.configuration is RoomSettings.Main } == null) {
            openRoomSettings(roomId)
        }
        val config = AddMembers(roomId)
        if (stack.value.active.configuration is AddMembers) {
            extrasNavigation.replaceCurrentSuspending(config)
        } else extrasNavigation.bringToFrontSuspending(config)
    }

    override suspend fun openExportRoom(roomId: RoomId) {
        if (stack.value.items.find { it.configuration is RoomSettings.Main } == null) {
            openRoomSettings(roomId)
        }
        val config = ExportRoom(roomId)
        if (stack.value.active.configuration is ExportRoom) {
            extrasNavigation.replaceCurrentSuspending(config)
        } else extrasNavigation.bringToFrontSuspending(config)
    }

    override suspend fun openMessageMetadata(eventId: EventId, roomId: RoomId) {
        val config = MessageMetadata(eventId, roomId)
        extrasNavigation.bringToFrontSuspending(config)
    }

    private fun createSettingsChild(
        config: Config,
        componentContext: ComponentContext,
    ): Wrapper = when (config) {
        is None -> Wrapper.None

        is RoomSettings.Main -> Wrapper.RoomSettings(
            viewModelContext.get<RoomSettingsViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                onBack = onCloseRoom, // TODO ::onBack?
                selectedRoomId = config.roomId,
                onOpenAddMembers = { onOpenAddMembers(config.roomId) },
                onOpenExportRoom = { onOpenExportRoom(config.roomId) },
                onCloseRoomSettings = ::onCloseRoomSettings,
                onOpenAvatarCutter = onOpenAvatarCutter,
            )
        )

        is AddMembers -> Wrapper.AddMember(
            viewModelContext.get<AddMembersViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                onBack = ::onBack,
                roomId = config.roomId,
                addMembersToRoomViewModel = viewModelContext.get<PotentialMembersViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        roomId = config.roomId,
                    ),
            )
        )

        is ExportRoom -> Wrapper.ExportRoom(
            viewModelContext.get<ExportRoomViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                roomId = config.roomId,
                onBack = ::onBack,
            )
        )

        is MessageMetadata -> Wrapper.MessageMetadata(
            viewModelContext.get<MessageMetadataViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                eventId = config.eventId,
                roomId = config.roomId,
                onBack = ::onBack,
            )
        )
    }

    private fun onBack() = viewModelContext.coroutineScope.launch {
        back()
    }

    private fun onOpenAddMembers(roomId: RoomId) = viewModelContext.coroutineScope.launch {
        openAddMembers(roomId)
    }

    private fun onOpenExportRoom(roomId: RoomId) = viewModelContext.coroutineScope.launch {
        openExportRoom(roomId)
    }

    private fun onCloseRoomSettings() = viewModelContext.coroutineScope.launch {
        closeExtrasRouter()
    }

    private fun Boolean.toSuccessString() =
        if (this) "successfully" else "failed"
}
