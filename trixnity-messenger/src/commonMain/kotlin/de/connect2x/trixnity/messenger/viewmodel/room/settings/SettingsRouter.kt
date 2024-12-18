package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.launchBringToFront
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface SettingsRouter {
    val stack: Value<ChildStack<Config, Wrapper>>
    suspend fun showSettings()
    suspend fun closeSettings()
    suspend fun showMessageMetadata(messageHolder: TimelineElementHolderViewModel)
    fun closeMessageMetadata()
    fun isSettingsShown(): Boolean
//    fun isMessageMetadataShown(): Boolean
//    fun shownSettings(): Config

    sealed class Wrapper {
        data object None : Wrapper()
        class View(val viewModel: RoomSettingsViewModel) : Wrapper()
        class AddMember(val viewModel: AddMembersViewModel) : Wrapper()
        class ExportRoom(val viewModel: ExportRoomViewModel) : Wrapper()
        class MessageMetadata(val viewModel: UnifiedMessageMetadataViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data object Settings : Config()

        @Serializable
        data object AddMembers : Config()

        @Serializable
        data object ExportRoom : Config()

        @Serializable
        data class MessageMetadata(val messageHolder: TimelineElementHolderViewModel) : Config()
    }
}

class SettingsRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onSettingsBack: () -> Unit,
    private val onRoomBack: () -> Unit,
    private val onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
) : SettingsRouter {

    private val settingsNavigation = StackNavigation<Config>()
    override val stack =
        viewModelContext.childStack(
            source = settingsNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "SettingsRouter",
            childFactory = ::createSettingsChild,
        )

    private fun createSettingsChild(
        settingsConfig: Config,
        componentContext: ComponentContext,
    ): Wrapper =
        when (settingsConfig) {
            is Config.None -> Wrapper.None
            is Config.Settings -> Wrapper.View(
                viewModelContext.get<RoomSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onBack = onRoomBack,
                    selectedRoomId = roomId,
                    onShowAddMembers = ::showAddMembers,
                    onShowExportRoom = ::showExportRoom,
                    onCloseRoomSettings = onSettingsBack,
                    onOpenAvatarCutter = onOpenAvatarCutter,
                )
            )

            is Config.AddMembers -> Wrapper.AddMember(
                viewModelContext.get<AddMembersViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onBack = ::closeAddMembers,
                    roomId = roomId,
                    addMembersToRoomViewModel = viewModelContext.get<PotentialMembersViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext(componentContext),
                            roomId = roomId
                        ),
                )
            )

            is Config.ExportRoom -> Wrapper.ExportRoom(
                viewModelContext.get<ExportRoomViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    roomId = roomId,
                    onBack = ::closeExportRoom,
                )
            )

            is Config.MessageMetadata -> Wrapper.MessageMetadata(
                viewModelContext.get<UnifiedMessageMetadataViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    message = settingsConfig.messageHolder,
                    roomId = roomId,
                    onBack = ::closeMessageMetadata,
                )
            )
        }

    override suspend fun showSettings() {
        log.debug { "show settings" }
        if (stack.value.active.configuration is Config.MessageMetadata) {
            settingsNavigation.bringToFrontSuspending(stack.value.active.configuration)
        } else {
            settingsNavigation.bringToFrontSuspending(Config.Settings)
        }
    }

    override suspend fun closeSettings() {
//        close(Config.Settings)
        settingsNavigation.popWhileSuspending { it != Config.None }
    }

    private suspend fun close(config: Config) {
        when (config) {
            is Config.Settings,
            is Config.MessageMetadata ->
                settingsNavigation.popWhileSuspending { it != Config.None }

            else -> {}
        }
    }

    private fun showAddMembers() {
        settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, Config.AddMembers)
    }

    private fun closeAddMembers() {
        settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun showExportRoom() {
        settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, Config.ExportRoom)
    }

    private fun closeExportRoom() {
        settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    override suspend fun showMessageMetadata(messageHolder: TimelineElementHolderViewModel) {
        log.debug { "show metadata" }
        if (!isSettingsShown()) settingsNavigation.bringToFrontSuspending(Config.MessageMetadata(messageHolder))
        else settingsNavigation.launchBringToFront(
            viewModelContext.coroutineScope,
            Config.MessageMetadata(messageHolder),
        )
    }

    override fun closeMessageMetadata() {
        // TODO
//        if (stack.value.active.configuration is Config.MessageMetadata) close(stack.value.active.configuration)
        settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    override fun isSettingsShown(): Boolean = stack.value.active.configuration !is Config.None
//    override fun isMessageMetadataShown(): Boolean = stack.value.active.configuration is Config.MessageMetadata
//    override fun shownSettings(): Config = stack.value.active.configuration
}
