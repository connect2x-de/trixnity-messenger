package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScenePlaceholder
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter

@Composable
fun RoomSwitch(stack: Value<ChildStack<*, RoomRouter.Wrapper>>) {
    Children(stack = stack, animation = stackAnimation(fade())) {
        when (val child = it.instance) {
            is RoomRouter.Wrapper.View -> Room(child.viewModel)
            is RoomRouter.Wrapper.JoinRoomAction -> JoinRoomAction(child.viewModel)
            is RoomRouter.Wrapper.None -> TwoPaneScenePlaceholder()
        }.let {}
    }
}
