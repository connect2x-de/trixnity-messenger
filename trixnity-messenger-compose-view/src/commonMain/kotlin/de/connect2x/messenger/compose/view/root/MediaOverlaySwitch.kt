package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.files.ImageOverlay
import de.connect2x.messenger.compose.view.files.PdfOverlay
import de.connect2x.messenger.compose.view.files.VideoOverlay
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.MediaRouter

@Composable
fun MediaOverlaySwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.mediaRouterStack) {
        when (val child = it.instance) {
            is MediaRouter.Wrapper.Image -> ImageOverlay(child.viewModel)
            is MediaRouter.Wrapper.Video -> VideoOverlay(child.viewModel)
            is MediaRouter.Wrapper.Pdf -> PdfOverlay(child.viewModel)
            // TODO Text and Markdown
            else -> Box {}
        }.let {}
    }
}

