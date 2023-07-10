package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class RoomListRouter(
    private val viewModelContext: ViewModelContext,
    private val selectedRoomId: MutableStateFlow<RoomId?>,
    private val onRoomSelected: (accountName: String, id: RoomId) -> Unit,
    private val onOpenAvatarCutter: (accountName: String, file: String) -> Unit,
    private val onSendLogs: () -> Unit,
    private val onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (String) -> Unit
) {

    private val navigation = StackNavigation<RoomListConfig>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = RoomListConfig.RoomList,
        key = "RoomListRouter",
        childFactory = ::createChild,
    )

    fun closeAccountsOverview() {
        if (stack.active.configuration is RoomListConfig.AccountsOverview) {
            log.debug { "close accounts overview" }
            navigation.launchPop(viewModelContext.coroutineScope)
        }
    }

    private fun createChild(
        roomListConfig: RoomListConfig,
        componentContext: ComponentContext
    ): RoomListWrapper =
        when (roomListConfig) {
            is RoomListConfig.None -> RoomListWrapper.None
            is RoomListConfig.RoomList -> RoomListWrapper.List(
                viewModelContext.get<RoomListViewModelFactory>().newRoomListViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    selectedRoomId = selectedRoomId,
                    onRoomSelected = onRoomSelected,
                    onCreateNewRoom = ::onCreateNewChat,
                    onUserSettingsSelected = ::onOpenUserSettings,
                    onOpenAppInfo = ::onOpenAppInfo,
                    onSendLogs = onSendLogs,
                    onOpenAccountsOverview = ::onOpenAccountsOverview,
                )
            )

            is RoomListConfig.CreateNewChat -> RoomListWrapper.CreateNewChat(
                viewModelContext.get<CreateNewChatViewModelFactory>()
                    .newCreateNewChatViewModel(
                        viewModelContext.childContext(
                            componentContext,
                            roomListConfig.accountName,
                        ),
                        viewModelContext.get<CreateNewRoomViewModelFactory>()
                            .newCreateNewRoomViewModel(
                                viewModelContext.childContext(
                                    componentContext,
                                    roomListConfig.accountName,
                                )
                            ),
                        onCreateGroup = ::onCreateGroup,
                        onCancel = ::onCancelCreateNewChat,
                        goToRoom = ::goToRoom,
                    )
            )

            is RoomListConfig.CreateNewGroup -> RoomListWrapper.CreateNewGroup(
                viewModelContext.get<CreateNewGroupViewModelFactory>()
                    .newCreateNewGroupViewModel(
                        viewModelContext.childContext(
                            componentContext,
                            roomListConfig.accountName,
                        ),
                        viewModelContext.get<CreateNewRoomViewModelFactory>()
                            .newCreateNewRoomViewModel(
                                viewModelContext.childContext(
                                    componentContext,
                                    roomListConfig.accountName,
                                )
                            ),
                        onBack = ::onCancelCreateNewGroup,
                        onGroupCreated = ::onGroupCreated,
                    )
            )

            is RoomListConfig.UserSettings -> RoomListWrapper.UserSettings(
                viewModelContext.get<UserSettingsViewModelFactory>().newUserSettingsViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseUserSettings = ::onCloseUserSettings,
                    onShowDevicesSettings = ::onShowDevicesSettings,
                    onShowProfile = ::onShowProfile,
                    onShowNotificationsSettings = ::onShowNotificationsSettings,
                )
            )

            is RoomListConfig.DevicesSettings -> RoomListWrapper.DevicesSettings(
                viewModelContext.get<DevicesSettingsViewModelFactory>()
                    .newDevicesSettingsViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCloseDevicesSettings = ::onCloseDevicesSettings,
                    )
            )

            is RoomListConfig.Profile -> RoomListWrapper.Profile(
                viewModelContext.get<ProfileViewModelFactory>().newProfileViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseProfile = ::onCloseProfile,
                    onOpenAvatarCutter = onOpenAvatarCutter,
                )
            )

            is RoomListConfig.NotificationsSettings -> RoomListWrapper.NotificationsSettings(
                viewModelContext.get<NotificationsSettingsViewModelFactory>()
                    .newNotificationsSettingsViewModel(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCloseNotificationsSettings = ::onCloseNotificationsSettings,
                        onShowConfigureNotifications = ::onShowConfigureNotifications,
                    )
            )

            is RoomListConfig.ConfigureNotifications -> RoomListWrapper.ConfigureNotifications(
                viewModelContext.get<ConfigureNotificationsViewModelFactory>()
                    .newConfigureNotificationsViewModel(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            roomListConfig.accountName,
                        ),
                        onCloseConfigureNotifications = ::onCloseConfigureNotifications,
                    )
            )

            is RoomListConfig.AppInfo -> RoomListWrapper.AppInfo(
                viewModelContext.get<AppInfoViewModelFactory>().newAppInfoViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseAppInfo = ::onCloseAppInfo,
                )
            )

            is RoomListConfig.AccountsOverview -> RoomListWrapper.AccountsOverview(
                viewModelContext.get<AccountsOverviewViewModelFactory>().newAccountsOverviewViewModel(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCreateNewAccount = onCreateNewAccount,
                    onRemoveAccount = onRemoveAccount,
                    onClose = ::onCloseAccountsOverview,
                )
            )
        }

    private fun onCreateNewChat(accountName: String) {
        log.debug { "on create new chat" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.CreateNewChat(accountName))
    }

    private fun onCancelCreateNewChat() {
        log.debug { "on cancel create new chat" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun goToRoom(roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "go to room $roomId" }
        selectedRoomId.value = roomId
//        onRoomSelected(roomId)
        navigation.popSuspending()
    }

    private fun onCreateGroup(accountName: String) {
        log.debug { "on create group in account $accountName" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.CreateNewGroup(accountName))
    }

    private fun onCancelCreateNewGroup() {
        log.debug { "on cancel create new group" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onGroupCreated(roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "on group created ($roomId)" }
        navigation.popWhileSuspending { it !is RoomListConfig.RoomList }
        selectedRoomId.value = roomId
//        onRoomSelected(roomId)
    }

    private fun onOpenUserSettings() {
        log.debug { "open user settings" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.UserSettings)
    }

    private fun onCloseUserSettings() {
        log.debug { "close user settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onOpenAppInfo() {
        log.debug { "open app info" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.AppInfo)
    }

    private fun onCloseAppInfo() {
        log.debug { "close app info" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowDevicesSettings() {
        log.debug { "show device settings" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.DevicesSettings)
    }

    private fun onCloseDevicesSettings() {
        log.debug { "close device settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowProfile() {
        log.debug { "show profile" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.Profile)
    }

    private fun onCloseProfile() {
        log.debug { "close profile" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowNotificationsSettings() {
        log.debug { "show notification settings" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.NotificationsSettings)
    }

    private fun onCloseNotificationsSettings() {
        log.debug { "close notification settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowConfigureNotifications(accountName: String) {
        log.debug { "configure notifications for account $accountName" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.ConfigureNotifications(accountName))
    }

    private fun onCloseConfigureNotifications() = viewModelContext.coroutineScope.launch {
        log.debug { "close configure notification settings" }
        navigation.popSuspending()
        if (stack.value.active.configuration is RoomListConfig.NotificationsSettings) {
            (stack.value.active.instance as RoomListWrapper.NotificationsSettings)
                .notificationsSettingsViewModel.reloadNotificationSettings()
        }
    }

    private fun onOpenAccountsOverview() {
        log.debug { "open accounts overview" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.AccountsOverview)
    }

    private fun onCloseAccountsOverview() {
        log.debug { "close accounts overview" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    suspend fun moveToBackStack() {
        if (stack.value.active.configuration !is RoomListConfig.None) {
            log.debug { "move active view to back (push Config.None)" }
            navigation.pushSuspending(RoomListConfig.None)
        }
    }

    suspend fun show() {
        if (stack.value.active.configuration is RoomListConfig.None) {
            log.debug { "move view to front (pop Config.None)" }
            navigation.popSuspending()
        }
    }

    suspend fun close() {
        log.debug { "close" }
        navigation.popSuspending()
    }

    fun isShown(): Boolean {
        return stack.value.active.configuration is RoomListConfig.RoomList
    }

    sealed class RoomListConfig : Parcelable {
        @Parcelize
        object RoomList : RoomListConfig()

        @Parcelize
        data class CreateNewChat(val accountName: String) : RoomListConfig()

        @Parcelize
        data class CreateNewGroup(val accountName: String) : RoomListConfig()

        @Parcelize
        object UserSettings : RoomListConfig()

        @Parcelize
        object DevicesSettings : RoomListConfig()

        @Parcelize
        object Profile : RoomListConfig()

        @Parcelize
        object NotificationsSettings : RoomListConfig()

        @Parcelize
        data class ConfigureNotifications(val accountName: String) : RoomListConfig()

        @Parcelize
        object AppInfo : RoomListConfig()

        @Parcelize
        object AccountsOverview : RoomListConfig()

        @Parcelize
        object None : RoomListConfig()
    }

    sealed class RoomListWrapper {
        class List(val roomListViewModel: RoomListViewModel) : RoomListWrapper()
        class CreateNewChat(val createNewChatViewModel: CreateNewChatViewModel) : RoomListWrapper()
        class CreateNewGroup(val createNewGroupViewModel: CreateNewGroupViewModel) :
            RoomListWrapper()

        class UserSettings(val userSettingsViewModel: UserSettingsViewModel) : RoomListWrapper()
        class DevicesSettings(val devicesSettingsViewModel: DevicesSettingsViewModel) :
            RoomListWrapper()

        class Profile(val profileViewModel: ProfileViewModel) : RoomListWrapper()
        class NotificationsSettings(val notificationsSettingsViewModel: NotificationsSettingsViewModel) :
            RoomListWrapper()

        class ConfigureNotifications(val configureNotificationsViewModel: ConfigureNotificationsViewModel) :
            RoomListWrapper()

        class AppInfo(val appInfoViewModel: AppInfoViewModel) : RoomListWrapper()
        class AccountsOverview(val accountsOverviewViewModel: AccountsOverviewViewModel) : RoomListWrapper()

        object None : RoomListWrapper()
    }
}