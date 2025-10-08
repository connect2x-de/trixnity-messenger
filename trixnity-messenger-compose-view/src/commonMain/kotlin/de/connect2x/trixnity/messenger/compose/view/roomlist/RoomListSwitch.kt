package de.connect2x.trixnity.messenger.compose.view.roomlist

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewChat
import de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewGroup
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchGroup
import de.connect2x.trixnity.messenger.compose.view.settings.AccountsOverview
import de.connect2x.trixnity.messenger.compose.view.settings.AppInfo
import de.connect2x.trixnity.messenger.compose.view.settings.AppearanceSettings
import de.connect2x.trixnity.messenger.compose.view.settings.BlockedContactsSettings
import de.connect2x.trixnity.messenger.compose.view.settings.DevicesSettings
import de.connect2x.trixnity.messenger.compose.view.settings.NotificationsSettings
import de.connect2x.trixnity.messenger.compose.view.settings.PrivacySettings
import de.connect2x.trixnity.messenger.compose.view.settings.ProfileSettings
import de.connect2x.trixnity.messenger.compose.view.settings.UserSettings
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter

@Composable
fun RoomListSwitch(
    mainViewModel: MainViewModel,
) {
    Children(
        stack = mainViewModel.roomListRouterStack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is RoomListRouter.Wrapper.List -> RoomListContainer(child.viewModel)
            is RoomListRouter.Wrapper.CreateNewChat -> CreateNewChat(child.viewModel)
            is RoomListRouter.Wrapper.CreateNewGroup -> CreateNewGroup(child.viewModel)
            is RoomListRouter.Wrapper.SearchGroup -> SearchGroup(child.viewModel)
            is RoomListRouter.Wrapper.UserSettings -> UserSettings(child.viewModel, mainViewModel)
            is RoomListRouter.Wrapper.DevicesSettings -> DevicesSettings(child.viewModel)
            is RoomListRouter.Wrapper.Profile -> ProfileSettings(child.viewModel)
            is RoomListRouter.Wrapper.NotificationsSettings -> NotificationsSettings(child.viewModel)
            is RoomListRouter.Wrapper.PrivacySettings -> PrivacySettings(child.viewModel)
            is RoomListRouter.Wrapper.AppearanceSettings -> AppearanceSettings(child.viewModel)
            is RoomListRouter.Wrapper.BlockedContactsSettings -> BlockedContactsSettings(child.viewModel)
            is RoomListRouter.Wrapper.AppInfo -> AppInfo(child.viewModel)
            is RoomListRouter.Wrapper.AccountsOverview -> AccountsOverview(child.viewModel)
            is RoomListRouter.Wrapper.None -> Box {}
        }.let {}
    }
}
