package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter

@Composable
fun RoomSettingsSwitch(
    stack: Value<ChildStack<*, SettingsRouter.Wrapper>>,
    isTwoPane: Boolean,
) {
    Children(
        stack = stack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is SettingsRouter.Wrapper.View -> RoomSettingsContainer(child.viewModel, isTwoPane)
            is SettingsRouter.Wrapper.AddMember -> AddMembersContainer(child.viewModel)
            is SettingsRouter.Wrapper.ExportRoom -> ExportRoomContainer(child.viewModel)
            is SettingsRouter.Wrapper.None -> Box {}
        }.let {}
    }
}