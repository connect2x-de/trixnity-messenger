package de.connect2x.trixnity.messenger.export

import js.typedarrays.Uint8Array
import web.streams.WritableStream

actual data class Destination(
    val stream: WritableStream<Uint8Array>
)
