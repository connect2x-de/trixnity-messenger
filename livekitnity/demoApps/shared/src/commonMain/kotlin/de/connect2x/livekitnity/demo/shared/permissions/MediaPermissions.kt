package de.connect2x.livekitnity.demo.shared.permissions

import kotlinx.coroutines.flow.StateFlow

// TODO(fhilgers): I like the public API but both impls for the demo is currently slop

public interface MediaPermissions {
  public val camera: StateFlow<PermissionStatus>
  public val microphone: StateFlow<PermissionStatus>

  public suspend fun request(permission: MediaPermission): PermissionStatus
}

public suspend fun MediaPermissions.withPermission(
    permission: MediaPermission,
    action: suspend () -> Unit,
    fallback: suspend (PermissionStatus) -> Unit,
) {
  val current =
      when (permission) {
        MediaPermission.Camera -> camera.value
        MediaPermission.Microphone -> microphone.value
      }

  val status =
      when (current) {
        PermissionStatus.Unknown,
        PermissionStatus.Promptable -> request(permission)

        PermissionStatus.Granted,
        PermissionStatus.Blocked -> current
      }

  if (status == PermissionStatus.Granted) {
    action()
  } else {
    fallback(status)
  }
}
