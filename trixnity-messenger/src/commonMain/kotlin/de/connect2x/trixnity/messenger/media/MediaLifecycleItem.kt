package de.connect2x.trixnity.messenger.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

interface MediaLifecycleItem : AutoCloseable {
    /**
     * This function updates the lifecycle scope specified when opening the media item. When the lifecycle scope is
     * set to null, it removes the lifecycle completely.
     *
     * @param lifecycleScope the new lifecycle scope or identifier for the removal of the lifecycle
     */
    fun updateLifecycle(lifecycleScope: CoroutineScope?)
}

abstract class MediaLifecycleItemImpl(
    private val coroutineScope: CoroutineScope,
    private val state: MutableStateFlow<MediaPlayer.State>
) : MediaLifecycleItem {
    private var lifecycleCompletionAwaitJob: Job? = null
    private var lifecycleCompletionJob: DisposableHandle? = null

    override fun updateLifecycle(lifecycleScope: CoroutineScope?) {
        lifecycleCompletionAwaitJob?.cancel()
        lifecycleCompletionJob?.dispose()
        if (lifecycleScope == null) {
            lifecycleCompletionAwaitJob = null
            lifecycleCompletionJob = null
            return
        }

        lifecycleCompletionJob = lifecycleScope.coroutineContext.job.invokeOnCompletion {
            if (state.value is MediaPlayer.State.Ready) {
                close()
                return@invokeOnCompletion
            }

            lifecycleCompletionAwaitJob = coroutineScope.launch {
                state.collect {
                    if (it !is MediaPlayer.State.Ready)
                        return@collect
                    close()
                }
            }
        }
    }
}
