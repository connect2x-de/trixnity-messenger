package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel

@TrixnityMessengerCallApi
@Composable
internal fun RoomCall(viewModel: RoomCallViewModel) {
    val currentState by viewModel.state.collectAsState()
    val state = currentState ?: return
    val roomInfo = state.roomInfo
    val size = Modifier.fillMaxSize()
    ThemedSurface(modifier = size, style = MaterialTheme.components.timeline) {
        Column(size) {
            Header(
                onBack = viewModel::back,
                title = {
                    if (roomInfo != null) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ThemedUserAvatar(roomInfo.roomImageInitials, roomInfo.roomImage)
                            Text(
                                roomInfo.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                    }
                },
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                LiveKitCall(
                    state,
                    onLeave = viewModel::leave,
                    onMicrophone = viewModel::toggleMicrophone,
                    onCamera = viewModel::toggleCamera,
                )
            }
        }
    }
}
