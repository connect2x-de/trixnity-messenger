package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.launchBringToFront
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.MessageMetadata
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.None
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.AddMembers
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.ExportRoom
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
import kotlin.reflect.KClass


private val log = KotlinLogging.logger {}

interface ExtrasRouter {
    val stack: Value<ChildStack<Config, Wrapper>>

    suspend fun showRoomSettings(roomId: RoomId)
    suspend fun showMessageMetadata(eventId: EventId, roomId: RoomId)
    fun isExtrasRouterShown(): Boolean
    suspend fun closeExtrasRouter()

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
            data class MainSettings(val roomId: RoomId) : RoomSettings

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
    private val onSettingsBack: () -> Unit,
    private val onRoomBack: () -> Unit,
    private val onOpenAvatarCutter: AvatarCutterCallback,
) : ExtrasRouter {

    private val settingsNavigation = StackNavigation<Config>()
    override val stack = viewModelContext.childStack(
        source = settingsNavigation,
        serializer = Config.serializer(),
        initialConfiguration = None,
        key = "ExtrasRouter",
        childFactory = ::createSettingsChild,
    )

    override suspend fun showRoomSettings(roomId: RoomId) {
        log.debug { "show settings for room: $roomId" }
        val config = Config.RoomSettings.MainSettings(roomId)
        showRouterOrCallFallback(config) {
            settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, config)
        }
    }

    override suspend fun closeExtrasRouter() {
        log.debug { "close room settings" }
        // TODO: clear only settings items if Metadata is open?
        settingsNavigation.popWhileSuspending { it != None }
    }

    override suspend fun showMessageMetadata(eventId: EventId, roomId: RoomId) {
        log.debug { "show message metadata for event: $eventId in room: $roomId" }
        val config = MessageMetadata(eventId, roomId)
        showRouterOrCallFallback(config) {
            settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, config)
        }
    }

    private suspend fun showRouterOrCallFallback(baseConfig: Config, onRouterAlreadyShown: () -> Unit) {
        if (isExtrasRouterShown().not()) {
            settingsNavigation.bringToFrontSuspending(baseConfig)
        } else onRouterAlreadyShown()
    }

    override fun isExtrasRouterShown(): Boolean = stack.value.active.configuration !is None

    private fun createSettingsChild(
        config: Config,
        componentContext: ComponentContext,
    ): Wrapper = when (config) {
        is None -> Wrapper.None

        is Config.RoomSettings.MainSettings -> Wrapper.RoomSettings(
            viewModelContext.get<RoomSettingsViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                onBack = onRoomBack,
                selectedRoomId = config.roomId,
                onShowAddMembers = { showAddMembers(config.roomId) },
                onShowExportRoom = { showExportRoom(config.roomId) },
                onCloseRoomSettings = onSettingsBack,
                onOpenAvatarCutter = onOpenAvatarCutter,
            )
        )

        is AddMembers -> Wrapper.AddMember(
            viewModelContext.get<AddMembersViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                onBack = ::closeAddMembers,
                roomId = config.roomId,
                addMembersToRoomViewModel = viewModelContext.get<PotentialMembersViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        roomId = config.roomId
                    ),
            )
        )

        is ExportRoom -> Wrapper.ExportRoom(
            viewModelContext.get<ExportRoomViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                roomId = config.roomId,
                onBack = ::closeExportRoom,
            )
        )

        is MessageMetadata -> Wrapper.MessageMetadata(
            viewModelContext.get<MessageMetadataViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                eventId = config.eventId,
                roomId = config.roomId,
                onBack = ::closeMessageMetadata,
            )
        )
    }

    private fun showAddMembers(roomId: RoomId) {
        settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, AddMembers(roomId))
    }

    private fun closeAddMembers() {
        if (AddMembers::class.isActive())
            settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun showExportRoom(roomId: RoomId) {
        settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, ExportRoom(roomId))
    }

    private fun closeExportRoom() {
        if (ExportRoom::class.isActive())
            settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun closeMessageMetadata() {
        if (MessageMetadata::class.isActive())
            settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun KClass<out Config>.isActive(): Boolean =
        stack.value.active.configuration::class == this
}
