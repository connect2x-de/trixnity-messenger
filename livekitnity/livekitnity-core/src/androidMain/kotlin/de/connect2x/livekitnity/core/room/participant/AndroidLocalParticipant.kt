package de.connect2x.livekitnity.core.room.participant

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import de.connect2x.livekitnity.api.room.participant.AndroidLocalParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidParticipant
import de.connect2x.livekitnity.api.room.track.AndroidLocalTrackPublication
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

internal fun AndroidLocalParticipant(
    delegate: io.livekit.android.room.participant.LocalParticipant,
    converter: Converter,
    context: Context,
): AndroidLocalParticipant {
  return AndroidLocalParticipantImpl(delegate, AndroidParticipant(delegate, converter), context)
}

private class AndroidLocalParticipantImpl(
    private val delegate: io.livekit.android.room.participant.LocalParticipant,
    private val base: AndroidParticipant,
    private val context: Context,
) : AndroidLocalParticipant, AndroidParticipant by base {

  override val trackPublications: StateFlow<Map<String, AndroidLocalTrackPublication>> =
      base.trackPublications.mapState {
        it.mapValues { (_, trackPublication) -> trackPublication as AndroidLocalTrackPublication }
      }

  override suspend fun setCameraEnabled(enabled: Boolean): Boolean {
    return delegate.setCameraEnabled(enabled = enabled)
  }

  override suspend fun setMicrophoneEnabled(enabled: Boolean): Boolean {
    return delegate.setMicrophoneEnabled(enabled = enabled)
  }

  // TODO(fhilgers): This has to be reworked and is slop
  override suspend fun setScreenShareEnabled(enabled: Boolean): Boolean {
    if (!enabled) {
      delegate.setScreenShareEnabled(false)
      return false
    }

    val activity = context as? ComponentActivity ?: return false

    val permissionResultIntent = requestMediaProjectionToken(activity) ?: return false

    return delegate.setScreenShareEnabled(
        enabled = true,
        screenCaptureParams =
            ScreenCaptureParams(
                mediaProjectionPermissionResultData = permissionResultIntent,
                notificationId = NOTIFICATION_ID,
                notification = createNotification(activity),
                onStop = {},
            ),
    )
  }

  private suspend fun requestMediaProjectionToken(activity: ComponentActivity): Intent? =
      suspendCancellableCoroutine { continuation ->
        val key = "media_proj_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<Intent>? = null

        launcher =
            activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
              launcher?.unregister()
              if (continuation.isActive) {
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                  continuation.resume(result.data)
                } else {
                  continuation.resume(null)
                }
              }
            }

        try {
          val projectionManager =
              activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
          launcher.launch(projectionManager.createScreenCaptureIntent())
        } catch (e: Exception) {
          launcher.unregister()
          if (continuation.isActive) {
            continuation.resume(null)
          }
        }

        continuation.invokeOnCancellation {
          launcher.unregister()
        }
      }

  private fun createNotification(activity: ComponentActivity): Notification {
    val channelId = "livekit_screen_share_channel"
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(
                  channelId,
                  "Screen Sharing",
                  NotificationManager.IMPORTANCE_LOW,
              )
              .apply {
                description = "Used while screen capture is actively streaming"
              }
      notificationManager.createNotificationChannel(channel)
    }

    return NotificationCompat.Builder(activity, channelId)
        .setContentTitle("Screen Sharing")
        .setContentText("Sharing screen with LiveKit")
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
  }

  private companion object {
    const val NOTIFICATION_ID = 1001
  }
}
