package de.connect2x.trixnity.messenger.internal.utils

import de.connect2x.trixnity.messenger.util.UriHandler
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow

internal class TestUriHandler : UriHandler {
    private val uris = MutableSharedFlow<String>()

    suspend fun emit(uri: String) {
        uris.emit(uri)
    }

    override suspend fun collect(collector: FlowCollector<String>) {
        uris.collect(collector)
    }
}
