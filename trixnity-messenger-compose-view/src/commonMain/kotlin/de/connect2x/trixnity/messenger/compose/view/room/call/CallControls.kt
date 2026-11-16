package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton

@TrixnityMessengerCallApi
internal data class CallMediaControl(val active: Boolean, val enabled: Boolean, val onToggle: () -> Unit = {})

@Composable
@TrixnityMessengerCallApi
internal fun CallControls(
    microphone: CallMediaControl?,
    camera: CallMediaControl?,
    onLeave: () -> Unit,
    onMicrophone: () -> Unit,
    onCamera: () -> Unit,
    onBackground: (() -> Unit)? = null,
) {
    val i18n = DI.get<I18nView>()
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBackground != null) {
                ThemedIconButton(onClick = onBackground) { Icon(Icons.AutoMirrored.Filled.Chat, i18n.callBackground()) }
            }
            MediaControlButton(
                control = microphone,
                activeIcon = Icons.Default.Mic,
                inactiveIcon = Icons.Default.MicOff,
                activeDescription = i18n.callMute(),
                inactiveDescription = i18n.callUnmute(),
                onToggle = onMicrophone,
            )
            MediaControlButton(
                control = camera,
                activeIcon = Icons.Default.Videocam,
                inactiveIcon = Icons.Default.VideocamOff,
                activeDescription = i18n.callCameraOff(),
                inactiveDescription = i18n.callCameraOn(),
                onToggle = onCamera,
            )
            LeaveCallButton(onLeave, i18n.callLeave())
        }
    }
}

@Composable
@TrixnityMessengerCallApi
internal fun CompactCallBar(
    roomName: String,
    status: String?,
    microphone: CallMediaControl?,
    onMicrophone: () -> Unit,
    onRestore: () -> Unit,
    onLeave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(onClickLabel = i18n.callRestore(), onClick = onRestore)
                .heightIn(min = 64.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaControlButton(
                control = microphone,
                activeIcon = Icons.Default.Mic,
                inactiveIcon = Icons.Default.MicOff,
                activeDescription = i18n.callMute(),
                inactiveDescription = i18n.callUnmute(),
                onToggle = onMicrophone,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    roomName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (status != null) {
                    Text(
                        status,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            LeaveCallButton(onLeave, i18n.callLeave())
        }
    }
}

@Composable
@TrixnityMessengerCallApi
private fun LeaveCallButton(onLeave: () -> Unit, description: String) {
    ThemedIconButton(onClick = onLeave) {
        Icon(Icons.Default.CallEnd, description, tint = MaterialTheme.colorScheme.error)
    }
}

@Composable
@TrixnityMessengerCallApi
private fun MediaControlButton(
    control: CallMediaControl?,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    activeDescription: String,
    inactiveDescription: String,
    onToggle: () -> Unit,
) {
    val active = control?.active == true
    ThemedIconButton(enabled = control?.enabled == true, onClick = onToggle) {
        Icon(if (active) activeIcon else inactiveIcon, if (active) activeDescription else inactiveDescription)
    }
}
