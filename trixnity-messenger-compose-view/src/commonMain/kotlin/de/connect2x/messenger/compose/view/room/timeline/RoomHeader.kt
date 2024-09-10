package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.AvatarWithPresence
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.UserState
import de.connect2x.messenger.compose.view.common.icons.PublicIcon
import de.connect2x.messenger.compose.view.common.icons.UnencryptedIcon
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderInfo
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModel

interface RoomHeaderView {
    @Composable
    fun create(roomHeaderViewModel: RoomHeaderViewModel, showSettingsButton: Boolean)
}

@Composable
fun RoomHeader(roomHeaderViewModel: RoomHeaderViewModel, showSettingsButton: Boolean = true) {
    with(DI.current.get<RoomHeaderView>()) { create(roomHeaderViewModel, showSettingsButton) }
}

class RoomHeaderViewImpl : RoomHeaderView {
    @Composable
    override fun create(roomHeaderViewModel: RoomHeaderViewModel, showSettingsButton: Boolean) {
        val roomHeaderElement = roomHeaderViewModel.roomHeaderInfo.collectAsState().value
        val isBackButtonVisible = roomHeaderViewModel.isBackButtonVisible.collectAsState().value
        val isTyping = roomHeaderViewModel.usersTyping.collectAsState().value

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    if (isBackButtonVisible) {
                        RoomBackButton(roomHeaderViewModel)
                    }
                    Row(
                        Modifier
                            .padding(vertical = 4.dp, horizontal = if (isBackButtonVisible) 0.dp else 10.dp)
                            .align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            AvatarWithPresence(
                                roomHeaderElement.roomImage,
                                roomHeaderElement.roomImageInitials,
                                roomHeaderElement.presence,
                            )
                            if (roomHeaderElement.isPublic) {
                                PublicIcon()
                            }
                        }
                        Spacer(Modifier.size(5.dp))
                        UserState(roomHeaderViewModel.userTrustLevel, roomHeaderViewModel.isUserBlocked)
                        if (roomHeaderElement.isEncrypted.not()) {
                            UnencryptedIcon()
                            Spacer(Modifier.size(5.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                RoomName(roomHeaderElement, isTyping)
                                RoomTopic(roomHeaderElement)
                            }
                            RoomExtras(roomHeaderViewModel, showSettingsButton)
                        }
                    }
                }
                HorizontalDivider(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun RowScope.RoomBackButton(roomHeaderViewModel: RoomHeaderViewModel) {
    val i18n = DI.current.get<I18nView>()
    IconButton(
        onClick = { roomHeaderViewModel.goBack() },
        modifier = Modifier.align(Alignment.CenterVertically).buttonPointerModifier()
    ) {
        Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, i18n.commonBack())
    }

}

@Composable
fun ColumnScope.RoomName(
    roomHeaderElement: RoomHeaderInfo,
    isTyping: String?
) {
    Tooltip({
        TooltipText { roomHeaderElement.roomName }
    }) {
        Text(
            roomHeaderElement.roomName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )

    }
    isTyping?.let {
        Text(
            text = it,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

@Composable
fun ColumnScope.RoomTopic(roomHeaderElement: RoomHeaderInfo) {
    val topic = roomHeaderElement.roomTopic
    if (topic.isNotBlank()) Tooltip(tooltip = {
        TooltipText(topic)
    }) {
        Text(
            topic,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Light,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
fun RoomExtras(
    roomHeaderViewModel: RoomHeaderViewModel,
    showSettingsButton: Boolean = true,
) {
    val contextMenuOpen = remember { mutableStateOf(false) }
    val isMobile = Platform.current.isMobile
    val i18n = DI.current.get<I18nView>()
    when {
        isMobile -> {
            if (showSettingsButton) IconButton(
                onClick = { roomHeaderViewModel.showRoomSettings() },
                Modifier.wrapContentSize()
            ) {
                Icon(Icons.Default.Settings, i18n.roomHeaderSettings())
            }
            Box {
                IconButton(onClick = {
                    contextMenuOpen.value = contextMenuOpen.value.not()
                }) {
                    Icon(Icons.Default.MoreVert, i18n.roomHeaderMore())
                }
                RoomContextMenu(contextMenuOpen, roomHeaderViewModel)
            }
        }

        else -> {
            if (showSettingsButton) IconButton(
                onClick = { roomHeaderViewModel.showRoomSettings() },
                Modifier.buttonPointerModifier().then(
                    Modifier.wrapContentSize(unbounded = true)
                )
            ) {
                Icon(Icons.Default.Settings, i18n.roomHeaderSettings())
            }
            Box {
                IconButton(
                    onClick = { contextMenuOpen.value = contextMenuOpen.value.not() },
                    Modifier.buttonPointerModifier().then(
                        Modifier.wrapContentSize(unbounded = true)
                    )
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, i18n.roomHeaderMore())
                }
                RoomContextMenu(contextMenuOpen, roomHeaderViewModel)
            }
        }
    }
}

@Composable
fun RoomContextMenu(
    contextMenuOpen: MutableState<Boolean>,
    roomHeaderViewModel: RoomHeaderViewModel
) {
    val i18n = DI.current.get<I18nView>()
    val canVerifyUser = roomHeaderViewModel.canVerifyUser.collectAsState().value
    val canBlockUser = roomHeaderViewModel.canBlockUser.collectAsState().value
    val canUnblockUser = roomHeaderViewModel.canUnblockUser.collectAsState().value

    DropdownMenu(
        expanded = contextMenuOpen.value,
        onDismissRequest = { contextMenuOpen.value = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    i18n.roomHeaderStartUserVerification(),
                    Modifier.buttonPointerModifier(canVerifyUser),
                    color = textColor(canVerifyUser),
                )
            },
            onClick = {
                contextMenuOpen.value = false
                roomHeaderViewModel.verifyUser()
            },
            contentPadding = PaddingValues(horizontal = 10.dp),
            enabled = canVerifyUser,
        )
        if (canBlockUser) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.roomHeaderBlockUser(),
                        Modifier.buttonPointerModifier(canBlockUser),
                        color = textColor(canBlockUser),
                    )
                },
                onClick = {
                    contextMenuOpen.value = false
                    roomHeaderViewModel.blockUser()
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
                enabled = canBlockUser,
            )
        }
        if (canUnblockUser) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.roomHeaderUnblockUser(),
                        Modifier.buttonPointerModifier(canUnblockUser),
                        color = textColor(canUnblockUser),
                    )
                },
                onClick = {
                    contextMenuOpen.value = false
                    roomHeaderViewModel.unblockUser()
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
                enabled = canUnblockUser,
            )
        }
    }
}

@Composable
private fun textColor(enabled: Boolean) =
    if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
