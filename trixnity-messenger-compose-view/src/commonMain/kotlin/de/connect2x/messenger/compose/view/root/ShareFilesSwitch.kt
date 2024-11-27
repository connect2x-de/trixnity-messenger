package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.messenger.compose.view.files.ShareFiles
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.sharing.SharingRouter

@Composable
fun ShareFilesSwitch(mainViewModel: MainViewModel) {
    Children(stack = mainViewModel.sharingStack) {
        when (val child = it.instance) {
            is SharingRouter.Wrapper.ShareFiles -> ShareFiles(child.viewModel)
            is SharingRouter.Wrapper.None -> Box {}
        }
    }
}
