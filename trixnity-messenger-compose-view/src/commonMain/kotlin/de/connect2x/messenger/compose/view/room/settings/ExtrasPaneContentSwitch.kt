package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter


@Composable
fun ExtrasPaneContentSwitch(
    stack: Value<ChildStack<*, ExtrasRouter.Wrapper>>,
    isSinglePane: Boolean,
) {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is ExtrasRouter.Wrapper.RoomSettings -> RoomSettingsContainer(child.viewModel, isSinglePane)
            is ExtrasRouter.Wrapper.AddMember -> AddMembersContainer(child.viewModel)
            is ExtrasRouter.Wrapper.ExportRoom -> ExportRoomContainer(child.viewModel)
            is ExtrasRouter.Wrapper.MessageMetadata -> MessageMetadata(
                child.viewModel,
                stack.value.items.size,
                isSinglePane,
            )

            is ExtrasRouter.Wrapper.None -> Box {}
        }.let {}
    }
}
