package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.call.livekit.CallVideoView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

@Composable
internal fun CallParticipantTile(modifier: Modifier, item: CallParticipant) {
    val i18n = DI.get<I18nView>()
    Card(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (item.video != null) {
                CallVideoView(item.video, Modifier.fillMaxSize())
            } else {
                val avatarSize = (minOf(maxWidth, maxHeight) * 0.45f).coerceAtMost(120.dp)
                CallAvatar(item.user, avatarSize)
            }
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(CallSpacing),
                horizontalArrangement = Arrangement.spacedBy(CallSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    CallBubble {
                        if (item.video != null) {
                            CallAvatar(item.user, InputChipDefaults.AvatarSize)
                        }
                        Text(
                            text = if (item.isLocal) i18n.callYou() else item.user.name,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CallBubble(Modifier.size(InputChipDefaults.Height)) {
                    Icon(
                        imageVector = if (item.microphoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun CallAvatar(user: UserInfoElement, size: Dp) {
    val image = user.image?.collectAsState()?.value
    ThemedUserAvatar(user.initials, image, size = size)
}

@Composable
private fun CallBubble(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(modifier, shape = CircleShape, color = Color.Black.copy(alpha = 0.6f), contentColor = Color.White) {
        Row(
            Modifier.heightIn(min = InputChipDefaults.Height)
                .padding(horizontal = CallSpacing, vertical = CallSpacing / 2),
            horizontalArrangement = Arrangement.spacedBy(CallSpacing / 2),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
