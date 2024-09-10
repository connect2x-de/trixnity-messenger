package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.settings.AvatarCutter
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterRouter


@Composable
fun AvatarCutterSwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.avatarCutterRouterStack) {
        when (val child = it.instance) {
            is AvatarCutterRouter.Wrapper.AvatarCutter -> AvatarCutter(child.viewModel)
            is AvatarCutterRouter.Wrapper.None -> Box {}
        }
    }
}