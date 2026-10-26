package de.connect2x.livekitnity.demo.shared.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun ScreenToggleButton(
    isScreenEnabled: Boolean,
    canScreenBeEnabled: Boolean,
    onToggleCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
  DeviceToggleButton(
      isEnabled = isScreenEnabled,
      isToggleable = canScreenBeEnabled,
      onToggle = onToggleCamera,
      enabledIcon = Icons.AutoMirrored.Filled.ScreenShare,
      disabledIcon = Icons.AutoMirrored.Filled.StopScreenShare,
      deviceLabel = "Screen",
      modifier = modifier,
  )
}
