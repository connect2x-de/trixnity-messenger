package de.connect2x.trixnity.messenger.media

import android.content.ComponentName
import android.media.MediaMetadataRetriever
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import de.connect2x.trixnity.messenger.util.ContextGetter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class AndroidMediaPlayer(
    getContext: ContextGetter,
    internal val coroutineScope: CoroutineScope
) : MediaPlayer {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AndroidMediaPlayer")
    private val controller: ListenableFuture<MediaController>

    internal val currentItemPlaying: MutableStateFlow<AbstractMediaItemLifecycle?> = MutableStateFlow(null)
    internal val playingItemMutex: Mutex = Mutex()

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
                        }
                    }
                })
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?
    ): Result<MediaPlayer.Item> {
        check(media is OkioPlatformMedia) { "PlatformMedia is required to be a OkioPlatformMedia" }
        val playingItem = playingItem.value
        if (playingItem != null && playingItem.id == id) {
            playingItem.updateLifecycle(lifecycleScope)
            return Result.success(playingItem)
        }

        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            },
            onSuccess = { tempFile ->
                log.debug { "Successfully opened media as temporary file" }

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(tempFile.path.toString())
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0
                    if (duration <= 0) {
                        return Result.failure(IllegalArgumentException("Media duration could not be extracted"))
                    }

                    val coroutineCtx = coroutineScope.coroutineContext
                    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                        log.error(throwable) { "Unexpected error while running media player" }
                    }

                    val coroutineScope = CoroutineScope(SupervisorJob(coroutineCtx[Job]) + exceptionHandler)
                    val playerItem = AndroidPlayerItem(
                        id = id,
                        mimeType = mimeType,
                        tempFile = tempFile,
                        coroutineScope = coroutineScope,
                        player = this@AndroidMediaPlayer,
                        duration = duration.milliseconds
                    )

                    playerItem.updateLifecycle(lifecycleScope)
                    return Result.success(playerItem)
                } catch (ex: Exception) {
                    return Result.failure(IllegalArgumentException("Illegal media specified", ex))
                } finally {
                    retriever.release()
                }
            }
        )
    }

    override fun close() {
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
    }

}
