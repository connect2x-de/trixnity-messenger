package de.connect2x.trixnity.messenger.services

import android.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.NotificationPriority
import de.connect2x.sysnotify.create
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandle
import de.connect2x.sysnotify.notification
import de.connect2x.sysnotify.update
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.DateFormat
import java.text.SimpleDateFormat

private val log = KotlinLogging.logger { }

class AudioPlayerService : Service() {
    private val audioPlayer: ExoPlayer = ExoPlayer.Builder(this).build()
    private val binder = ServiceBinder()
    private val notificationHandler: NotificationHandler by lazy {
        NotificationHandler(
            id = NOTIFICATION_CHANNEL,
            name = "Audio Player",
            priority = NotificationPriority.LOW,
            appId = "de.connect2x.trixnity.messenger" // TODO
        )
    }

    private val _position: MutableStateFlow<Long> = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration: MutableStateFlow<Long> = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private var notificationHandle: NotificationHandle? = null

    override fun onCreate() {
        log.debug { "Creating notification channel for audio player service" }
        audioPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (duration.value == 0L && position.value != 0L) {
                    if (audioPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) {
                        audioPlayer.seekTo(position.value)
                        updateNotification() // TODO: Is this really required
                    }
                }

                if (audioPlayer.playbackState == Player.STATE_READY) {
                    _position.value = audioPlayer.currentPosition.coerceAtLeast(0)
                    _duration.value = audioPlayer.duration.coerceAtLeast(0)
                    // TODO: Update progress bar
                    updateNotification()
                }

                if (!isPlaying) {
                    if (audioPlayer.currentPosition >= audioPlayer.duration) {
                        _position.value = 0
                    }

                    stopSelf()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                log.error(error) { "Unexpected error while playing audio" }
                stopSelf()
            }
        })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        log.debug { "Received command" } // TODO
        when (intent.action) {
            START_ACTION -> {
                // TODO: Replace with getParcelableExtra(<name>, <class>) when min API level is >= 33
                val uri = @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(START_AUDIO_URI) ?: run {
                    log.error { "Unable to play audio file: Start player command doesn't contain URI of resource" }
                    return START_STICKY
                }

                log.debug { "Received command for playing audio '$uri'. Starting..." }
                val mimeType = intent.getStringExtra(MIME_TYPE)
                val position = intent.getLongExtra(POSITION, 0)
                println("LOL")
                // TODO: Play audio
            }
            STOP_ACTION -> {
                log.debug { "Received command for stopping audio playback. Stopping..." }
                // TODO: Stop audio
                println("LOL")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        log.debug { "Cancelling notifications and destroy audio player service's channel" }
        notificationHandler.close()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun startAudioPlayback(uri: Uri, mimeType: String?, position: Long) {
        startForeground(NOTIFICATION_ID, updateOrCreateNotification().notification)
        audioPlayer.stop()
        audioPlayer.clearMediaItems()

        val mediaItem = MediaItem.Builder()
            .setMimeType(mimeType ?: MimeTypes.AUDIO_RAW) // TODO
            .setUri(uri)
            .build()

        _position.value = position
        audioPlayer.setMediaItem(mediaItem)
        audioPlayer.prepare()
        audioPlayer.play()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            updateOrCreateNotification().notification
        )
    }

    private fun updateOrCreateNotification(): NotificationHandle {
        val stopIntent = Intent(this, AudioPlayerService::class.java)
        stopIntent.action = STOP_ACTION
        val stopIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, stopIntentFlags)

        val currentPosition = position.value
        val duration = duration.value.coerceAtLeast(1)
        val progressText = "${TIME_FORMAT.format(currentPosition)} / ${TIME_FORMAT.format(duration)}"

        val notification = Notification(description = progressText)
        val notificationCallback: NotificationCompat.Builder.() -> Unit = {
            setOngoing(audioPlayer.isLoading || audioPlayer.isPlaying)
            addAction(NotificationCompat.Action(R.drawable.ic_media_pause, "Stop", stopPendingIntent))
            setProgress(duration.toInt(), currentPosition.toInt(), audioPlayer.isLoading)
            setOnlyAlertOnce(true)
        }

        notificationHandle?.let { handle ->
            notificationHandle = notificationHandler.update(handle, notification, notificationCallback)
        } ?: run {
            notificationHandle = notificationHandler.create(notification, "Test", notificationCallback) // TODO
        }
        return requireNotNull(notificationHandle)
    }

    inner class ServiceBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    companion object {
        const val START_ACTION = "de.connect2x.trixnity.messenger.AudioPlayerService.START_PLAY"
        const val START_AUDIO_URI = "de.connect2x.trixnity.messenger.AudioPlayerService.START_AUDIO_URI"
        const val MIME_TYPE = "de.connect2x.trixnity.messenger.AudioPlayerService.MIME_TYPE"
        const val POSITION = "de.connect2x.trixnity.messenger.AudioPlayerService.POSITION"
        const val STOP_ACTION = "de.connect2x.trixnity.messenger.AudioPlayerService.STOP_PLAY"
        const val NOTIFICATION_CHANNEL = "AudioPlayerService"
        const val NOTIFICATION_ID = 621

        val TIME_FORMAT: DateFormat = SimpleDateFormat.getTimeInstance()
    }
}
