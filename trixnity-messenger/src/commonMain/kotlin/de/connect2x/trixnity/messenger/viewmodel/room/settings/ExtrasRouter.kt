package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.navigateSuspending
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.Details.TimelineElementMetadata
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.Details.UserProfile
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.None
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.AddMembers
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.DevInfos
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.ExportRoom
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings.PowerLevels
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

interface ExtrasRouter {
    val stack: Value<ChildStack<Config, Wrapper>>

    suspend fun back()
    suspend fun closeAll()
    suspend fun openRoomSettings(roomId: RoomId)
    suspend fun openAddMembers(roomId: RoomId)
    suspend fun openDevInfo(roomId: RoomId)
    suspend fun openExportRoom(roomId: RoomId)
    suspend fun openPowerLevel(roomId: RoomId)
    suspend fun openUserProfile(userId: UserId, roomId: RoomId)
    suspend fun openTimelineElementMetadata(eventId: EventId, roomId: RoomId)

    sealed class Wrapper {
        data object None : Wrapper()
        class UserProfile(val viewModel: UserProfileViewModel) : Wrapper()
        class TimelineElementMetadata(val viewModel: TimelineElementMetadataViewModel) : Wrapper()
        class RoomSettings(val viewModel: RoomSettingsViewModel) : Wrapper()
        class AddMember(val viewModel: AddMembersViewModel) : Wrapper()
        class DevInfo(val viewModel: DevInfoViewModel) : Wrapper()
        class ExportRoom(val viewModel: ExportRoomViewModel) : Wrapper()
        class PowerLevels(val viewModel: PowerlevelViewModel) : Wrapper()
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
            data class DevInfos(val roomId: RoomId): RoomSettings

            @Serializable
            data class PowerLevels(val roomId: RoomId) : RoomSettings

            @Serializable
            data class ExportRoom(val roomId: RoomId) : RoomSettings
        }

        @Serializable
        sealed interface Details : Config {
            @Serializable
            data class UserProfile(val userId: UserId, val roomId: RoomId) : RoomSettings

            @Serializable
            data class TimelineElementMetadata(val eventId: EventId, val roomId: RoomId) : Config
        }

        @Serializable
        data object None : Config
    }
}

class ExtrasRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val onOpenRoom: (UserId, RoomId) -> Unit,
    private val onCloseRoom: () -> Unit,
    private val onOpenAvatarCutter: OpenAvatarCutterCallback,
    private val onOpenMention: OpenMentionCallback,
) : ExtrasRouter {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouterImpl")
    }

    private val extrasNavigation = StackNavigation<Config>()
    override val stack = viewModelContext.childStack(
        source = extrasNavigation,
        serializer = Config.serializer(),
        initialConfiguration = None,
        key = "ExtrasRouter",
        childFactory = ::createSettingsChild,
    )

    override suspend fun back() {
        val config = stack.value.active.configuration
        extrasNavigation.popSuspending {
            log.debug { "extras: closed $config ${it.toSuccessString()}" }
        }
    }

    override suspend fun closeAll() {
        extrasNavigation.replaceAllSuspending(None) {
            log.debug { "extras: closed pane" }
        }
    }

    override suspend fun openRoomSettings(roomId: RoomId) {
        extrasNavigation.replaceAllSuspending(None, RoomSettings.Main(roomId)) {
            log.debug { "extras: opened room settings for room: $roomId" }
        }
    }

    override suspend fun openAddMembers(roomId: RoomId) {
        if (stack.value.active.configuration !is RoomSettings) {
            openRoomSettings(roomId)
        }
        extrasNavigation.pushSuspending(AddMembers(roomId)) {
            log.debug { "extras: opened add members for room: $roomId" }
        }
    }

    override suspend fun openDevInfo(roomId: RoomId) {
        if (stack.value.active.configuration !is RoomSettings) {
            openRoomSettings(roomId)
        }
        extrasNavigation.pushSuspending(DevInfos(roomId)) {
            log.debug { "extras: opened dev information for room: $roomId" }
        }
    }

    override suspend fun openPowerLevel(roomId: RoomId) {
        if (stack.value.active.configuration !is RoomSettings) {
            openRoomSettings(roomId)
        }
        extrasNavigation.pushSuspending(PowerLevels(roomId)) {
            log.debug { "extras: opened powerlevel for room: $roomId" }
        }
    }

    override suspend fun openExportRoom(roomId: RoomId) {
        if (stack.value.active.configuration !is RoomSettings) {
            openRoomSettings(roomId)
        }
        extrasNavigation.pushSuspending(ExportRoom(roomId)) {
            log.debug { "extras: opened export room for room: $roomId" }
        }
    }

    override suspend fun openUserProfile(userId: UserId, roomId: RoomId) {
        extrasNavigation.navigateSuspending {
            it.filterNot { it is UserProfile } + UserProfile(userId, roomId)
        }
        log.debug { "extras: opened user profile for user: $userId in room: $roomId" }
    }

    override suspend fun openTimelineElementMetadata(eventId: EventId, roomId: RoomId) {
        extrasNavigation.navigateSuspending {
            it.filterNot { it is TimelineElementMetadata } + TimelineElementMetadata(eventId, roomId)
        }
        log.debug { "extras: opened message metadata for event: $eventId from room $roomId" }
    }

    private fun createSettingsChild(
        config: Config,
        componentContext: ComponentContext,
    ): Wrapper = when (config) {
        is None -> Wrapper.None

        is RoomSettings.Main -> Wrapper.RoomSettings(
            viewModelContext.get<RoomSettingsViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext("RoomSettingsMain", componentContext),
                onCloseRoom = onCloseRoom,
                selectedRoomId = config.roomId,
                onOpenAddMembers = { onOpenAddMembers(config.roomId) },
                onOpenDevInfo = { onOpenDevInfo(config.roomId)},
                onOpenExportRoom = { onOpenExportRoom(config.roomId) },
                onCloseRoomSettings = ::onCloseRoomSettings,
                onOpenAvatarCutter = onOpenAvatarCutter,
                onOpenUserProfile = { onOpenUserProfile(it, config.roomId) },
                onOpenMention = onOpenMention,
                onOpenPowerLevel = { onOpenPowerLevel(config.roomId) }
            )
        )

        is AddMembers -> Wrapper.AddMember(
            viewModelContext.get<AddMembersViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext("AddMembers", componentContext),
                onBack = ::onBack,
                roomId = config.roomId,
                addMembersToRoomViewModel = viewModelContext.get<PotentialMembersViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext("PartialMembers", componentContext),
                        roomId = config.roomId,
                    ),
            )
        )

        is DevInfos -> Wrapper.DevInfo(
            viewModelContext.get<DevInfoViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext(componentContext),
                roomId = config.roomId,
                onBack = ::onBack,
            )
        )

        is PowerLevels -> Wrapper.PowerLevels(
            viewModelContext.get<PowerlevelViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext("PowerLevels", componentContext),
                roomId = config.roomId,
                onBack = ::onBack,
            )
        )

        is ExportRoom -> Wrapper.ExportRoom(
            viewModelContext.get<ExportRoomViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext("ExportRoom", componentContext),
                roomId = config.roomId,
                onBack = ::onBack,
            )
        )

        is UserProfile -> Wrapper.UserProfile(
            viewModelContext.get<UserProfileViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext("UserProfile", componentContext),
                userId = config.userId,
                selectedRoomId = config.roomId,
                onOpenRoom = onOpenRoom,
                onBack = ::onBack,
                onCloseSettings = ::onCloseRoomSettings
            )
        )

        is TimelineElementMetadata -> Wrapper.TimelineElementMetadata(
            viewModelContext.get<TimelineElementMetadataViewModelFactory>().create(
                viewModelContext = viewModelContext.childContext("TimelineElementMetadata", componentContext),
                eventId = config.eventId,
                roomId = config.roomId,
                onOpenUserProfile = { onOpenUserProfile(it, config.roomId) },
                onBack = ::onBack,
            )
        )
    }

    private fun onBack() =
        viewModelContext.coroutineScope.launch {
            back()
        }

    private fun onOpenAddMembers(roomId: RoomId) =
        viewModelContext.coroutineScope.launch {
            openAddMembers(roomId)
        }

    private fun onOpenDevInfo(roomId: RoomId) =
        viewModelContext.coroutineScope.launch {
            openDevInfo(roomId)
        }

    private fun onOpenExportRoom(roomId: RoomId) =
        viewModelContext.coroutineScope.launch {
            openExportRoom(roomId)
        }

    private fun onCloseRoomSettings() =
        viewModelContext.coroutineScope.launch {
            closeAll()
        }

    private fun onOpenUserProfile(userId: UserId, roomId: RoomId) =
        viewModelContext.coroutineScope.launch {
            openUserProfile(userId, roomId)
        }

    private fun onOpenPowerLevel(roomId: RoomId) = viewModelContext.coroutineScope.launch {
        openPowerLevel(roomId)
    }

    private fun Boolean.toSuccessString() =
        if (this) "successfully" else "failed"
}
