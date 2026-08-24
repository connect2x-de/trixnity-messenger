package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.settings.UserSettings
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.roomlist.UserSettingsRoute
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.sharing.SharingRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import kotlinx.coroutines.flow.MutableStateFlow

internal class UserSettingsEntry : NavigationEntry<UserSettingsRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.left() + ThreePaneScene.left()

    @Composable
    override fun Content(route: UserSettingsRoute) {
        UserSettings(
            userSettingsViewModel = rememberComponent<UserSettingsViewModel>(),
            mainViewModel = UnsupportedMainViewModel,
        )
    }
}

private object UnsupportedMainViewModel : MainViewModel {
    override val selectedRoomId: MutableStateFlow<RoomId?>
        get() = error("unsupported")

    override val initialSyncStack: Value<ChildStack<InitialSyncRouter.Config, InitialSyncRouter.Wrapper>>
        get() = error("unsupported")

    override val selfVerificationStack: Value<ChildStack<SelfVerificationRouter.Config, SelfVerificationRouter.Wrapper>>
        get() = error("unsupported")

    override val roomListRouterStack: Value<ChildStack<RoomListRouter.Config, RoomListRouter.Wrapper>>
        get() = error("unsupported")

    override val roomRouterStack: Value<ChildStack<RoomRouter.Config, RoomRouter.Wrapper>>
        get() = error("unsupported")

    override val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>>
        get() = error("unsupported")

    override val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.Wrapper>>
        get() = error("unsupported")

    override val accountSetupRouterStack: Value<ChildStack<AccountSetupRouter.Config, AccountSetupRouter.Wrapper>>
        get() = error("unsupported")

    override val sharingStack: Value<ChildStack<SharingRouter.Config, SharingRouter.Wrapper>>
        get() = error("unsupported")

    override fun start() {}

    override fun closeDetailsAndShowList() {}

    override fun onRoomSelected(userId: UserId, id: RoomId, via: Set<String>?) {}

    override fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor) {}

    override fun onOpenAvatarCutter(userId: UserId, selectedRoomId: RoomId, file: FileDescriptor) {}

    override fun openSelfVerification(userId: UserId) {}

    override fun openMention(userId: UserId, timelineElementMention: TimelineElementMention) {}
}
