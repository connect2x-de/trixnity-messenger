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
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface SettingsRouter {
    val settingsStack: Value<ChildStack<Config, Wrapper>>
    suspend fun showSettings()
    suspend fun closeSettings()
    fun isShown(): Boolean

    sealed class Wrapper {
        data object None : Wrapper()
        class View(val viewModel: RoomSettingsViewModel) : Wrapper()
        class AddMember(val viewModel: AddMembersViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data object Settings : Config()

        @Serializable
        data object AddMembers : Config()
    }

}

class SettingsRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onSettingsBack: () -> Unit,
    private val onRoomBack: () -> Unit,
) : SettingsRouter {

    private val settingsNavigation = StackNavigation<Config>()
    override val settingsStack =
        viewModelContext.childStack(
            source = settingsNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.None,
            key = "SettingsRouter",
            childFactory = ::createSettingsChild,
        )

    private fun createSettingsChild(
        settingsConfig: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (settingsConfig) {
            is Config.None -> Wrapper.None
            is Config.Settings -> Wrapper.View(
                viewModelContext.get<RoomSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onBack = onRoomBack,
                    selectedRoomId = roomId,
                    onShowAddMembers = ::showAddMembers,
                    onCloseRoomSettings = onSettingsBack,
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
        }

    override suspend fun showSettings() {
        log.debug { "show settings" }
        settingsNavigation.bringToFrontSuspending(Config.Settings)
    }

    override suspend fun closeSettings() {
        settingsNavigation.popWhileSuspending { it != Config.None }
    }

    private fun showAddMembers() {
        settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, Config.AddMembers)
    }

    private fun closeAddMembers() {
        settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    override fun isShown(): Boolean =
        when (settingsStack.value.active.configuration) {
            is Config.Settings -> true
            is Config.AddMembers -> true
            is Config.None -> false
        }
}
