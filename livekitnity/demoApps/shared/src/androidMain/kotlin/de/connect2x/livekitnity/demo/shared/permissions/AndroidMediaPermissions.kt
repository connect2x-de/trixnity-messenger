package de.connect2x.livekitnity.demo.shared.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

public class AndroidMediaPermissions(
    private val activity: ComponentActivity,
) : MediaPermissions, DefaultLifecycleObserver {

  private val cameraState = MutableStateFlow(PermissionStatus.Unknown)
  private val microphoneState = MutableStateFlow(PermissionStatus.Unknown)

  override val camera: StateFlow<PermissionStatus> = cameraState.asStateFlow()
  override val microphone: StateFlow<PermissionStatus> = microphoneState.asStateFlow()

  private val requestMutex = Mutex()
  private var pending: Pending? = null
  private var destroyed = false

  // Inferences from request results during this activity's lifetime.
  private val inferredBlocked = mutableSetOf<MediaPermission>()

  private data class Pending(
      val permission: MediaPermission,
      val result: CompletableDeferred<PermissionStatus>,
  )

  private val launcher =
      activity.registerForActivityResult(
          ActivityResultContracts.RequestPermission(),
      ) { granted ->
        val request = pending
        pending = null

        if (request != null) {
          val permission = request.permission

          val status =
              when {
                granted -> {
                  inferredBlocked.remove(permission)
                  PermissionStatus.Granted
                }

                shouldShowRationale(permission) -> {
                  inferredBlocked.remove(permission)
                  PermissionStatus.Promptable
                }

                else -> {
                  inferredBlocked.add(permission)
                  PermissionStatus.Blocked
                }
              }

          state(permission).value = status
          request.result.complete(status)
        }
      }

  init {
    activity.lifecycle.addObserver(this)
    refresh()
  }

  override fun onResume(owner: LifecycleOwner) {
    refresh()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    destroyed = true
    pending
        ?.result
        ?.cancel(
            CancellationException("Activity destroyed"),
        )
    pending = null
    launcher.unregister()
    owner.lifecycle.removeObserver(this)
  }

  private fun refresh() {
    MediaPermission.entries.forEach { permission ->
      state(permission).value = read(permission)
    }
  }

  override suspend fun request(
      permission: MediaPermission,
  ): PermissionStatus =
      withContext(Dispatchers.Main.immediate) {
        requestMutex.withLock {
          check(!destroyed) { "Activity already destroyed" }

          // A cancelled caller does not dismiss the system dialog.
          // Wait for that dialog's result before launching another.
          pending?.result?.await()

          val current = read(permission)
          state(permission).value = current

          if (current == PermissionStatus.Granted || current == PermissionStatus.Blocked) {
            return@withLock current
          }

          val result = CompletableDeferred<PermissionStatus>()
          pending = Pending(permission, result)

          try {
            launcher.launch(permission.androidName)
          } catch (error: Exception) {
            pending = null
            throw error
          }

          result.await()
        }
      }

  private fun read(permission: MediaPermission): PermissionStatus =
      when {
        ContextCompat.checkSelfPermission(
            activity,
            permission.androidName,
        ) == PackageManager.PERMISSION_GRANTED -> {
          inferredBlocked.remove(permission)
          PermissionStatus.Granted
        }

        shouldShowRationale(permission) -> {
          inferredBlocked.remove(permission)
          PermissionStatus.Promptable
        }

        permission in inferredBlocked -> PermissionStatus.Blocked

        // Could be first request, a reset, or a pre-existing block.
        else -> PermissionStatus.Unknown
      }

  private fun shouldShowRationale(permission: MediaPermission): Boolean =
      ActivityCompat.shouldShowRequestPermissionRationale(
          activity,
          permission.androidName,
      )

  private fun state(permission: MediaPermission) =
      when (permission) {
        MediaPermission.Camera -> cameraState
        MediaPermission.Microphone -> microphoneState
      }

  private val MediaPermission.androidName: String
    get() =
        when (this) {
          MediaPermission.Camera -> Manifest.permission.CAMERA
          MediaPermission.Microphone -> Manifest.permission.RECORD_AUDIO
        }
}
