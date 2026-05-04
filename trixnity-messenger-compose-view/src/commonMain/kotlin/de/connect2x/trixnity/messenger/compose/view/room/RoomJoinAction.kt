package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomConfirmViewModel

interface JoinRoomActionView {
    @Composable
    fun create(viewModel: JoinRoomConfirmViewModel)
}

@Composable
fun JoinRoomAction(viewModel: JoinRoomConfirmViewModel) {
    DI.current.get<JoinRoomActionView>().create(viewModel)
}

class JoinRoomActionViewImpl : JoinRoomActionView {
    @Composable
    override fun create(viewModel: JoinRoomConfirmViewModel) {
        val action = viewModel.actionNecessary.collectAsState().value
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                when (action) {
                    JoinRoomConfirmViewModel.JoinRoomAction.Impossible -> {
                        Text("Can't join room")
                    }

                    JoinRoomConfirmViewModel.JoinRoomAction.Join -> Text("Please join room")
                    JoinRoomConfirmViewModel.JoinRoomAction.Knock -> Text("Please knock")
                    is JoinRoomConfirmViewModel.JoinRoomAction.Restricted -> Text("Please join rooms")
                    else -> {}
                }
            }
        }
    }
}
