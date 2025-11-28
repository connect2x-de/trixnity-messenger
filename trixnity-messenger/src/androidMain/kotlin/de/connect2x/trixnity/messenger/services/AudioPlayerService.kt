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
import de.connect2x.sysnotify.withActivationFactory
import de.connect2x.sysnotify.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

class AudioPlayerService : Service() {
    private lateinit var audioPlayer: ExoPlayer
    private val binder = ServiceBinder()
    private val notificationHandler: NotificationHandler by lazy {
        NotificationHandler(
            id = NOTIFICATION_CHANNEL,
            name = "Audio Player",
            priority = NotificationPriority.LOW,
            appId = "de.connect2x.trixnity.messenger" // TODO
        )
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressUpdateJob: Job? = null

    private val _elapsedTime: MutableStateFlow<Long> = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    private val _duration: MutableStateFlow<Long> = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var notificationHandle: NotificationHandle? = null

    override fun onCreate() {
        log.debug { "Creating notification channel for audio player service" }
        notificationHandler.withContext { applicationContext }
        notificationHandler.withActivationFactory { _, _ -> null }

        audioPlayer = ExoPlayer.Builder(this).build()
        audioPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (audioPlayer.playbackState == Player.STATE_READY) {
                    _elapsedTime.value = audioPlayer.currentPosition.coerceAtLeast(0)
                    _duration.value = audioPlayer.duration.coerceAtLeast(0)
                    if (isPlaying)
                        startProgressUpdates()
                    else
                        stopProgressUpdates()
                    updateNotification()
                }

                if (!isPlaying) {
                    if (audioPlayer.currentPosition >= audioPlayer.duration) {
                        _elapsedTime.value = 0
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log.debug { "Received command" } // TODO
        when (intent?.action) {
            START_ACTION -> {
                // TODO: Replace with getParcelableExtra(<name>, <class>) when min API level is >= 33
                val uri = @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(START_AUDIO_URI) ?: run {
                    log.error { "Unable to play audio file: Start player command doesn't contain URI of resource" }
                    return START_STICKY
                }

                log.debug { "Received command for playing audio '$uri'. Starting..." }
                val mimeType = intent.getStringExtra(MIME_TYPE)
                val position = intent.getLongExtra(POSITION, 0)
                startAudioPlayback(uri, mimeType, position)
            }
            STOP_ACTION -> {
                log.debug { "Received command for stopping audio playback. Stopping..." }
                stopAudioPlayback()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        log.debug { "Cancelling notifications and destroy audio player service's channel" }
        stopAudioPlayback()
        notificationHandler.close()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        stopSelf()
        return super.onUnbind(intent)
    }

    fun seekTo(position: Long) {
        if (audioPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) {
            audioPlayer.seekTo(position)
            updateNotification() // TODO: Is this really required
        }
    }

    private fun startAudioPlayback(uri: Uri, mimeType: String?, position: Long) {
        startForeground(NOTIFICATION_ID, updateOrCreateNotification().notification)
        audioPlayer.stop()
        audioPlayer.clearMediaItems()

        val mediaItem = MediaItem.Builder()
            .setMimeType(mimeType ?: MimeTypes.AUDIO_RAW) // TODO
            .setUri(uri)
            .build()

        _elapsedTime.value = position
        audioPlayer.setMediaItem(mediaItem)
        audioPlayer.prepare()
        audioPlayer.play()
        if (position != 0L && audioPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) {
            audioPlayer.seekTo(position)
            updateNotification() // TODO: Is this really required
        }
    }

    fun stopAudioPlayback() {
        log.debug { "Stopping audio..." }
        if (this::audioPlayer.isInitialized) {
            audioPlayer.stop()
        }

        updateNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)

        audioPlayer.stop()
        audioPlayer.clearMediaItems()
    }

    private fun updateNotification() {
        val notification = updateOrCreateNotification()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification.notification)
    }

    private fun updateOrCreateNotification(): NotificationHandle {
        val stopIntent = Intent(this, AudioPlayerService::class.java)
        stopIntent.action = STOP_ACTION
        val stopIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, stopIntentFlags)

        val currentPosition = elapsedTime.value
        val duration = duration.value.coerceAtLeast(1)
        val progressText = "${TIME_FORMAT.format(currentPosition)} / ${TIME_FORMAT.format(duration)}"

        val notification = Notification(description = progressText)
        val notificationCallback: NotificationCompat.Builder.() -> Unit = {
            setOngoing(audioPlayer.isLoading || audioPlayer.isPlaying)
            addAction(NotificationCompat.Action(R.drawable.ic_media_pause, "Stop", stopPendingIntent))
            setProgress(duration.toInt(), currentPosition.toInt(), audioPlayer.isLoading)
            setOnlyAlertOnce(true)
        }

        notificationHandle = notificationHandler.create(notification, "Test", notificationCallback) // TODO
        return requireNotNull(notificationHandle)
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = coroutineScope.launch(CoroutineName("AudioPlayerService Progress Loop")) {
            while (isActive && this@AudioPlayerService::audioPlayer.isInitialized && audioPlayer.isPlaying) {
                _elapsedTime.value = audioPlayer.currentPosition.coerceAtLeast(0)
                _duration.value = audioPlayer.duration.coerceAtLeast(0)
                delay(100.milliseconds) // The moment of cooperative multitasking
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
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
