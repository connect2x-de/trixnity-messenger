package de.connect2x.trixnity.messenger.internal.utils

import de.connect2x.trixnity.messenger.util.SharedData
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class TestSharedDataHandler(initialData: SharedData? = null) : SharedDataHandler {
    private val sharedData = MutableStateFlow<SharedData?>(initialData)

    override val value: SharedData?
        get() = sharedData.value

    override val replayCache: List<SharedData?>
        get() = sharedData.replayCache

    override suspend fun collect(collector: FlowCollector<SharedData?>): Nothing {
        sharedData.collect(collector)
    }

    override fun onShare(files: SharedData?) {
        sharedData.update { files }
    }
}
