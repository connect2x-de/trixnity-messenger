package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.files.ImageOverlay
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.files.ImageRouter

@Composable
fun ImageOverlaySwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.imageRouterStack) {
        when (val child = it.instance) {
            is ImageRouter.Wrapper.Image -> ImageOverlay(child.viewModel)
            is ImageRouter.Wrapper.None -> Box {}
        }.let {}
    }
}