package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import de.connect2x.trixnity.messenger.util.launchBringToFront
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.SettingsConfig
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter.SettingsWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface SettingsRouter {
    val settingsStack: Value<ChildStack<SettingsConfig, SettingsWrapper>>
    suspend fun showSettings()
    suspend fun closeSettings()
    fun isShown(): Boolean

    sealed class SettingsWrapper {
        object None : SettingsWrapper()
        class View(val settingsViewModel: RoomSettingsViewModel) : SettingsWrapper()
        class AddMember(val addMembersViewModel: AddMembersViewModel) : SettingsWrapper()
    }

    sealed class SettingsConfig : Parcelable {
        @Parcelize
        object None : SettingsConfig()

        @Parcelize
        object Settings : SettingsConfig()

        @Parcelize
        object AddMembers : SettingsConfig()
    }

}

class SettingsRouterImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onSettingsBack: () -> Unit,
    private val onRoomBack: () -> Unit,
) : SettingsRouter {

    private val settingsNavigation = StackNavigation<SettingsConfig>()
    override val settingsStack =
        viewModelContext.childStack(
            source = settingsNavigation,
            initialConfiguration = SettingsConfig.None,
            key = "SettingsRouter",
            childFactory = ::createSettingsChild,
        )

    private fun createSettingsChild(
        settingsConfig: SettingsConfig,
        componentContext: ComponentContext
    ): SettingsWrapper =
        when (settingsConfig) {
            is SettingsConfig.None -> SettingsWrapper.None
            is SettingsConfig.Settings -> SettingsWrapper.View(
                viewModelContext.get<RoomSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onBack = onRoomBack,
                    selectedRoomId = roomId,
                    onShowAddMembers = ::showAddMembers,
                    onCloseRoomSettings = onSettingsBack,
                )
            )

            is SettingsConfig.AddMembers -> SettingsWrapper.AddMember(
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
        settingsNavigation.bringToFrontSuspending(SettingsConfig.Settings)
    }

    override suspend fun closeSettings() {
        settingsNavigation.popWhileSuspending { it != SettingsConfig.None }
    }

    private fun showAddMembers() {
        settingsNavigation.launchBringToFront(viewModelContext.coroutineScope, SettingsConfig.AddMembers)
    }

    private fun closeAddMembers() {
        settingsNavigation.launchPop(viewModelContext.coroutineScope)
    }

    override fun isShown(): Boolean =
        when (settingsStack.value.active.configuration) {
            is SettingsConfig.Settings -> true
            is SettingsConfig.AddMembers -> true
            is SettingsConfig.None -> false
        }
}
