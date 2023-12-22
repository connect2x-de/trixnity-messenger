package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class RoomListRouter(
    private val viewModelContext: ViewModelContext,
    private val selectedRoomId: MutableStateFlow<RoomId?>,
    private val onRoomSelected: (userId: UserId, roomId: RoomId) -> Unit,
    private val onOpenAvatarCutter: (userId: UserId, file: FileDescriptor) -> Unit,
    private val onSendLogs: () -> Unit,
    private val onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (userId: UserId) -> Unit
) {

    private val navigation = StackNavigation<RoomListConfig>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = RoomListConfig.serializer(),
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
                viewModelContext.get<RoomListViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    selectedRoomId = selectedRoomId,
                    onRoomSelected = onRoomSelected,
                    onStartCreateNewRoom = ::onStartCreateNewRoom,
                    onUserSettingsSelected = ::onOpenUserSettings,
                    onOpenAppInfo = ::onOpenAppInfo,
                    onSendLogs = onSendLogs,
                    onOpenAccountsOverview = ::onOpenAccountsOverview,
                )
            )

            is RoomListConfig.CreateNewChat -> RoomListWrapper.CreateNewChat(
                viewModelContext.get<CreateNewChatViewModelFactory>()
                    .create(
                        viewModelContext.childContext(
                            componentContext,
                            roomListConfig.userId,
                        ),
                        viewModelContext.get<CreateNewRoomViewModelFactory>()
                            .create(
                                viewModelContext.childContext(
                                    componentContext,
                                    roomListConfig.userId,
                                )
                            ),
                        onCreateGroup = ::onCreateGroup,
                        onSearchGroup = ::onSearchGroup,
                        onCancel = ::onCancelCreateNewChat,
                        goToRoom = ::goToRoom,
                    )
            )

            is RoomListConfig.CreateNewGroup -> RoomListWrapper.CreateNewGroup(
                viewModelContext.get<CreateNewGroupViewModelFactory>()
                    .create(
                        viewModelContext.childContext(
                            componentContext,
                            roomListConfig.userId,
                        ),
                        viewModelContext.get<CreateNewRoomViewModelFactory>()
                            .create(
                                viewModelContext.childContext(
                                    componentContext,
                                    roomListConfig.userId,
                                )
                            ),
                        onBack = ::onCancelCreateNewGroup,
                        onGroupCreated = ::onGroupCreated,
                    )
            )

            is RoomListConfig.SearchGroup -> RoomListWrapper.SearchGroup(
                viewModelContext.get<SearchGroupViewModelFactory>().create(
                    viewModelContext.childContext(
                        componentContext,
                        roomListConfig.userId,
                    ),
                    onBack = ::onCancelSearchGroup,
                    onGroupJoined = ::onGroupJoined,
                )
            )

            is RoomListConfig.UserSettings -> RoomListWrapper.UserSettings(
                viewModelContext.get<UserSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseUserSettings = ::onCloseUserSettings,
                    onShowDevicesSettings = ::onShowDevicesSettings,
                    onShowProfile = ::onShowProfile,
                    onShowNotificationsSettings = ::onShowNotificationsSettings,
                    onShowPrivacySettings = ::onShowPrivacySettings,
                )
            )

            is RoomListConfig.DevicesSettings -> RoomListWrapper.DevicesSettings(
                viewModelContext.get<DevicesSettingsViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCloseDevicesSettings = ::onCloseDevicesSettings,
                    )
            )

            is RoomListConfig.Profile -> RoomListWrapper.Profile(
                viewModelContext.get<ProfileViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseProfile = ::onCloseProfile,
                    onOpenAvatarCutter = onOpenAvatarCutter,
                )
            )

            is RoomListConfig.NotificationsSettings -> RoomListWrapper.NotificationsSettings(
                viewModelContext.get<NotificationsSettingsViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCloseNotificationsSettings = ::onCloseNotificationsSettings,
                        onShowConfigureNotifications = ::onShowConfigureNotifications,
                    )
            )

            is RoomListConfig.PrivacySettings -> RoomListWrapper.PrivacySettings(
                viewModelContext.get<PrivacySettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onClosePrivacySettings = ::onClosePrivacySettings,
                )
            )

            is RoomListConfig.ConfigureNotifications -> RoomListWrapper.ConfigureNotifications(
                viewModelContext.get<ConfigureNotificationsViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            roomListConfig.userId,
                        ),
                        onCloseConfigureNotifications = ::onCloseConfigureNotifications,
                    )
            )

            is RoomListConfig.AppInfo -> RoomListWrapper.AppInfo(
                viewModelContext.get<AppInfoViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseAppInfo = ::onCloseAppInfo,
                )
            )

            is RoomListConfig.AccountsOverview -> RoomListWrapper.AccountsOverview(
                viewModelContext.get<AccountsOverviewViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCreateNewAccount = onCreateNewAccount,
                    onRemoveAccount = onRemoveAccount,
                    onClose = ::onCloseAccountsOverview,
                )
            )
        }

    private fun onStartCreateNewRoom(userId: UserId) {
        log.debug { "on create new chat" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.CreateNewChat(userId))
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

    private fun onCreateGroup(userId: UserId) {
        log.debug { "on create group in account $userId" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.CreateNewGroup(userId))
    }

    private fun onCancelCreateNewGroup() {
        log.debug { "on cancel create new group" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onGroupCreated(userId: UserId, roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "on group created ($roomId)" }
        navigation.popWhileSuspending { it !is RoomListConfig.RoomList }
        selectedRoomId.value = roomId
        onRoomSelected(userId, roomId)
    }

    private fun onSearchGroup(userId: UserId) {
        log.debug { "on search group in account $userId" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.SearchGroup(userId))
    }

    private fun onCancelSearchGroup() {
        log.debug { "on cancel search group" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onGroupJoined(userId: UserId, roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "on group joined ($roomId)" }
        navigation.popWhileSuspending { it !is RoomListConfig.RoomList }
        selectedRoomId.value = roomId
        onRoomSelected(userId, roomId)
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

    private fun onShowPrivacySettings() {
        log.debug { "show privacy settings" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.PrivacySettings)
    }

    private fun onClosePrivacySettings() {
        log.debug { "close privacy settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowConfigureNotifications(userId: UserId) {
        log.debug { "configure notifications for account $userId" }
        navigation.launchPush(viewModelContext.coroutineScope, RoomListConfig.ConfigureNotifications(userId))
    }

    private fun onCloseConfigureNotifications() = viewModelContext.coroutineScope.launch {
        log.debug { "close configure notification settings" }
        navigation.popSuspending()
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

    @Serializable
    sealed class RoomListConfig {
        @Serializable
        data object RoomList : RoomListConfig()

        @Serializable
        data class CreateNewChat(val userId: UserId) : RoomListConfig()

        @Serializable
        data class CreateNewGroup(val userId: UserId) : RoomListConfig()

        @Serializable
        data class SearchGroup(val userId: UserId) : RoomListConfig()

        @Serializable
        data object UserSettings : RoomListConfig()

        @Serializable
        data object DevicesSettings : RoomListConfig()

        @Serializable
        data object Profile : RoomListConfig()

        @Serializable
        data object NotificationsSettings : RoomListConfig()

        @Serializable
        data object PrivacySettings : RoomListConfig()

        @Serializable
        data class ConfigureNotifications(val userId: UserId) : RoomListConfig()

        @Serializable
        data object AppInfo : RoomListConfig()

        @Serializable
        data object AccountsOverview : RoomListConfig()

        @Serializable
        data object None : RoomListConfig()
    }

    sealed class RoomListWrapper {
        class List(val roomListViewModel: RoomListViewModel) : RoomListWrapper()
        class CreateNewChat(val createNewChatViewModel: CreateNewChatViewModel) : RoomListWrapper()
        class CreateNewGroup(val createNewGroupViewModel: CreateNewGroupViewModel) :
            RoomListWrapper()

        class SearchGroup(val searchGroupViewModel: SearchGroupViewModel) : RoomListWrapper()

        class UserSettings(val userSettingsViewModel: UserSettingsViewModel) : RoomListWrapper()
        class DevicesSettings(val devicesSettingsViewModel: DevicesSettingsViewModel) :
            RoomListWrapper()

        class Profile(val profileViewModel: ProfileViewModel) : RoomListWrapper()
        class NotificationsSettings(val notificationsSettingsViewModel: NotificationsSettingsViewModel) :
            RoomListWrapper()

        class PrivacySettings(val privacySettingsViewModel: PrivacySettingsViewModel) : RoomListWrapper()

        class ConfigureNotifications(val configureNotificationsViewModel: ConfigureNotificationsViewModel) :
            RoomListWrapper()

        class AppInfo(val appInfoViewModel: AppInfoViewModel) : RoomListWrapper()
        class AccountsOverview(val accountsOverviewViewModel: AccountsOverviewViewModel) : RoomListWrapper()

        data object None : RoomListWrapper()
    }
}