package de.connect2x.livekitnity.demo.shared.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun CameraToggleButton(
    isCameraEnabled: Boolean,
    canCameraBeEnabled: Boolean,
    onToggleCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
  DeviceToggleButton(
      isEnabled = isCameraEnabled,
      isToggleable = canCameraBeEnabled,
      onToggle = onToggleCamera,
      enabledIcon = Icons.Default.Videocam,
      disabledIcon = Icons.Default.VideocamOff,
      deviceLabel = "Camera",
      modifier = modifier,
  )
}
