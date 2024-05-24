package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.ConfigureNotificationsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ConfigureNotificationsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.DevicesSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.DevicesSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationsSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationsSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
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

    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.RoomList,
        key = "RoomListRouter",
        childFactory = ::createChild,
    )

    fun closeAccountsOverview() {
        if (stack.active.configuration is Config.AccountsOverview) {
            log.debug { "close accounts overview" }
            navigation.launchPop(viewModelContext.coroutineScope)
        }
    }

    private fun createChild(
        roomListConfig: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (roomListConfig) {
            is Config.None -> Wrapper.None
            is Config.RoomList -> Wrapper.List(
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

            is Config.CreateNewChat -> Wrapper.CreateNewChat(
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

            is Config.CreateNewGroup -> Wrapper.CreateNewGroup(
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

            is Config.SearchGroup -> Wrapper.SearchGroup(
                viewModelContext.get<SearchGroupViewModelFactory>().create(
                    viewModelContext.childContext(
                        componentContext,
                        roomListConfig.userId,
                    ),
                    onBack = ::onCancelSearchGroup,
                    onGroupJoined = ::onGroupJoined,
                )
            )

            is Config.UserSettings -> Wrapper.UserSettings(
                viewModelContext.get<UserSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseUserSettings = ::onCloseUserSettings,
                    onShowDevicesSettings = ::onShowDevicesSettings,
                    onShowProfile = ::onShowProfile,
                    onShowNotificationsSettings = ::onShowNotificationsSettings,
                    onShowPrivacySettings = ::onShowPrivacySettings,
                    onShowAppearanceSettings = ::onShowAppearanceSettings,
                )
            )

            is Config.DevicesSettings -> Wrapper.DevicesSettings(
                viewModelContext.get<DevicesSettingsViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCloseDevicesSettings = ::onCloseDevicesSettings,
                    )
            )

            is Config.Profile -> Wrapper.Profile(
                viewModelContext.get<ProfileViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseProfile = ::onCloseProfile,
                    onOpenAvatarCutter = onOpenAvatarCutter,
                )
            )

            is Config.NotificationsSettings -> Wrapper.NotificationsSettings(
                viewModelContext.get<NotificationsSettingsViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(componentContext),
                        onCloseNotificationsSettings = ::onCloseNotificationsSettings,
                        onShowConfigureNotifications = ::onShowConfigureNotifications,
                    )
            )

            is Config.PrivacySettings -> Wrapper.PrivacySettings(
                viewModelContext.get<PrivacySettingsAllAccountsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onClosePrivacySettings = ::onClosePrivacySettings,
                    onShowBlockedContactsSettings = ::onShowBlockedContactsSettings,
                )
            )

            is Config.AppearanceSettings -> Wrapper.AppearanceSettings(
                viewModelContext.get<AppearanceSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseAppearanceSettings = ::onCloseAppearanceSettings,
                )
            )

            is Config.BlockedContactsSettings -> Wrapper.BlockedContactsSettings(
                viewModelContext.get<BlockedContactsSettingsViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext).childContext(
                        componentContext,
                        roomListConfig.account,
                    ),
                    onCloseBlockedContactsSettings = ::onCloseBlockedContactsSettings,
                )
            )

            is Config.ConfigureNotifications -> Wrapper.ConfigureNotifications(
                viewModelContext.get<ConfigureNotificationsViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            roomListConfig.userId,
                        ),
                        onCloseConfigureNotifications = ::onCloseConfigureNotifications,
                    )
            )

            is Config.AppInfo -> Wrapper.AppInfo(
                viewModelContext.get<AppInfoViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext),
                    onCloseAppInfo = ::onCloseAppInfo,
                )
            )

            is Config.AccountsOverview -> Wrapper.AccountsOverview(
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
        navigation.launchPush(viewModelContext.coroutineScope, Config.CreateNewChat(userId))
    }

    private fun onCancelCreateNewChat() {
        log.debug { "on cancel create new chat" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun goToRoom(userId: UserId, roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "go to room $roomId" }
        selectedRoomId.value = roomId
        navigation.popSuspending()
        onRoomSelected(userId, roomId)
    }

    private fun onCreateGroup(userId: UserId) {
        log.debug { "on create group in account $userId" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.CreateNewGroup(userId))
    }

    private fun onCancelCreateNewGroup() {
        log.debug { "on cancel create new group" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onGroupCreated(userId: UserId, roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "on group created ($roomId)" }
        navigation.popWhileSuspending { it !is Config.RoomList }
        selectedRoomId.value = roomId
        onRoomSelected(userId, roomId)
    }

    private fun onSearchGroup(userId: UserId) {
        log.debug { "on search group in account $userId" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.SearchGroup(userId))
    }

    private fun onCancelSearchGroup() {
        log.debug { "on cancel search group" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onGroupJoined(userId: UserId, roomId: RoomId) = viewModelContext.coroutineScope.launch {
        log.debug { "on group joined ($roomId)" }
        navigation.popWhileSuspending { it !is Config.RoomList }
        selectedRoomId.value = roomId
        onRoomSelected(userId, roomId)
    }

    private fun onOpenUserSettings() {
        log.debug { "open user settings" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.UserSettings)
    }

    private fun onCloseUserSettings() {
        log.debug { "close user settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onOpenAppInfo() {
        log.debug { "open app info" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.AppInfo)
    }

    private fun onCloseAppInfo() {
        log.debug { "close app info" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowDevicesSettings() {
        log.debug { "show device settings" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.DevicesSettings)
    }

    private fun onCloseDevicesSettings() {
        log.debug { "close device settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowProfile() {
        log.debug { "show profile" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.Profile)
    }

    private fun onCloseProfile() {
        log.debug { "close profile" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowNotificationsSettings() {
        log.debug { "show notification settings" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.NotificationsSettings)
    }

    private fun onCloseNotificationsSettings() {
        log.debug { "close notification settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowPrivacySettings() {
        log.debug { "show privacy settings" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.PrivacySettings)
    }

    private fun onShowAppearanceSettings() {
        log.debug { "show appearance settings" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.AppearanceSettings)
    }

    private fun onClosePrivacySettings() {
        log.debug { "close privacy settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onCloseAppearanceSettings() {
        log.debug { "close appearance settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowBlockedContactsSettings(account: UserId) {
        log.debug { "show blocked contacts settings for account $account" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.BlockedContactsSettings(account))
    }

    private fun onCloseBlockedContactsSettings() {
        log.debug { "close blocked contacts settings" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    private fun onShowConfigureNotifications(userId: UserId) {
        log.debug { "configure notifications for account $userId" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.ConfigureNotifications(userId))
    }

    private fun onCloseConfigureNotifications() = viewModelContext.coroutineScope.launch {
        log.debug { "close configure notification settings" }
        navigation.popSuspending()
    }

    private fun onOpenAccountsOverview() {
        log.debug { "open accounts overview" }
        navigation.launchPush(viewModelContext.coroutineScope, Config.AccountsOverview)
    }

    private fun onCloseAccountsOverview() {
        log.debug { "close accounts overview" }
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    suspend fun moveToBackStack() {
        if (stack.value.active.configuration !is Config.None) {
            log.debug { "move active view to back (push Config.None)" }
            navigation.pushSuspending(Config.None)
        }
    }

    suspend fun show() {
        if (stack.value.active.configuration is Config.None) {
            log.debug { "move view to front (pop Config.None)" }
            navigation.popSuspending()
        }
    }

    suspend fun close() {
        log.debug { "close" }
        navigation.popSuspending()
    }

    fun isShown(): Boolean {
        return stack.value.active.configuration is Config.RoomList
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object RoomList : Config()

        @Serializable
        data class CreateNewChat(val userId: UserId) : Config()

        @Serializable
        data class CreateNewGroup(val userId: UserId) : Config()

        @Serializable
        data class SearchGroup(val userId: UserId) : Config()

        @Serializable
        data object UserSettings : Config()

        @Serializable
        data object DevicesSettings : Config()

        @Serializable
        data object Profile : Config()

        @Serializable
        data object NotificationsSettings : Config()

        @Serializable
        data object PrivacySettings : Config()

        @Serializable
        data object AppearanceSettings : Config()

        @Serializable
        data class BlockedContactsSettings(val account: UserId) : Config()

        @Serializable
        data class ConfigureNotifications(val userId: UserId) : Config()

        @Serializable
        data object AppInfo : Config()

        @Serializable
        data object AccountsOverview : Config()

        @Serializable
        data object None : Config()
    }

    sealed class Wrapper {
        class List(val viewModel: RoomListViewModel) : Wrapper()
        class CreateNewChat(val viewModel: CreateNewChatViewModel) : Wrapper()
        class CreateNewGroup(val viewModel: CreateNewGroupViewModel) : Wrapper()
        class SearchGroup(val viewModel: SearchGroupViewModel) : Wrapper()
        class UserSettings(val viewModel: UserSettingsViewModel) : Wrapper()
        class DevicesSettings(val viewModel: DevicesSettingsViewModel) : Wrapper()
        class Profile(val viewModel: ProfileViewModel) : Wrapper()
        class NotificationsSettings(val viewModel: NotificationsSettingsViewModel) : Wrapper()
        class PrivacySettings(val viewModel: PrivacySettingsAllAccountsViewModel) : Wrapper()
        class AppearanceSettings(val viewModel: AppearanceSettingsViewModel) : Wrapper()
        class BlockedContactsSettings(val viewModel: BlockedContactsSettingsViewModel) : Wrapper()
        class ConfigureNotifications(val viewModel: ConfigureNotificationsViewModel) : Wrapper()
        class AppInfo(val viewModel: AppInfoViewModel) : Wrapper()
        class AccountsOverview(val viewModel: AccountsOverviewViewModel) : Wrapper()
        data object None : Wrapper()
    }
}
