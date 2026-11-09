package de.connect2x.livekitnity.demo.shared.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun DeviceToggleButton(
    isEnabled: Boolean,
    isToggleable: Boolean,
    onToggle: () -> Unit,
    enabledIcon: ImageVector,
    disabledIcon: ImageVector,
    deviceLabel: String,
    modifier: Modifier = Modifier,
) {
  val tooltipState = rememberTooltipState()
  val tooltipText = if (isEnabled) "Disable $deviceLabel" else "Enable $deviceLabel"

  TooltipBox(
      positionProvider =
          rememberTooltipPositionProvider(
              TooltipAnchorPosition.Above,
          ),
      tooltip = {
        PlainTooltip {
          Text(tooltipText)
        }
      },
      state = tooltipState,
      modifier = modifier,
  ) {
    IconButton(
        onClick = onToggle,
        enabled = isToggleable,
    ) {
      Icon(
          imageVector = if (isEnabled) enabledIcon else disabledIcon,
          contentDescription = tooltipText,
      )
    }
  }
}
