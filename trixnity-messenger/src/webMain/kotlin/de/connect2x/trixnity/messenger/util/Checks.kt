package de.connect2x.trixnity.messenger.util

import js.array.jsArrayOf
import js.objects.unsafeJso
import web.blob.Blob
import web.navigator.navigator
import web.storage.getDirectory
import web.url.URL
import web.workers.Worker

@OptIn(ExperimentalWasmJsInterop::class)
private val blob = Blob(blobParts = jsArrayOf("".toJsString()), options = unsafeJso { type = "text/javascript" })

internal fun isWorkersAvailable(): Boolean {
    return runCatching { withBlob { Worker(it).terminate() } }.isSuccess
}

internal suspend fun isOPFSAvailable(): Boolean {
    return runCatching { navigator.storage.getDirectory() }.isSuccess
}

private inline fun <T> withBlob(block: (String) -> T): T {
    val url = URL.createObjectURL(blob)

    return try {
        block(url)
    } finally {
        URL.revokeObjectURL(url)
    }
}
