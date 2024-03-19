@file:JsModule("@zip.js/zip.js")
@file:JsNonModule

package externals.jszip

import js.promise.Promise
import js.typedarrays.Uint8Array
import web.streams.ReadableStream
import web.streams.WritableStream

external class ZipWriterStream(options: ZipWriterConstructorOptions = definedExternally) {

    var readable: ReadableStream<Uint8Array>
    fun <T> writable(path: String): WritableStream<T>
    fun close(
        comment: Uint8Array = definedExternally,
        options: ZipWriterCloseOptions = definedExternally
    ): Promise<Any?>
}

external interface ZipWriterStreamTransformResult<T> {
    var readable: ReadableStream<T>
    var writable: WritableStream<T>
}

external interface ZipWriterCloseOptions {
    // removed all properties
}

external interface ZipWriterConstructorOptions {
    // removed all properties
}
