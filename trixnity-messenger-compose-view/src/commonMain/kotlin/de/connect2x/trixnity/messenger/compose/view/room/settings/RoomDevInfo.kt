package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.CopyToClipboardButton
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.settings.CopyableUserId
import de.connect2x.trixnity.messenger.compose.view.settings.DevInfoCard
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText
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
        val scrollState = rememberScrollState()
        Column(Modifier.fillMaxSize()) {
            Header(roomDevInfoViewModel::back, i18n.devInfo())
            SmallSpacer()
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 8.dp, end = 8.dp).verticalScroll(scrollState)) {
                    DevInfoCard(
                        i18n.roomSettingsRoomId(),
                        Icons.Default.Numbers,
                        additionalButtons = {
                            CopyToClipboardButton(
                                roomDevInfoViewModel.roomId.full,
                                i18n.copyToClipboardButton()
                            )
                        }) {
                        ThemedSelectableText(
                            roomDevInfoViewModel.roomId.full,
                            MaterialTheme.components.selectionOnSurface
                        )
                    }
                }
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scrollState)
            }
        }
    }
}
