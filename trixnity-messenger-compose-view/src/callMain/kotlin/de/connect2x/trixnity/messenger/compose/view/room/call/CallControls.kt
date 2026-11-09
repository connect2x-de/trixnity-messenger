package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton

@Composable
internal fun CallControls(microphone: CallMediaControl, camera: CallMediaControl) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        MediaControlButton(microphone, activeIcon = Icons.Default.Mic, inactiveIcon = Icons.Default.MicOff)
        MediaControlButton(camera, activeIcon = Icons.Default.Videocam, inactiveIcon = Icons.Default.VideocamOff)
    }
}

@Composable
private fun MediaControlButton(control: CallMediaControl, activeIcon: ImageVector, inactiveIcon: ImageVector) {
    ThemedIconButton(
        style = MaterialTheme.components.commonIconButton,
        enabled = control.enabled,
        onClick = control.onToggle,
    ) {
        Icon(if (control.active) activeIcon else inactiveIcon, contentDescription = null)
    }
}
