package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

interface MediaItemLifecycle : AutoCloseable {
    val state: StateFlow<MediaPlayer.Item.State>

    /**
     * This function updates the lifecycle scope specified when opening the media item. When the lifecycle scope is
     * set to null, it removes the lifecycle completely.
     *
     * @param lifecycleScope the new lifecycle scope or identifier for the removal of the lifecycle
     */
    fun updateLifecycle(lifecycleScope: CoroutineScope?)
}

abstract class MediaItemLifecycleImpl(private val coroutineScope: CoroutineScope) : MediaItemLifecycle {
    protected abstract val log: Logger

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
            if (state.value !is MediaPlayer.Item.State.Playing) {
                log.debug { "Media player item '${(this@MediaItemLifecycleImpl as MediaPlayer.Item).id}' is ready on completion of lifecycle, closing item..." }
                close() // TODO
                return@invokeOnCompletion
            }

            log.debug { "Media player item is still playing on completion of lifecycle, awaiting end..." }
            lifecycleCompletionAwaitJob = coroutineScope.launch {
                state.collect {
                    if (it !is MediaPlayer.Item.State.Ready)
                        return@collect

                    log.debug { "Media player item '${(this@MediaItemLifecycleImpl as MediaPlayer.Item).id}' is ready after waiting, closing item..." }
                    close()
                }
            }
        }
    }
}
