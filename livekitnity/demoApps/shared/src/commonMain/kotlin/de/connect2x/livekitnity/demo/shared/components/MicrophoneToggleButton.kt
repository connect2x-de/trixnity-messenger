package de.connect2x.livekitnity.demo.shared.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun MicrophoneToggleButton(
    isMicrophoneEnabled: Boolean,
    canMicrophoneBeEnabled: Boolean,
    onToggleMicrophone: () -> Unit,
    modifier: Modifier = Modifier,
) {
  DeviceToggleButton(
      isEnabled = isMicrophoneEnabled,
      isToggleable = canMicrophoneBeEnabled,
      onToggle = onToggleMicrophone,
      enabledIcon = Icons.Default.Mic,
      disabledIcon = Icons.Default.MicOff,
      deviceLabel = "Microphone",
      modifier = modifier,
  )
}
