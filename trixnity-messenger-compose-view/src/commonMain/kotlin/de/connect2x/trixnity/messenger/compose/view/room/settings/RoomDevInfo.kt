package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.settings.CopyableUserId
import de.connect2x.trixnity.messenger.compose.view.settings.DevInfoCard
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomDevInfoViewModel


@Composable
fun RoomDevInfoContainer(roomDevInfoViewModel: RoomDevInfoViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            DevInfo(roomDevInfoViewModel)
        }
    }
}

interface RoomDevInfoView {
    @Composable
    fun create(roomDevInfoViewModel: RoomDevInfoViewModel)
}

@Composable
fun DevInfo(roomDevInfoViewModel: RoomDevInfoViewModel) {
    DI.get<RoomDevInfoView>().create(roomDevInfoViewModel)
}

class RoomDevInfoViewImpl : RoomDevInfoView {
    @Composable
    override fun create(roomDevInfoViewModel: RoomDevInfoViewModel) {
        val i18n = DI.get<I18nView>()
        val roomIdAsUserId = remember { UserId(roomDevInfoViewModel.roomId.full) }

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column{
                    Header(roomDevInfoViewModel::back, i18n.devInfo())
                    SmallSpacer()
                    Column(Modifier.padding(start = 8.dp, end = 8.dp)){
                        DevInfoCard(i18n.roomSettingsRoomId(), Icons.Default.Numbers){
                            CopyableUserId(roomIdAsUserId, MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
