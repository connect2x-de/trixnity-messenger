package de.connect2x.trixnity.messenger.compose.view.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Box(Modifier.fillMaxSize().background(Color.Blue)) {

        }
    }
}
