package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter


@Composable
fun RoomSwitch(
    stack: Value<ChildStack<*, RoomRouter.Wrapper>>,
) {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is RoomRouter.Wrapper.View -> Room(child.viewModel)
            is RoomRouter.Wrapper.JoinRoomConfirm -> JoinRoomAction(child.viewModel)
            is RoomRouter.Wrapper.None -> Box {} // TODO: Would be nice to show a placeholder here.
        }.let {}
    }
}
