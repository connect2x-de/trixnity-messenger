package de.connect2x.trixnity.messenger.media

import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

internal class AndroidMediaPlayer(
    getContext: ContextGetter,
    private val coroutineScope: CoroutineScope
) : MediaPlayer {
    private val controller: ListenableFuture<MediaController>
    private val updateJob = coroutineScope.launch {
        while (isActive) {
            delay(150)
            val playingItem = currentItemPlaying.value ?: continue

            withMediaController { controller ->
                playingItem.duration.value = controller.duration.coerceAtLeast(0).milliseconds
                playingItem.elapsedTime.value = controller.currentPosition.coerceAtLeast(0).milliseconds
            }
        }
    }

    internal val currentItemPlaying: MutableStateFlow<AndroidPlayerItem?> = MutableStateFlow(null)
    internal val playingItemMutex: Mutex = Mutex()

    override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    init {
        val context = getContext()
        val sessionToken = SessionToken(context, ComponentName(context, MediaPlayerService::class.java))
        controller = MediaController.Builder(context, sessionToken).buildAsync()
        controller.addListener(
            {
                val mediaController = controller.get()
                mediaController.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val item = currentItemPlaying.value ?: return
                        try {
                            val controller = controller.get(10, TimeUnit.SECONDS)
                            if (controller.playbackState != Player.STATE_ENDED || isPlaying)
                                return

                            coroutineScope.launch {
                                item.pause()
                                item.elapsedTime.value = Duration.ZERO
                            }
                        } catch (ex: TimeoutException) {
                            log.error(ex) { "Failed to acquire media controller: Unable to init player in 10 seconds" }
                            state.value = MediaPlayer.State.Failed("Unable to acquire media controller: $ex")
                        }
                    }
                })
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    override suspend fun open(
        media: PlatformMedia,
        mimeType: String,
        coroutineScope: CoroutineScope
    ): Result<MediaPlayer.Item> {
        check(media is OkioPlatformMedia) { "PlatformMedia is required to be a OkioPlatformMedia" }
        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            },
            onSuccess = { tempFile ->
                log.debug { "Successfully opened media as temporary file" }
                return Result.success(AndroidPlayerItem(mimeType, tempFile, coroutineScope, this))
            }
        )
    }

    override fun close() {
        updateJob.cancel()
        coroutineScope.launch {
            withMediaController { controller ->
                controller.clearMediaItems()
            }
        }
    }

    internal suspend fun withMediaController(closure: suspend (MediaController) -> Unit): Unit = try {
        val controller = withContext(Dispatchers.IO) {
            controller.get(10, TimeUnit.SECONDS)
        }

        withContext(Dispatchers.Main) {
            closure(controller)
        }
    } catch (ex: TimeoutException) {
        log.error(ex) { "Failed to acquire media controller: Unable to init player in 10 seconds" }
        state.value = MediaPlayer.State.Failed("Unable to acquire media controller: Failed to create in 10 seconds")
    }
}
