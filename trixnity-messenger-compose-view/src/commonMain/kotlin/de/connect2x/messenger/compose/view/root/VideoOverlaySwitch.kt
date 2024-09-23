package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.files.VideoOverlay
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.files.VideoRouter

@Composable
fun VideoOverlaySwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.videoRouterStack) {
        when (val child = it.instance) {
            is VideoRouter.Wrapper.Video -> VideoOverlay(child.viewModel)
            is VideoRouter.Wrapper.None -> Box {}
        }
    }
}