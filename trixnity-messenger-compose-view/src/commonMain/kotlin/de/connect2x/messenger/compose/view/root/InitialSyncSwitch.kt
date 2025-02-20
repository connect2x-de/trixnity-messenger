package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter


@Composable
fun InitialSyncSwitch(mainViewModel: MainViewModel, isSinglePane: Boolean) {
    Children(
        stack = mainViewModel.initialSyncStack,
    ) {
        when (val child = it.instance) {
            is InitialSyncRouter.Wrapper.None -> Messenger(mainViewModel, isSinglePane)
            is InitialSyncRouter.Wrapper.Sync -> SyncOverlay(child.viewModel)
            is InitialSyncRouter.Wrapper.Undefined -> Box {}
        }.let {}
    }
}
