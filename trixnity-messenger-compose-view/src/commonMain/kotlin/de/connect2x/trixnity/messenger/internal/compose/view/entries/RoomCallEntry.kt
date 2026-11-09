package de.connect2x.trixnity.messenger.internal.compose.view.entries

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.room.call.LiveKitCall
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel

@OptIn(TrixnityMessengerCallApi::class)
internal class RoomCallEntry : NavigationEntry<RoomCallRoute> {
    @Composable
    override fun Content(route: RoomCallRoute) {
        val viewModel = rememberComponent<RoomCallViewModel>()
        val roomInfo = viewModel.roomInfo.collectAsState().value
        ThemedSurface(modifier = Modifier.fillMaxSize(), style = MaterialTheme.components.timeline) {
            Column(Modifier.fillMaxSize()) {
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
                Box(Modifier.fillMaxWidth().weight(1f)) { LiveKitCall(viewModel) }
            }
        }
    }
}
