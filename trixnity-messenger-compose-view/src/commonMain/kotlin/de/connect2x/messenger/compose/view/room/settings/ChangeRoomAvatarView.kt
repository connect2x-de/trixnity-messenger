package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.EditButton
import de.connect2x.messenger.compose.view.common.icons.EditIcon
import de.connect2x.messenger.compose.view.files.LoadDialog
import de.connect2x.messenger.compose.view.files.LoadFileMode
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangeRoomAvatarViewModel

interface ChangeRoomAvatarView {
    @Composable
    fun create(changeRoomAvatarViewModel: ChangeRoomAvatarViewModel)
}

@Composable
fun ChangeRoomAvatar(changeRoomAvatarViewModel: ChangeRoomAvatarViewModel) {
    DI.get<ChangeRoomAvatarView>().create(changeRoomAvatarViewModel)
}

class ChangeRoomAvatarViewImpl : ChangeRoomAvatarView {
    @Composable
    override fun create(changeRoomAvatarViewModel: ChangeRoomAvatarViewModel) {
        val canChangeAvatar = changeRoomAvatarViewModel.canChangeRoomAvatar.collectAsState().value
        val openSelector = changeRoomAvatarViewModel.openImageSelector.collectAsState().value
        val avatar = changeRoomAvatarViewModel.avatar.collectAsState().value
        val initials = changeRoomAvatarViewModel.initials.collectAsState().value
        val i18n = DI.get<I18nView>()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.Center)) {
                Avatar(avatar, initials, this@BoxWithConstraints.maxWidth.coerceAtMost(200.dp)) {
                    if (canChangeAvatar) Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            EditButton(onClick = { changeRoomAvatarViewModel.openImageSelector.value = true }) {
                                EditIcon(Icons.Default.PhotoCamera, i18n.profileAvatarChange())
                            }
                        }
                    }
                }
            }
        }
        if (openSelector) LoadDialog(
            onFileSelect = changeRoomAvatarViewModel::openAvatarCutter,
            { changeRoomAvatarViewModel.openImageSelector.value = false },
            mode = LoadFileMode.Picture,
        )
    }
}
