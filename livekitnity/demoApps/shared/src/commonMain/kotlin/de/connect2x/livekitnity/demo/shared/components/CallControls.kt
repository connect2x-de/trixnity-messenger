package de.connect2x.livekitnity.demo.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.livekitnity.api.room.participant.LocalParticipant
import de.connect2x.livekitnity.demo.shared.optimistic.rememberOptimisticState
import de.connect2x.livekitnity.demo.shared.permissions.LocalMediaPermissions
import de.connect2x.livekitnity.demo.shared.permissions.MediaPermission
import de.connect2x.livekitnity.demo.shared.permissions.PermissionStatus
import de.connect2x.livekitnity.demo.shared.permissions.withPermission

@Composable
internal fun CallControls(
    localParticipant: LocalParticipant,
    modifier: Modifier = Modifier,
) {
  val mediaPermissions = LocalMediaPermissions.current

  val microphonePermissions by mediaPermissions.microphone.collectAsState()
  val cameraPermissions by mediaPermissions.camera.collectAsState()

  var isMicrophoneEnabled by
      rememberOptimisticState(
          source = localParticipant.isMicrophoneEnabled.collectAsState(),
          update = { desired ->
            if (!desired) {
              localParticipant.setMicrophoneEnabled(false)
            } else {
              mediaPermissions.withPermission(
                  permission = MediaPermission.Microphone,
                  action = {
                    localParticipant.setMicrophoneEnabled(true)
                  },
                  fallback = { status -> error("permissions denied") },
              )
            }
          },
          onError = { it.printStackTrace() },
      )

  var isCameraEnabled by
      rememberOptimisticState(
          source = localParticipant.isCameraEnabled.collectAsState(),
          update = { desired ->
            if (!desired) {
              localParticipant.setCameraEnabled(false)
            } else {
              mediaPermissions.withPermission(
                  permission = MediaPermission.Camera,
                  action = {
                    localParticipant.setCameraEnabled(true)
                  },
                  fallback = { status -> error("permissions denied") },
              )
            }
          },
          onError = { it.printStackTrace() },
      )

  var isScreenEnabled by
      rememberOptimisticState(
          source = localParticipant.isScreenShareEnabled.collectAsState(),
          update = { desired ->
            if (!desired) {
              check(!localParticipant.setScreenShareEnabled(false))
            } else {
              mediaPermissions.withPermission(
                  permission = MediaPermission.Camera,
                  action = {
                    check(localParticipant.setScreenShareEnabled(true))
                  },
                  fallback = { status -> error("permissions denied") },
              )
            }
          },
          onError = { it.printStackTrace() },
      )

  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement =
          Arrangement.spacedBy(
              space = 24.dp,
              alignment = Alignment.CenterHorizontally,
          ),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    MicrophoneToggleButton(
        isMicrophoneEnabled = isMicrophoneEnabled,
        canMicrophoneBeEnabled = microphonePermissions != PermissionStatus.Blocked,
        onToggleMicrophone = { isMicrophoneEnabled = !isMicrophoneEnabled },
    )

    ScreenToggleButton(
        isScreenEnabled = isScreenEnabled,
        canScreenBeEnabled = true,
        onToggleCamera = { isScreenEnabled = !isScreenEnabled },
    )

    CameraToggleButton(
        isCameraEnabled = isCameraEnabled,
        canCameraBeEnabled = cameraPermissions != PermissionStatus.Blocked,
        onToggleCamera = { isCameraEnabled = !isCameraEnabled },
    )
  }
}
