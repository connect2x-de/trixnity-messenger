package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.lognity.api.logger.Level
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.media.AbstractMediaItem
import de.connect2x.trixnity.messenger.media.MediaPlayer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.sync.Mutex

@OptIn(InternalCoroutinesApi::class)
internal class MediaPlayerMock(private val coroutineContext: CoroutineContext) : MediaPlayer {
    override val playingItem: MutableStateFlow<AbstractMediaItem?> = MutableStateFlow(null)
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerMock")
    private val operationMutex: Mutex = Mutex()

    private val items: ArrayList<MediaItemMock> = ArrayList()
    private val itemsMutex: SynchronizedObject = SynchronizedObject()

    fun errorAllItems() = synchronized(itemsMutex) { items.forEach { it.setError() } }

    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?,
    ): Result<MediaPlayer.Item> {
        val item = playingItem.value
        if (item != null && item.id == id) {
            log.debug { "Get current playing element and updating lifecycle" }
            item.updateLifecycle(lifecycleScope)
            return Result.success(item)
        }

        log.debug { "Creating new media item and return" }
        val newItem =
            MediaItemMock(
                id = id,
                coroutineScope = CoroutineScope(coroutineContext + SupervisorJob()),
                operationMutex = operationMutex,
                currentItemPlaying = playingItem,
            )

        items.add(newItem)
        log.debug { "Creating new media item and return" }
        return Result.success(newItem)
    }

    override fun close() = Unit

    internal class MediaItemMock(
        override val id: String,
        coroutineScope: CoroutineScope,
        operationMutex: Mutex,
        currentItemPlaying: MutableStateFlow<AbstractMediaItem?>,
    ) : AbstractMediaItem(coroutineScope, operationMutex, currentItemPlaying) {
        internal val isClosed: AtomicBoolean = AtomicBoolean(false)
        override val duration: Duration = 10.seconds

        init {
            log.level = Level.TRACE
        }

        override suspend fun onPlay(duration: Duration): Result<Unit> = Result.success(Unit)

        override suspend fun onSeekTo(position: Duration) = Unit

        override suspend fun onPause() = Unit

        override suspend fun onClose() {
            isClosed.store(true)
        }

        fun setError() = setError("This is an error")
    }
}
