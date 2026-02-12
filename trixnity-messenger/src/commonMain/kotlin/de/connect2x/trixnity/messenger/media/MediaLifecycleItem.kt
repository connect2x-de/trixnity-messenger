package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

interface MediaLifecycleItem : AutoCloseable {
    val state: StateFlow<MediaPlayer.State>

    /**
     * This function updates the lifecycle scope specified when opening the media item. When the lifecycle scope is
     * set to null, it removes the lifecycle completely.
     *
     * @param lifecycleScope the new lifecycle scope or identifier for the removal of the lifecycle
     */
    fun updateLifecycle(lifecycleScope: CoroutineScope?)
}

abstract class MediaLifecycleItemImpl(private val coroutineScope: CoroutineScope) : MediaLifecycleItem {
    protected open val log: Logger = Logger("de.connect2x.trixnity.messenger.media.MediaLifecycleItem")

    private var lifecycleCompletionAwaitJob: Job? = null
    private var lifecycleCompletionJob: DisposableHandle? = null

    override fun updateLifecycle(lifecycleScope: CoroutineScope?) {
        log.debug { "Cancelling lifecycle jobs" }
        lifecycleCompletionAwaitJob?.cancel()
        lifecycleCompletionJob?.dispose()
        if (lifecycleScope == null) {
            lifecycleCompletionAwaitJob = null
            lifecycleCompletionJob = null
            return
        }

        log.debug { "Updating lifecycle of media item" }
        lifecycleCompletionJob = lifecycleScope.coroutineContext.job.invokeOnCompletion {
            if (state.value is MediaPlayer.State.Ready) {
                log.debug { "Media player item is ready on completion of lifecycle, closing item..." }
                close()
                return@invokeOnCompletion
            }

            log.debug { "Media player item is still playing on completion of lifecycle, awaiting end..." }
            lifecycleCompletionAwaitJob = coroutineScope.launch {
                state.collect {
                    if (it !is MediaPlayer.State.Ready)
                        return@collect

                    log.debug { "Media player item is ready after waiting, closing item..." }
                    close()
                }
            }
        }
    }
}
