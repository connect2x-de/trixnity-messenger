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
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Config.MessageMetadata
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Config.None
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Config.RoomSettings.AddMembers
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Config.RoomSettings.ExportRoom
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get
import kotlin.reflect.KClass


private val log = KotlinLogging.logger {}

interface SettingsRouter { // TODO: rename to RoomInfoRouter, RoomControlsRouter, RoomSidePanelRouter, DetailsRouter, ExtrasRouter
    val stack: Value<ChildStack<Config, Wrapper>>

    suspend fun showSettings(roomId: RoomId)
    suspend fun closeSettings()
    suspend fun showMessageMetadata(eventId: EventId, roomId: RoomId)
    fun isSettingsShown(): Boolean // TODO rename to isPanelShown, isPaneShown, isRouterVisible?

    sealed class Wrapper {
        data object None : Wrapper()
        class View(val viewModel: RoomSettingsViewModel) : Wrapper()
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

class SettingsRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val onSettingsBack: () -> Unit,
    private val onRoomBack: () -> Unit,
    private val onOpenAvatarCutter: AvatarCutterCallback,
) : SettingsRouter {

    private val settingsNavigation = StackNavigation<Config>()
    override val stack =
        viewModelContext.childStack(
            source = settingsNavigation,
            serializer = Config.serializer(),
            initialConfiguration = None,
            key = "SettingsRouter",
            childFactory = ::createSettingsChild,
        )

//    private data class StackTravel(val configType: KClass<out Config>, val replaceActiveItem: Boolean)
//
//    private val travelPermissions = mapOf<KClass<out Config>, List<StackTravel>>(
//        None::class to listOf(
//            StackTravel(Settings::class, true),
//            StackTravel(MessageMetadata::class, true),
//        ),
//        Settings::class to listOf(
//            StackTravel(AddMembers::class, false),
//            StackTravel(ExportRoom::class, false),
//            StackTravel(MessageMetadata::class, false),
//        ),
//        MessageMetadata::class to listOf(
//            StackTravel(Settings::class, true),
//            StackTravel(MessageMetadata::class, true),
//        ),
//        AddMembers::class to listOf(
//            StackTravel(MessageMetadata::class, false),
//        ),
//        ExportRoom::class to listOf(
//            StackTravel(MessageMetadata::class, false),
//        ),
//    )

    override suspend fun showSettings(roomId: RoomId) {
        log.debug { "show settings for room: $roomId" }
        val config = Config.RoomSettings.MainSettings(roomId)
        if (showRouter(config).not())
            settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, config)
    }

    override suspend fun closeSettings() {
        log.debug { "close room settings" }
        // TODO: clear only settings items if Metadata is open?
        settingsNavigation.popWhileSuspending { it != None }
    }

    override suspend fun showMessageMetadata(eventId: EventId, roomId: RoomId) {
        log.debug { "show message metadata for event: $eventId in room: $roomId" }
        val config = MessageMetadata(eventId, roomId)
        if (showRouter(config).not())
            settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, config)
    }

    private suspend fun showRouter(baseConfig: Config): Boolean =
        if (isSettingsShown().not()) {
            settingsNavigation.bringToFrontSuspending(baseConfig)
            true
        } else false

    override fun isSettingsShown(): Boolean = stack.value.active.configuration !is None

    private fun createSettingsChild(
        config: Config,
        componentContext: ComponentContext,
    ): Wrapper =
        when (config) {
            is None -> Wrapper.None

            is Config.RoomSettings.MainSettings -> Wrapper.View(
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

//    override suspend fun showSettings() {
//        log.debug { "show settings" }
//        if (stack.value.active.configuration is MessageMetadata) {
//            settingsNavigation.bringToFrontSuspending(stack.value.active.configuration)
//        } else {
//            settingsNavigation.bringToFrontSuspending(Config.Settings)
//        }
//    }

//    override suspend fun closeSettings() {
////        close(Config.Settings)
//        settingsNavigation.popWhileSuspending { it != None }
//    }

//    private suspend fun close(config: Config) {
//        when (config) {
//            is Settings,
//            is MessageMetadata ->
//                settingsNavigation.popWhileSuspending { it != None }
//
//            else -> {}
//        }
//    }

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

//    private fun isActive(ofType: KClass<out Config>) =
//        stack.value.active.configuration::class == ofType

    private fun KClass<out Config>.isActive(): Boolean =
        stack.value.active.configuration::class == this

//    private fun isActiveStackItem(ofType: KClass<out Config>): Boolean =
//        stack.value.active.configuration::class == ofType

//    override suspend fun showMessageMetadata(messageHolder: TimelineElementHolderViewModel) {
//        log.debug { "show metadata" }
//        if (!isSettingsShown()) settingsNavigation.bringToFrontSuspending(Config.MessageMetadata(messageHolder))
//        else settingsNavigation.launchBringToFront(
//            viewModelContext.coroutineScope,
//            Config.MessageMetadata(messageHolder),
//        )
//    }
//
//    override fun closeMessageMetadata() {
//        // TODO
////        if (stack.value.active.configuration is Config.MessageMetadata) close(stack.value.active.configuration)
//        settingsNavigation.launchPop(viewModelContext.coroutineScope)
//    }

//    override fun isSettingsShown(): Boolean = stack.value.active.configuration !is Config.None
//    override fun isMessageMetadataShown(): Boolean = stack.value.active.configuration is Config.MessageMetadata
//    override fun shownSettings(): Config = stack.value.active.configuration
}
