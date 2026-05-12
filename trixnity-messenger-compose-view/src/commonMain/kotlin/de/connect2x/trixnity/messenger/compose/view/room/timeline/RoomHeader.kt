package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.HeaderSurface
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.UserState
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.icons.PublicIcon
import de.connect2x.trixnity.messenger.compose.view.common.icons.UnencryptedIcon
import de.connect2x.trixnity.messenger.compose.view.common.modifier.minHeaderHeight
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedLabel
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderInfo
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModel

interface RoomHeaderView {
    @Composable
    fun create(roomHeaderViewModel: RoomHeaderViewModel, showSettingsButton: Boolean, showBackButton: Boolean)
}

@Composable
fun RoomHeader(
    roomHeaderViewModel: RoomHeaderViewModel,
    showSettingsButton: Boolean,
    showBackButton: Boolean,
) {
    with(DI.get<RoomHeaderView>()) { create(roomHeaderViewModel, showSettingsButton, showBackButton) }
}

class RoomHeaderViewImpl : RoomHeaderView {
    @Composable
    override fun create(
        roomHeaderViewModel: RoomHeaderViewModel,
        showSettingsButton: Boolean,
        showBackButton: Boolean,
    ) {
        val roomHeaderElement = roomHeaderViewModel.roomHeaderInfo.collectAsState().value
        val usersTyping = roomHeaderViewModel.usersTyping.collectAsState().value
        val knockingMembersCount = roomHeaderViewModel.knockingMembersCount.collectAsState().value
        val i18n = DI.get<I18nView>()

        HeaderSurface {
            Column {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.minHeaderHeight()
                ) {
                    if (showBackButton) {
                        Spacer(Modifier.size(8.dp))
                        RoomBackButton(roomHeaderViewModel)
                    }
                    Row(
                        Modifier
                            .padding(vertical = 4.dp)
                            .align(Alignment.CenterVertically)
                            .fillMaxWidth()
                            .weight(1f, true),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.size(8.dp))

                        ThemedButton(
                            style = MaterialTheme.components.accountSelector,
                            onClick = { roomHeaderViewModel.openRoomSettings() },
                            modifier = Modifier.semantics(mergeDescendants = true) {

                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.semantics {
                                    text = AnnotatedString(roomHeaderElement.roomName)
                                    role = Role.Button
                                }
                            ) {
                                Box {
                                    ThemedUserAvatar(
                                        initials = roomHeaderElement.roomImageInitials,
                                        image = roomHeaderElement.roomImage,
                                        presence = roomHeaderElement.presence,
                                    ) {
                                        AvatarPresenceBadge(roomHeaderElement.presence)
                                    }
                                    if (roomHeaderElement.isPublic) {
                                        PublicIcon()
                                    }
                                }
                                VerySmallSpacer()
                                UserState(roomHeaderViewModel.userTrustLevel, roomHeaderViewModel.isUserBlocked)
                                VerySmallSpacer()
                                if (roomHeaderElement.isEncrypted.not()) {
                                    UnencryptedIcon()
                                    VerySmallSpacer()
                                }

                                if (knockingMembersCount > 0) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.commonIconButton,
                                        onClick = { roomHeaderViewModel.openRoomSettings() }
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text("$knockingMembersCount")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.DoorFront,
                                                i18n.roomHeaderKnockingUsersCount(knockingMembersCount),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.size(5.dp))
                                }

                                Column(modifier = Modifier.padding(end = 14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RoomName(roomHeaderElement)
                                        Spacer(Modifier.size(7.dp))
                                        if (roomHeaderElement.isLeave) {
                                            ThemedLabel(i18n.commonArchived())
                                        }
                                    }
                                    if (usersTyping != null) {
                                        UsersTyping(usersTyping)
                                    } else {
                                        RoomTopic(roomHeaderElement)
                                    }
                                }
                            }
                        }
                    }

                    RoomExtras(roomHeaderViewModel, showSettingsButton)
                    Spacer(Modifier.size(8.dp))
                }

                HorizontalDivider(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun RowScope.RoomBackButton(roomHeaderViewModel: RoomHeaderViewModel) {
    val i18n = DI.get<I18nView>()
    Tooltip(
        tooltip = { Text(i18n.commonBack()) },
    ) {
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            onClick = { roomHeaderViewModel.back() },
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = i18n.roomHeaderClose()
            }
        ) {
            Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = null)
        }
    }
}

@Composable
fun RoomName(
    roomHeaderElement: RoomHeaderInfo,
) {
    Tooltip({ Text(roomHeaderElement.roomName) }) {
        Text(
            roomHeaderElement.roomName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
fun UsersTyping(usersTyping: String) {
    Text(
        text = usersTyping,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        maxLines = 1,
    )
}

@Composable
fun ColumnScope.RoomTopic(roomHeaderElement: RoomHeaderInfo) {
    val topic = roomHeaderElement.roomTopic
    if (topic.isNotBlank()) {
        Tooltip(tooltip = {
            Text(topic)
        }) {
            Text(
                topic,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun RoomExtras(
    roomHeaderViewModel: RoomHeaderViewModel,
    showSettingsButton: Boolean,
) {
    val i18n = DI.get<I18nView>()

    if (showSettingsButton) {
        Tooltip(
            tooltip = { Text(i18n.roomHeaderSettings()) }
        ) {
            ThemedIconButton(
                style = MaterialTheme.components.commonIconButton,
                onClick = { roomHeaderViewModel.openRoomSettings() },
            ) {
                Icon(Icons.Default.Settings, i18n.roomHeaderSettings())
            }
        }
    }
}
