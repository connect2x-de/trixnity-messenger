package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.settings.DevInfoCard
import de.connect2x.trixnity.messenger.viewmodel.room.settings.DevInfoViewModel
import net.folivo.trixnity.core.model.UserId


@Composable
fun DevInfoContainer(devInfoViewModel: DevInfoViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            DevInfo(devInfoViewModel)
        }
    }
}

interface DevInfoView {
    @Composable
    fun create(devInfoViewModel: DevInfoViewModel)
}

@Composable
fun DevInfo(devInfoViewModel: DevInfoViewModel) {
    DI.get<DevInfoView>().create(devInfoViewModel)
}

class DevInfoViewImpl : DevInfoView {
    @Composable
    override fun create(devInfoViewModel: DevInfoViewModel) {
        val i18n = DI.get<I18nView>()
        val roomIdAsUserId = remember { UserId(devInfoViewModel.roomId.full) }

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column{
                    Header(devInfoViewModel::back, i18n.devInfo())
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
