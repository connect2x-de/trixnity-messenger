/**
 * zip.js is a JavaScript open-source library (BSD-3-Clause license) for
 * compressing and decompressing zip files. It has been designed to handle large amounts
 * of data. It supports notably multi-core compression, native compression with
 * compression streams, archives larger than 4GB with Zip64, split zip files and data
 * encryption.
 *
 * @author Gildas Lormeau
 * @license BSD-3-Clause
 */

@file:JsModule("@zip.js/zip.js")
@file:JsNonModule
@file:Suppress("unused", "PropertyName", "KDocUnresolvedReference")

package externals.zipjs

import js.buffer.ArrayBufferLike
import js.core.Void
import js.generator.AsyncGenerator
import js.promise.Promise
import js.typedarrays.Uint8Array
import web.abort.AbortSignal
import web.blob.Blob
import web.compression.CompressionStream
import web.compression.DecompressionStream
import web.file.File
import web.streams.ReadableStream
import web.streams.TransformStream
import web.streams.WritableStream
import web.url.URL
import kotlin.js.Date

/**
 * Represents the `FileSystemEntry` class.
 *
 * @see [https://wicg.github.io/entries-api/#api-entry|specification]
 */
external interface FileSystemEntryLike

/**
 * Represents the `FileSystemHandle` class.
 *
 * @see [https://fs.spec.whatwg.org/#api-filesystemhandle]
 */
external interface FileSystemHandleLike

/**
 * Represents a generic `TransformStream` class.
 *
 * @see [https://streams.spec.whatwg.org/#generictransformstream|specification]
 */
abstract external class TransformStreamLike<I, O> {
    /**
     * The readable stream.
     */
    val readable: ReadableStream<O>

    /**
     * The writable stream.
     */
    val writable: WritableStream<I>
}

/**
 * Configures zip.js
 *
 * @param configuration The configuration.
 */
external fun configure(configuration: Configuration)

/**
 * Represents the configuration passed to [configure].
 */
external interface Configuration : WorkerConfiguration {
    /**
     * The maximum number of web workers used to compress/decompress data simultaneously.
     *
     * @defaultValue `navigator.hardwareConcurrency`
     */
    var maxWorkers: Number

    /**
     * The delay in milliseconds before idle web workers are automatically terminated. You can call `terminateWorkers()` to terminate idle workers.
     *
     * @defaultValue 5000
     */
    var terminateWorkerTimeout: Number?

    /**
     * The URIs of the compression/decompression scripts run in web workers.
     *
     * It allows using alternative deflate implementations or specifying a URL to the worker script if the CSP of the page blocks scripts imported from a Blob URI.
     * The properties `deflate` and `inflate` must specify arrays of URLs to import the deflate/inflate web workers, respectively.
     * The first URL is relative to the base URI of the document. The other URLs are relative to the URL of the first script. Scripts in the array are executed in order.
     * If you only use deflation or inflation, the unused `deflate`/`inflate` property can be omitted.
     */
    var workerScripts: WorkerScripts?

    /**
     * The size of the chunks in bytes during data compression/decompression.
     *
     * @defaultValue 524288
     */
    var chunkSize: Number?

    /**
     * The codec implementation used to compress data.
     *
     * @defaultValue [ZipDeflate]
     */
    var Deflate: (() -> ZipDeflate)?

    /**
     * The codec implementation used to decompress data.
     *
     * @defaultValue [ZipInflate]
     */
    var Inflate: (() -> ZipInflate)?

    /**
     * The stream implementation used to compress data when `useCompressionStream` is set to `false`.
     *
     * @defaultValue [CodecStream]
     */
    var CompressionStream: (() -> TransformStreamLike<dynamic, dynamic>)?

    /**
     * The stream implementation used to decompress data when `useCompressionStream` is set to `false`.
     *
     * @defaultValue [CodecStream]
     */
    var DecompressionStream: (() -> TransformStreamLike<dynamic, dynamic>)?
}

external interface WorkerScripts {
    /**
     * The URIs of the scripts implementing used for compression.
     */
    var deflate: Array<String>?

    /**
     * The URIs of the scripts implementing used for decompression.
     */
    var inflate: Array<String>?
}

/**
 * Represents configuration passed to [configure], the constructor of [ZipReader], [Entry.getData], the constructor of [ZipWriter], and [ZipWriter.add].
 */
external interface WorkerConfiguration {
    /**
     * `true` to use web workers to compress/decompress data in non-blocking background processes.
     *
     * @defaultValue true
     */
    var useWebWorkers: Boolean?

    /**
     * `true` to use the native API [CompressionStream]/[DecompressionStream] to compress/decompress data.
     *
     * @defaultValue true
     */
    var useCompressionStream: Boolean?
}

/**
 * Transforms event-based third-party codec implementations into implementations compatible with zip.js
 *
 * @param library The third-party codec implementations.
 * @param constructorOptions The options passed to the third-party implementations when building instances.
 * @param registerDataHandler The function called to handle the `data` events triggered by a third-party codec implementation.
 * @returns An instance containing classes compatible with [ZipDeflate] and [ZipInflate].
 */
external fun initShimAsyncCodec(
    library: EventBasedZipLibrary,
    constructorOptions: dynamic,
    registerDataHandler: RegisterDataHandler,
): ZipLibrary

/**
 * Represents the callback function used to register the `data` event handler.
 */
external interface RegisterDataHandler {
    /**
     * @param codec The third-party codec instance.
     * @param onData The `data` event handler.
     */
    operator fun invoke(codec: EventBasedCodec, onData: DataHandler)
}

/**
 * Represents the callback function used to handle `data` events.
 */
external interface DataHandler {
    /**
     * @param data The processed chunk of data.
     */
    operator fun invoke(data: Uint8Array<out ArrayBufferLike>)
}

/**
 * Terminates all the web workers
 */
@JsName("terminateWorkers")
external suspend fun terminateWorkers()

@JsName("terminateWorkers")
external fun terminateWorkersAsync(): Promise<Void>

/**
 * Represents event-based implementations used to compress/decompress data.
 */
external interface EventBasedZipLibrary {
    /**
     * The class used to compress data.
     */
    var Deflate: () -> EventBasedCodec

    /**
     * The class used to decompress data.
     */
    var Inflate: () -> EventBasedCodec
}

/**
 * Represents an event-based implementation of a third-party codec.
 */
abstract external class EventBasedCodec {
    /**
     * Appends a chunk of data to compress/decompress
     *
     * @param data The chunk of data to append.
     */
    fun push(data: Uint8Array<out ArrayBufferLike>)

    /**
     * The function called when a chunk of data has been compressed/decompressed.
     *
     * @param data The chunk of compressed/decompressed data.
     */
    fun ondata(data: Uint8Array<out ArrayBufferLike>? = definedExternally)
}

/**
 * Represents the implementations zip.js uses to compress/decompress data.
 */
external interface ZipLibrary {
    /**
     * The class used to compress data.
     *
     * @defaultValue [ZipDeflate]
     */
    var Deflate: () -> ZipDeflate

    /**
     * The class used to decompress data.
     *
     * @defaultValue [ZipInflate]
     */
    var Inflate: () -> ZipInflate
}

abstract external class SyncCodec {
    /**
     * Appends a chunk of decompressed data to compress
     *
     * @param data The chunk of decompressed data to append.
     * @returns A chunk of compressed data.
     */
    fun append(data: Uint8Array<out ArrayBufferLike>): Uint8Array<out ArrayBufferLike>
}

/**
 * Represents an instance used to compress data.
 */
open external class ZipDeflate : SyncCodec {
    /**
     * Flushes the data
     *
     * @returns A chunk of compressed data.
     */
    open fun flush(): Uint8Array<out ArrayBufferLike>
}

/**
 * Represents a codec used to decompress data.
 */
open external class ZipInflate : SyncCodec {
    /**
     * Flushes the data
     */
    open fun flush()
}

/**
 * Represents a class implementing `CompressionStream` or `DecompressionStream` interfaces.
 */
abstract external class CodecStream<I, O> : TransformStream<I, O>

/**
 * Returns the MIME type corresponding to a filename extension.
 *
 * @param fileExtension the extension of the filename.
 * @returns The corresponding MIME type.
 */
external fun getMimeType(fileExtension: String): String

/**
 * Represents an instance used to read data from a [ReadableStream] instance.
 */
external interface ReadableReader<T> {
    /**
     * The `ReadableStream` instance.
     */
    val readable: ReadableStream<T>
}

/**
 * Represents an instance used to read unknown type of data.
 */
open external class Reader<T> : ReadableReader<T> {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: T)

    /**
     * The `ReadableStream` instance.
     */
    override val readable: ReadableStream<T>

    /**
     * The total size of the data in bytes.
     */
    open val size: Number

    /**
     * Initializes the instance asynchronously
     */
    @JsName("init")
    suspend fun init()

    @JsName("init")
    fun initAsync(): Promise<Void>

    /**
     * Reads a chunk of data
     *
     * @param index The byte index of the data to read.
     * @param length The length of the data to read in bytes.
     * @returns A promise resolving to a chunk of data.
     */
    @JsName("readCompatibleUint8Array")
    suspend fun readCompatibleUint8Array(index: Number, length: Number): Uint8Array<out ArrayBufferLike>

    @JsName("readCompatibleUint8Array")
    fun readCompatibleUint8ArrayAsync(index: Number, length: Number): Promise<Uint8Array<out ArrayBufferLike>>
}

/**
 * Represents a [Reader] instance used to read data provided as a `string`.
 */
open external class TextReader : Reader<String> {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: String)
}

/**
 * Represents a Reader instance used to read data provided as a `Blob` instance.
 */
open external class BlobReader : Reader<Blob> {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: Blob)
}

/**
 * Represents a Reader instance used to read data provided as a Data URI `string` encoded in Base64.
 */
open external class Data64URIReader : Reader<String> {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: String)
}

/**
 * Represents a Reader instance used to read data provided as a `Uint8Array` instance.
 */
open external class CompatibleUint8ArrayReader : Reader<Uint8Array<out ArrayBufferLike>> {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: Uint8Array<out ArrayBufferLike>)
}

/**
 * Represents a [Reader] instance used to read data provided as an array of [ReadableReader] instances (e.g. split zip files).
 *
 * @deprecated Use [SplitDataReader] instead.
 */
open external class SplitZipReader : SplitDataReader {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: Array<dynamic>)
}

/**
 * Represents a Reader instance used to read data provided as an array of [ReadableReader] instances (e.g. split zip files).
 */
open external class SplitDataReader : Reader<Array<dynamic>> {
    /**
     * Creates the [Reader] instance
     *
     * @param value The data to read.
     */
    constructor(value: Array<dynamic>)
}

/**
 * Represents a URL stored into a `string`.
 */
external interface URLString

/**
 * Represents a [Reader] instance used to fetch data from a URL.
 */
open external class HttpReader : Reader<URLString> {
    /**
     * Creates the [HttpReader] instance
     *
     * @param url The URL of the data.
     * @param options The options.
     */
    constructor(url: URLString, options: HttpOptions? = definedExternally)

    /**
     * Creates the [HttpReader] instance
     *
     * @param url The URL of the data.
     * @param options The options.
     */
    constructor(url: String, options: HttpOptions? = definedExternally)

    /**
     * Creates the [HttpReader] instance
     *
     * @param url The URL of the data.
     * @param options The options.
     */
    constructor(url: URL, options: HttpOptions? = definedExternally)
}

/**
 * Represents a [Reader] instance used to fetch data from servers returning `Accept-Ranges` headers.
 */
open external class HttpRangeReader : HttpReader {
    /**
     * Creates the [HttpRangeReader] instance
     *
     * @param url The URL of the data.
     * @param options The options.
     */
    constructor(url: URLString, options: HttpRangeOptions? = definedExternally)

    /**
     * Creates the [HttpRangeReader] instance
     *
     * @param url The URL of the data.
     * @param options The options.
     */
    constructor(url: String, options: HttpRangeOptions? = definedExternally)

    /**
     * Creates the [HttpRangeReader] instance
     *
     * @param url The URL of the data.
     * @param options The options.
     */
    constructor(url: URL, options: HttpRangeOptions? = definedExternally)
}

/**
 * Represents the options passed to the constructor of [HttpReader].
 */
external interface HttpOptions : HttpRangeOptions {
    /**
     * `true` to use `Range` headers when fetching data from servers returning `Accept-Ranges` headers.
     *
     * @defaultValue false
     */
    var useRangeHeader: Boolean?

    /**
     * `true` to always use `Range` headers when fetching data.
     *
     * @defaultValue false
     */
    var forceRangeRequests: Boolean?

    /**
     * `true` to prevent using `HEAD` HTTP request in order the get the size of the content.
     * `false` to explicitly use `HEAD`, this is useful in case of CORS where `Access-Control-Expose-Headers: Content-Range` is not returned by the server.
     *
     * @defaultValue false
     */
    var preventHeadRequest: Boolean?

    /**
     * `true` to use `Range: bytes=-22` on the first request and cache the EOCD, make sure beforehand that the server supports a suffix range request.
     *
     * @defaultValue false
     */
    var combineSizeEocd: Boolean?
}

/**
 * Represents options passed to the constructor of [HttpRangeReader] and [HttpReader].
 */
external interface HttpRangeOptions {
    /**
     * `true` to rely `XMLHttpRequest` instead of `fetch` to fetch data.
     *
     * @defaultValue false
     */
    var useXHR: Boolean?

    /**
     * The HTTP headers.
     */
    var headers: Map<String, String>
}

/**
 * Represents an instance used to write data into a `WritableStream` instance.
 */
external interface WritableWriter<T> {
    /**
     * The `WritableStream` instance.
     */
    val writable: WritableStream<T>

    /**
     * The maximum size of split data when creating a [ZipWriter] instance or when calling [Entry.getData] with a generator of [WritableWriter] instances.
     */
    val maxSize: Number? get() = definedExternally
}

/**
 * Represents an instance used to write unknown type of data.
 */
open external class Writer<T> : WritableWriter<T> {
    /**
     * The `WritableStream` instance.
     */
    override val writable: WritableStream<T>

    /**
     * Initializes the instance asynchronously
     */
    @JsName("init")
    suspend fun init()

    @JsName("init")
    fun initAsync(): Promise<Void>

    /**
     * Appends a chunk of data
     *
     * @param array The chunk data to append.
     */
    @JsName("writeCompatibleUint8Array")
    suspend fun writeCompatibleUint8Array(array: Uint8Array<out ArrayBufferLike>)

    @JsName("writeCompatibleUint8Array")
    fun writeCompatibleUint8ArrayAsync(array: Uint8Array<out ArrayBufferLike>): Promise<Void>

    /**
     * Retrieves all the written data
     *
     * @returns A promise resolving to the written data.
     */
    @JsName("getData")
    suspend fun getData(): T

    @JsName("getData")
    fun getDataAsync(): Promise<T>
}

/**
 * Represents a [Writer] instance used to retrieve the written data as a `string`.
 */
external class TextWriter : Writer<String> {
    /**
     * Creates the [TextWriter] instance
     *
     * @param encoding The encoding of the text.
     */
    constructor(encoding: String? = definedExternally)
}

/**
 * Represents a [WritableWriter] instance used to retrieve the written data as a `Blob` instance.
 */
external class BlobWriter : WritableWriter<Blob> {
    /**
     * The `WritableStream` instance.
     */
    override val writable: WritableStream<Blob>

    /**
     * Initializes the instance asynchronously
     */
    @JsName("init")
    suspend fun init()

    @JsName("init")
    fun initAsync(): Promise<Void>

    /**
     * Creates the [BlobWriter] instance
     *
     * @param mimeString The MIME type of the content.
     */
    constructor(mimeString: String? = definedExternally)

    /**
     * Retrieves all the written data
     *
     * @returns A promise resolving to the written data.
     */
    @JsName("getData")
    suspend fun getData(): Blob

    @JsName("getData")
    fun getDataAsync(): Promise<Blob>
}

/**
 * Represents a [Writer] instance used to retrieve the written data as a Data URI `string` encoded in Base64.
 */
external class Data64URIWriter : Writer<String> {
    /**
     * Creates the [Data64URIWriter] instance
     *
     * @param mimeString The MIME type of the content.
     */
    constructor(mimeString: String? = definedExternally)
}

/**
 * Represents a [Writer] instance used to retrieve the written data from a generator of [WritableWriter] instances  (i.e. split zip files).
 *
 * @deprecated Use [SplitDataWriter] instead.
 */
external class SplitZipWriter : SplitDataWriter {

    /**
     * Creates the [SplitDataWriter] instance
     *
     * @param writerGenerator A generator of Writer instances.
     * @param maxSize The maximum size of the data written into [Writer] instances (default: 4GB).
     */
    constructor(
        writerGenerator: AsyncGenerator<Writer<dynamic>, Boolean, dynamic>,
        maxSize: Number? = definedExternally,
    )

    /**
     * Creates the [SplitDataWriter] instance
     *
     * @param writerGenerator A generator of Writer instances.
     * @param maxSize The maximum size of the data written into [Writer] instances (default: 4GB).
     */
    constructor(
        writerGenerator: AsyncGenerator<WritableStream<dynamic>, Boolean, dynamic>,
        maxSize: Number? = definedExternally,
    )
}

/**
 * Represents a [Writer]  instance used to retrieve the written data from a generator of [WritableWriter]  instances  (i.e. split zip files).
 */
open external class SplitDataWriter : WritableWriter<dynamic> {
    /**
     * The `WritableStream` instance.
     */
    override val writable: WritableStream<dynamic>

    /**
     * Initializes the instance asynchronously
     */
    @JsName("init")
    suspend fun init()

    @JsName("init")
    fun initAsync(): Promise<Void>

    /**
     * Creates the [SplitDataWriter] instance
     *
     * @param writerGenerator A generator of Writer instances.
     * @param maxSize The maximum size of the data written into [Writer] instances (default: 4GB).
     */
    constructor(
        writerGenerator: AsyncGenerator<Writer<dynamic>, Boolean, dynamic>,
        maxSize: Number? = definedExternally,
    )

    /**
     * Creates the [SplitDataWriter] instance
     *
     * @param writerGenerator A generator of Writer instances.
     * @param maxSize The maximum size of the data written into [Writer] instances (default: 4GB).
     */
    constructor(
        writerGenerator: AsyncGenerator<WritableStream<dynamic>, Boolean, dynamic>,
        maxSize: Number? = definedExternally,
    )
}

/**
 * Represents a [Writer]  instance used to retrieve the written data as a `Uint8Array` instance.
 */
external class CompatibleUint8ArrayWriter : Writer<Uint8Array<out ArrayBufferLike>>

/**
 * Represents an instance used to create an unzipped stream.
 */
external class ZipReaderStream<T> {
    /**
     * Creates the stream.
     *
     * @param options The options.
     */
    constructor(options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * The readable stream.
     */
    val readable: ReadableStream<ZipReaderStreamEntry>

    /**
     * The writable stream.
     */
    val writable: WritableStream<T>
}

external interface ZipReaderStreamEntry : EntryMetaData {
    val readable: ReadableStream<Uint8Array<out ArrayBufferLike>>?
}

/**
 * Represents an instance used to read a zip file.
 */
external class ZipReader<T> {
    /**
     * Creates the instance
     *
     * @param reader The [Reader] instance used to read data.
     * @param options The options.
     */
    constructor(reader: Reader<T>, options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * Creates the instance
     *
     * @param reader The [Reader] instance used to read data.
     * @param options The options.
     */
    constructor(reader: ReadableReader<T>, options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * Creates the instance
     *
     * @param reader The [Reader] instance used to read data.
     * @param options The options.
     */
    constructor(reader: ReadableStream<T>, options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * Creates the instance
     *
     * @param reader The [Reader] instance used to read data.
     * @param options The options.
     */
    constructor(reader: Array<Reader<T>>, options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * Creates the instance
     *
     * @param reader The [Reader] instance used to read data.
     * @param options The options.
     */
    constructor(reader: Array<ReadableReader<T>>, options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * Creates the instance
     *
     * @param reader The [Reader] instance used to read data.
     * @param options The options.
     */
    constructor(reader: Array<ReadableStream<T>>, options: ZipReaderConstructorOptions? = definedExternally)

    /**
     * The global comment of the zip file.
     */
    val comment: Uint8Array<out ArrayBufferLike>

    /**
     * The data prepended before the zip file.
     */
    val prependedData: Uint8Array<out ArrayBufferLike>?

    /**
     * The data appended after the zip file.
     */
    val appendedData: Uint8Array<out ArrayBufferLike>?

    /**
     * Returns all the entries in the zip file
     *
     * @param options The options.
     * @returns A promise resolving to an `array` of [Entry] instances.
     */
    @JsName("getEntries")
    suspend fun getEntries(options: ZipReaderGetEntriesOptions? = definedExternally): Array<Entry>

    @JsName("getEntries")
    fun getEntriesAsync(options: ZipReaderGetEntriesOptions? = definedExternally): Promise<Array<Entry>>

    /**
     * Returns a generator used to iterate on all the entries in the zip file
     *
     * @param options The options.
     * @returns An asynchronous generator of [Entry] instances.
     */
    fun getEntriesGenerator(
        options: ZipReaderGetEntriesOptions? = definedExternally,
    ): AsyncGenerator<Entry, Boolean, dynamic>

    /**
     * Closes the zip file
     */
    @JsName("close")
    suspend fun close(): Void

    @JsName("close")
    fun closeAsync(): Promise<Void>
}

/**
 * Represents the options passed to the constructor of [ZipReader], and `[ZipDirectory].import*`.
 */
external interface ZipReaderConstructorOptions : ZipReaderOptions, GetEntriesOptions, WorkerConfiguration {
    /**
     * `true` to extract the prepended data into [ZipReader.prependedData].
     *
     * @defaultValue false
     */
    var extractPrependedData: Boolean?

    /**
     * `true` to extract the appended data into [ZipReader.appendedData].
     *
     * @defaultValue false
     */
    var extractAppendedData: Boolean?
}

/**
 * Represents the options passed to [ZipReader.getEntries] and ..
 */
external interface ZipReaderGetEntriesOptions : GetEntriesOptions, EntryOnprogressOptions

/**
 * Represents options passed to the constructor of [ZipReader], . and ..
 */
external interface GetEntriesOptions {
    /**
     * The encoding of the filename of the entry.
     */
    var filenameEncoding: String?

    /**
     * The encoding of the comment of the entry.
     */
    var commentEncoding: String?

    /**
     * The function called for decoding the filename and the comment of the entry.
     *
     * @param value The raw text value.
     * @param encoding The encoding of the text.
     * @returns The decoded text value or `undefined` if the raw text value should be decoded by zip.js.
     */
    var decodeText: ((value: Uint8Array<out ArrayBufferLike>, encoding: String) -> String?)?
}

/**
 * Represents options passed to the constructor of [ZipReader] and [Entry.getData].
 */
external interface ZipReaderOptions {
    /**
     * `true` to check only if the password is valid.
     *
     * @defaultValue false
     */
    var checkPasswordOnly: Boolean?

    /**
     * `true` to check the signature of the entry.
     *
     * @defaultValue false
     */
    var checkSignature: Boolean?

    /**
     * The password used to decrypt the content of the entry.
     */
    var password: String?

    /**
     * `true` to read the data as-is without decompressing it and without decrypting it.
     */
    var passThrough: Boolean?

    /**
     * The password used to encrypt the content of the entry (raw).
     */
    var rawPassword: Uint8Array<out ArrayBufferLike>?

    /**
     * The `AbortSignal` instance used to cancel the decompression.
     */
    var signal: AbortSignal?

    /**
     * `true` to prevent closing of [Writer.writable] when calling [Entry.getData].
     *
     * @defaultValue false
     */
    var preventClose: Boolean?

    /**
     * `true` to transfer streams to web workers when decompressing data.
     *
     * @defaultValue true
     */
    var transferStreams: Boolean?
}

/**
 * Represents the metadata of an entry in a zip file (Core API).
 */
external interface EntryMetaData {
    /**
     * The byte offset of the entry.
     */
    var offset: Number

    /**
     * The filename of the entry.
     */
    var filename: String

    /**
     * The filename of the entry (raw).
     */
    var rawFilename: Uint8Array<out ArrayBufferLike>

    /**
     * `true` if the filename is encoded in UTF-8.
     */
    var filenameUTF8: Boolean

    /**
     * `true` if the entry is a directory.
     */
    var directory: Boolean

    /**
     * `true` if the entry is an executable file
     */
    var executable: Boolean

    /**
     * `true` if the content of the entry is encrypted.
     */
    var encrypted: Boolean

    /**
     * `true` if the content of the entry is encrypted with the ZipCrypto algorithm.
     */
    var zipCrypto: Boolean

    /**
     * The size of the compressed data in bytes.
     */
    var compressedSize: Number

    /**
     * The size of the decompressed data in bytes.
     */
    var uncompressedSize: Number

    /**
     * The last modification date.
     */
    var lastModDate: Date

    /**
     * The last access date.
     */
    var lastAccessDate: Date?

    /**
     * The creation date.
     */
    var creationDate: Date?

    /**
     * The last modification date (raw).
     */
    var rawLastModDate: Number

    /**
     * The last access date (raw).
     */
    var rawLastAccessDate: Number?

    /**
     * The creation date (raw).
     */
    var rawCreationDate: Number

    /**
     * The comment of the entry.
     */
    var comment: String

    /**
     * The comment of the entry (raw).
     */
    var rawComment: Uint8Array<out ArrayBufferLike>

    /**
     * `true` if the comment is encoded in UTF-8.
     */
    var commentUTF8: Boolean

    /**
     * The signature (CRC32 checksum) of the content.
     */
    var signature: Number

    /**
     * The extra field.
     */
    var extraField: Map<Number, EntryExtraField>?

    /**
     * The extra field (raw).
     */
    var rawExtraField: Uint8Array<out ArrayBufferLike>

    /**
     * `true` if the entry is using Zip64.
     */
    var zip64: Boolean

    /**
     * The "Version" field.
     */
    var version: Number

    /**
     * The "Version made by" field.
     */
    var versionMadeBy: Number

    /**
     * `true` if `internalFileAttributes` and `externalFileAttributes` are compatible with MS-DOS format.
     */
    var msDosCompatible: Boolean

    /**
     * The internal file attributes (raw).
     */
    var internalFileAttributes: Number

    /**
     * The external file attributes (raw).
     */
    var externalFileAttributes: Number

    /**
     * The internal file attribute (raw).
     * @deprecated Use [EntryMetaData.internalFileAttributes] instead.
     */
    var internalFileAttribute: Number

    /**
     * The external file attribute (raw).
     * @deprecated Use [EntryMetaData.externalFileAttributes] instead.
     */
    var externalFileAttribute: Number

    /**
     * The number of the disk where the entry data starts.
     */
    var diskNumberStart: Number

    /**
     * The compression method.
     */
    var compressionMethod: Number
}

external interface EntryExtraField {
    var type: Number
    var data: Uint8Array<out ArrayBufferLike>
}

/**
 * Represents an entry with its data and metadata in a zip file (Core API).
 */
external interface Entry : EntryMetaData {
    /**
     * Returns the content of the entry
     *
     * @param writer The . instance used to write the content of the entry.
     * @param options The options.
     * @returns A promise resolving to the type to data associated to `writer`.
     */
    @JsName("getData")
    suspend fun <T> getData(writer: Writer<T>, options: EntryGetDataCheckPasswordOptions? = definedExternally): T

    @JsName("getData")
    fun <T> getDataAsync(writer: Writer<T>, options: EntryGetDataCheckPasswordOptions? = definedExternally): Promise<T>

    /**
     * Returns the content of the entry
     *
     * @param writer The . instance used to write the content of the entry.
     * @param options The options.
     * @returns A promise resolving to the type to data associated to `writer`.
     */
    @JsName("getData")
    suspend fun <T> getData(
        writer: WritableWriter<T>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): T

    @JsName("getData")
    fun <T> getDataAsync(
        writer: WritableWriter<T>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): Promise<T>

    /**
     * Returns the content of the entry
     *
     * @param writer The . instance used to write the content of the entry.
     * @param options The options.
     * @returns A promise resolving to the type to data associated to `writer`.
     */
    @JsName("getData")
    suspend fun <T> getData(
        writer: WritableStream<T>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): T

    @JsName("getData")
    fun <T> getDataAsync(
        writer: WritableStream<T>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): Promise<T>

    /**
     * Returns the content of the entry
     *
     * @param writer The . instance used to write the content of the entry.
     * @param options The options.
     * @returns A promise resolving to the type to data associated to `writer`.
     */
    @JsName("getData")
    suspend fun <T> getData(
        writer: AsyncGenerator<Writer<T>, Boolean, dynamic>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): T

    @JsName("getData")
    fun <T> getDataAsync(
        writer: AsyncGenerator<Writer<T>, Boolean, dynamic>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): Promise<T>

    /**
     * Returns the content of the entry
     *
     * @param writer The . instance used to write the content of the entry.
     * @param options The options.
     * @returns A promise resolving to the type to data associated to `writer`.
     */
    @JsName("getData")
    suspend fun <T> getData(
        writer: AsyncGenerator<WritableWriter<T>, Boolean, dynamic>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): T

    @JsName("getData")
    fun <T> getDataAsync(
        writer: AsyncGenerator<WritableWriter<T>, Boolean, dynamic>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): Promise<T>

    /**
     * Returns the content of the entry
     *
     * @param writer The . instance used to write the content of the entry.
     * @param options The options.
     * @returns A promise resolving to the type to data associated to `writer`.
     */
    @JsName("getData")
    suspend fun <T> getData(
        writer: AsyncGenerator<WritableStream<T>, Boolean, dynamic>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): T

    @JsName("getData")
    fun <T> getDataAsync(
        writer: AsyncGenerator<WritableStream<T>, Boolean, dynamic>,
        options: EntryGetDataCheckPasswordOptions? = definedExternally
    ): Promise<T>
}

/**
 * Represents the options passed to . and `..get*`.
 */
external interface EntryGetDataOptions : EntryDataOnprogressOptions, ZipReaderOptions, WorkerConfiguration

/**
 * Represents the options passed to . and `..get*`.
 */
external interface EntryGetDataCheckPasswordOptions : EntryGetDataOptions

/**
 * Represents an instance used to create a zipped stream.
 */
external class ZipWriterStream {
    /**
     * Creates the stream.
     *
     * @param options The options.
     */
    constructor(options: ZipWriterConstructorOptions? = definedExternally)

    /**
     * The readable stream.
     */
    val readable: ReadableStream<Uint8Array<out ArrayBufferLike>>

    /**
     * The ZipWriter property.
     */
    val zipWriter: ZipWriter<dynamic>

    /**
     * Returns an object containing a readable and writable property for the .pipeThrough method
     *
     * @param path The name of the stream when unzipped.
     * @returns An object containing readable and writable properties
     */
    fun <T> transform(path: String): TransformStreamLike<T, T>

    /**
     * Returns a WritableStream for the .pipeTo method
     *
     * @param path The directory path of where the stream should exist in the zipped stream.
     * @returns A WritableStream.
     */
    fun <T> writable(path: String): WritableStream<T>

    /**
     * Writes the entries directory, writes the global comment, and returns the content of the zipped file.
     *
     * @param comment The global comment of the zip file.
     * @param options The options.
     * @returns The content of the zip file.
     */
    @JsName("close")
    suspend fun close(
        comment: Uint8Array<out ArrayBufferLike>? = definedExternally,
        options: ZipWriterCloseOptions? = definedExternally,
    ): dynamic

    @JsName("close")
    fun closeAsync(
        comment: Uint8Array<out ArrayBufferLike>? = definedExternally,
        options: ZipWriterCloseOptions? = definedExternally,
    ): Promise<dynamic>
}

/**
 * Represents an instance used to create a zip file.
 *
 * @example
 * Here is an example showing how to create a zip file containing a compressed text file:
 * ```
 * // use a BlobWriter to store with a ZipWriter the zip into a Blob object
 * const blobWriter = new zip.BlobWriter("application/zip")
 * const writer = new zip.ZipWriter(blobWriter)
 *
 * // use a TextReader to read the String to add
 * await writer.add("filename.txt", new zip.TextReader("test!"))
 *
 * // close the ZipReader
 * await writer.close()
 *
 * // get the zip file as a Blob
 * const blob = await blobWriter.getData()
 * ```
 */
external class ZipWriter<T> {
    /**
     * Creates the . instance
     *
     * @param writer The . instance where the zip content will be written.
     * @param options The options.
     */
    constructor(writer: Writer<T>, options: ZipWriterConstructorOptions? = definedExternally)

    /**
     * Creates the . instance
     *
     * @param writer The . instance where the zip content will be written.
     * @param options The options.
     */
    constructor(writer: WritableWriter<T>, options: ZipWriterConstructorOptions? = definedExternally)

    /**
     * Creates the . instance
     *
     * @param writer The . instance where the zip content will be written.
     * @param options The options.
     */
    constructor(writer: WritableStream<T>, options: ZipWriterConstructorOptions? = definedExternally)

    /**
     * Creates the . instance
     *
     * @param writer The . instance where the zip content will be written.
     * @param options The options.
     */
    constructor(
        writer: AsyncGenerator<Writer<T>, Boolean, dynamic>,
        options: ZipWriterConstructorOptions? = definedExternally
    )

    /**
     * Creates the . instance
     *
     * @param writer The . instance where the zip content will be written.
     * @param options The options.
     */
    constructor(
        writer: AsyncGenerator<WritableWriter<T>, Boolean, dynamic>,
        options: ZipWriterConstructorOptions? = definedExternally
    )

    /**
     * Creates the . instance
     *
     * @param writer The . instance where the zip content will be written.
     * @param options The options.
     */
    constructor(
        writer: AsyncGenerator<WritableStream<T>, Boolean, dynamic>,
        options: ZipWriterConstructorOptions? = definedExternally
    )

    /**
     * `true` if the zip contains at least one entry that has been partially written.
     */
    val hasCorruptedEntries: Boolean?

    /**
     * Adds an entry into the zip file
     *
     * @param filename The filename of the entry.
     * @param reader The  . instance used to read the content of the entry.
     * @param options The options.
     * @returns A promise resolving to an . instance.
     */
    @JsName("add")
    suspend fun <O> add(
        filename: String,
        reader: Reader<O>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): EntryMetaData

    @JsName("add")
    fun <O> addAsync(
        filename: String,
        reader: Reader<O>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): Promise<EntryMetaData>

    /**
     * Adds an entry into the zip file
     *
     * @param filename The filename of the entry.
     * @param reader The  . instance used to read the content of the entry.
     * @param options The options.
     * @returns A promise resolving to an . instance.
     */
    @JsName("add")
    suspend fun <O> add(
        filename: String,
        reader: ReadableReader<O>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): EntryMetaData

    @JsName("add")
    fun <O> addAsync(
        filename: String,
        reader: ReadableReader<O>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): Promise<EntryMetaData>

    /**
     * Adds an entry into the zip file
     *
     * @param filename The filename of the entry.
     * @param reader The  . instance used to read the content of the entry.
     * @param options The options.
     * @returns A promise resolving to an . instance.
     */
    @JsName("add")
    suspend fun <O> add(
        filename: String,
        reader: ReadableStream<O>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): EntryMetaData

    @JsName("add")
    fun <O> addAsync(
        filename: String,
        reader: ReadableStream<O>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): Promise<EntryMetaData>

    /**
     * Adds an entry into the zip file
     *
     * @param filename The filename of the entry.
     * @param reader The  . instance used to read the content of the entry.
     * @param options The options.
     * @returns A promise resolving to an . instance.
     */
    @JsName("add")
    suspend fun <O> add(
        filename: String,
        reader: Array<Reader<O>>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): EntryMetaData

    @JsName("add")
    fun <O> addAsync(
        filename: String,
        reader: Array<Reader<O>>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): Promise<EntryMetaData>

    /**
     * Adds an entry into the zip file
     *
     * @param filename The filename of the entry.
     * @param reader The  . instance used to read the content of the entry.
     * @param options The options.
     * @returns A promise resolving to an . instance.
     */
    @JsName("add")
    suspend fun <O> add(
        filename: String,
        reader: Array<ReadableReader<O>>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): EntryMetaData

    @JsName("add")
    fun <O> addAsync(
        filename: String,
        reader: Array<ReadableReader<O>>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): Promise<EntryMetaData>

    /**
     * Adds an entry into the zip file
     *
     * @param filename The filename of the entry.
     * @param reader The  . instance used to read the content of the entry.
     * @param options The options.
     * @returns A promise resolving to an . instance.
     */
    @JsName("add")
    suspend fun <O> add(
        filename: String,
        reader: Array<ReadableStream<O>>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): EntryMetaData

    @JsName("add")
    fun <O> addAsync(
        filename: String,
        reader: Array<ReadableStream<O>>? = definedExternally,
        options: ZipWriterAddDataOptions? = definedExternally
    ): Promise<EntryMetaData>

    /**
     * Writes the entries directory, writes the global comment, and returns the content of the zip file
     *
     * @param comment The global comment of the zip file.
     * @param options The options.
     * @returns The content of the zip file.
     */
    @JsName("close")
    suspend fun close(
        comment: Uint8Array<out ArrayBufferLike>? = definedExternally,
        options: ZipWriterCloseOptions? = definedExternally
    ): T

    @JsName("close")
    fun closeAsync(
        comment: Uint8Array<out ArrayBufferLike>? = definedExternally,
        options: ZipWriterCloseOptions? = definedExternally
    ): Promise<T>
}

/**
 * Represents the options passed to ..
 */
external interface ZipWriterAddDataOptions : ZipWriterConstructorOptions, EntryDataOnprogressOptions,
    WorkerConfiguration {
    /**
     * `true` if the entry is a directory.
     *
     * @defaultValue false
     */
    var directory: Boolean?

    /**
     * `true` if the entry is an executable file.
     *
     * @defaultValue false
     */
    var executable: Boolean?

    /**
     * The comment of the entry.
     */
    var comment: String?

    /**
     * The extra field of the entry.
     */
    var extraField: Map<Number, Uint8Array<out ArrayBufferLike>>?

    /**
     * The uncompressed size of the entry. This option is ignored if the . option is not set to `true`.
     */
    var uncompressedSize: Number?

    /**
     * The signature (CRC32 checksum) of the content. This option is ignored if the . option is not set to `true`.
     */
    var signature: Number?
}

/**
 * Represents the options passed to  ..
 */
external interface ZipWriterCloseOptions : EntryOnprogressOptions {
    /**
     * `true` to use Zip64 to write the entries directory.
     *
     * @defaultValue false
     */
    var zip64: Boolean?

    /**
     * `true` to prevent closing of ..
     *
     * @defaultValue false
     */
    var preventClose: Boolean?
}

/**
 * Represents options passed to the constructor of ., . and `.#export*`.
 */
external interface ZipWriterConstructorOptions {
    /**
     * `true` to use Zip64 to store the entry.
     *
     * `zip64` is automatically set to `true` when necessary (e.g. compressed data larger than 4GB or with unknown size).
     *
     * @defaultValue false
     */
    var zip64: Boolean?

    /**
     * `true` to prevent closing of ..
     *
     * @defaultValue false
     */
    var preventClose: Boolean?

    /**
     * The level of compression.
     *
     * The minimum value is 0 and means that no compression is applied. The maximum value is 9.
     *
     * @defaultValue 5
     */
    var level: Number?

    /**
     * `true` to write entry data in a buffer before appending it to the zip file.
     *
     * `bufferedWrite` is automatically set to `true` when compressing more than one entry in parallel.
     *
     * @defaultValue false
     */
    var bufferedWrite: Boolean?

    /**
     * `true` to keep the order of the entry physically in the zip file.
     *
     * When set to `true`, the use of web workers will be improved. However, it also prevents files larger than 4GB from being created without setting the `zip64` option to `true` explicitly.
     * Another solution to improve the use of web workers is to add entries from smallest to largest in uncompressed size.
     *
     * @defaultValue true
     */
    var keepOrder: Boolean?

    /**
     * The password used to encrypt the content of the entry.
     */
    var password: String?

    /**
     * The password used to encrypt the content of the entry (raw).
     */
    var rawPassword: Uint8Array<out ArrayBufferLike>?

    /**
     * The encryption strength (AES):
     * - 1: 128-bit encryption key
     * - 2: 192-bit encryption key
     * - 3: 256-bit encryption key
     *
     * @defaultValue 3
     */
    var encryptionStrength: Number

    /**
     * The `AbortSignal` instance used to cancel the compression.
     */
    var signal: AbortSignal?

    /**
     * The last modification date.
     *
     * @defaultValue The current date.
     */
    var lastModDate: Date?

    /**
     * The last access date.
     *
     * This option is ignored if the . option is set to `false`.
     *
     * @defaultValue The current date.
     */
    var lastAccessDate: Date?

    /**
     * The creation date.
     *
     * This option is ignored if the . option is set to `false`.
     *
     * @defaultValue The current date.
     */
    var creationDate: Date?

    /**
     * `true` to store extended timestamp extra fields.
     *
     * When set to `false`, the maximum last modification date cannot exceed November 31, 2107 and the maximum accuracy is 2 seconds.
     *
     * @defaultValue true
     */
    var extendedTimestamp: Boolean?

    /**
     * `true` to use the ZipCrypto algorithm to encrypt the content of the entry.
     *
     * It is not recommended to set `zipCrypto` to `true` because the ZipCrypto encryption can be easily broken.
     *
     * @defaultValue false
     */
    var zipCrypto: Boolean?

    /**
     * The "Version" field.
     */
    var version: Number?

    /**
     * The "Version made by" field.
     *
     * @defaultValue 20
     */
    var versionMadeBy: Number?

    /**
     * `true` to mark the file names as UTF-8 setting the general purpose bit 11 in the header (see Appendix D - Language Encoding (EFS)), `false` to mark the names as compliant with the original IBM Code Page 437.
     *
     * Note that this does not ensure that the file names are in the correct encoding.
     *
     * @defaultValue true
     */
    var useUnicodeFileNames: Boolean?

    /**
     * `true` to add a data descriptor.
     *
     * When set to `false`, the . option  will automatically be set to `true`.
     *
     * @defaultValue true
     */
    var dataDescriptor: Boolean?

    /**
     * `true` to add the signature of the data descriptor.
     *
     * @defaultValue false
     */
    var dataDescriptorSignature: Boolean?

    /**
     * `true` to write . in MS-DOS format for folder entries.
     *
     * @defaultValue false
     */
    var msDosCompatible: Boolean?

    /**
     * The external file attribute.
     *
     * @defaultValue 0
     */
    var externalFileAttributes: Number?

    /**
     * The internal file attribute.
     *
     * @defaultValue 0
     */
    var internalFileAttributes: Number?

    /**
     * `false` to never write disk numbers in zip64 data.
     *
     * @defaultValue true
     */
    var supportZip64SplitFile: Boolean?

    /**
     * `true`to produce zip files compatible with the USDZ specification.
     *
     * @defaultValue false
     */
    var usdz: Boolean?

    /**
     * `true` to write the data as-is without compressing it and without crypting it.
     */
    var passThrough: Boolean?

    /**
     * `true` to write encrypted data when `passThrough` is set to `true`.
     */
    var encrypted: Boolean?

    /**
     * The offset of the first entry in the zip file.
     */
    var offset: Number?

    /**
     * The compression method (e.g. 8 for DEFLATE, 0 for STORE).
     */
    var compressionMethod: Number?

    /**
     * The function called for encoding the filename and the comment of the entry.
     *
     * @param text The text to encode.
     * @returns The encoded text or `undefined` if the text should be encoded by zip.js.
     */
    var encodeText: ((text: String) -> Uint8Array<out ArrayBufferLike>?)?
}

/**
 * Represents options passed to ., [ZipWriter.add] and `..export*`.
 */
external interface EntryDataOnprogressOptions {
    /**
     * The function called when starting compression/decompression.
     *
     * @param total The total number of bytes.
     * @returns An empty promise or `undefined`.
     */
    var onstart: ((total: Number) -> Promise<Void>?)?

    /**
     * The function called during compression/decompression.
     *
     * @param progress The current progress in bytes.
     * @param total The total number of bytes.
     * @returns An empty promise or `undefined`.
     */
    var onprogress: ((progress: Number, total: Number) -> Promise<Void>?)?

    /**
     * The function called when ending compression/decompression.
     *
     * @param computedSize The total number of bytes (computed).
     * @returns An empty promise or `undefined`.
     */
    var onend: ((computedSize: Number) -> Promise<Void>?)?
}

/**
 * Represents options passed to ., ., and ..
 */
external interface EntryOnprogressOptions {
    /**
     * The function called each time an entry is read/written.
     *
     * @param progress The entry index.
     * @param total The total number of entries.
     * @param entry The entry being read/written.
     * @returns An empty promise or `undefined`.
     */
    var onprogress: ((progress: Number, total: Number, entry: EntryMetaData) -> Promise<Void>?)?
}

/**
 * Represents an entry in a zip file (Filesystem API).
 */
open external class ZipEntry {
    /**
     * The relative filename of the entry.
     */
    var name: String

    /**
     * The underlying . instance.
     */
    var data: EntryMetaData?

    /**
     * The ID of the instance.
     */
    var id: Number

    /**
     * The parent directory of the entry.
     */
    var parent: ZipEntry?

    /**
     * The uncompressed size of the content.
     */
    var uncompressedSize: Number

    /**
     * The children of the entry.
     */
    var children: Array<ZipEntry>

    /**
     * Clones the entry
     *
     * @param deepClone `true` to clone all the descendants.
     */
    fun clone(deepClone: Boolean? = definedExternally): ZipEntry

    /**
     * Returns the full filename of the entry
     */
    fun getFullname(): String

    /**
     * Returns the filename of the entry relative to a parent directory
     */
    fun getRelativeName(ancestor: ZipDirectoryEntry): String

    /**
     * Tests if a . instance is an ancestor of the entry
     *
     * @param ancestor The . instance.
     */
    fun isDescendantOf(ancestor: ZipDirectoryEntry): Boolean

    /**
     * Tests if the entry or any of its children is password protected
     */
    fun isPasswordProtected(): Boolean

    /**
     * Tests the password on the entry and all children if any, returns `true` if the entry is not password protected
     */
    @JsName("checkPassword")
    suspend fun checkPassword(
        password: String,
        options: EntryGetDataOptions? = definedExternally,
    ): Boolean

    @JsName("checkPassword")
    fun checkPasswordAsync(
        password: String,
        options: EntryGetDataOptions? = definedExternally,
    ): Promise<Boolean>

    /**
     * Set the name of the entry
     *
     * @param name The new name of the entry.
     */
    fun rename(name: String)
}

/**
 * Represents a file entry in the zip (Filesystem API).
 */
external class ZipFileEntry<R, W> : ZipEntry {
    /**
     * `void` for . instances.
     */
    val directory: dynamic

    /**
     * The . instance used to read the content of the entry.
     *
     * @type Reader<R>
     * @type ReadableReader<R>
     * @type ReadableStream<R>
     * @type Array<Reader<R>>
     * @type Array<ReadableReader<R>>
     * @type Array<ReadableStream<R>>
     */
    val reader: dynamic

    /**
     * The . instance used to write the content of the entry.
     *
     * @type Writer<W>
     * @type WritableWriter<W>
     * @type WritableStream<W>
     * @type AsyncGenerator<Writer<W>>
     * @type AsyncGenerator<WritableWriter<W>>
     * @type AsyncGenerator<WritableStream<W>>
     */
    val writer: dynamic

    /**
     * Retrieves the text content of the entry as a `string`
     *
     * @param encoding The encoding of the text.
     * @param options The options.
     * @returns A promise resolving to a `string`.
     */
    @JsName("getText")
    suspend fun getText(
        encoding: String? = definedExternally,
        options: EntryGetDataOptions? = definedExternally
    ): String

    @JsName("getText")
    fun getTextAsync(
        encoding: String? = definedExternally,
        options: EntryGetDataOptions? = definedExternally
    ): Promise<String>

    /**
     * Retrieves the content of the entry as a `Blob` instance
     *
     * @param mimeType The MIME type of the content.
     * @param options The options.
     * @returns A promise resolving to a `Blob` instance.
     */
    @JsName("getBlob")
    suspend fun getBlob(
        mimeType: String? = definedExternally,
        options: EntryGetDataOptions? = definedExternally
    ): Blob

    @JsName("getBlob")
    fun getBlobAsync(
        mimeType: String? = definedExternally,
        options: EntryGetDataOptions? = definedExternally
    ): Promise<Blob>

    /**
     * Retrieves the content of the entry as as a Data URI `string` encoded in Base64
     *
     * @param mimeType The MIME type of the content.
     * @param options The options.
     * @returns A promise resolving to a Data URI `string` encoded in Base64.
     */
    @JsName("getData64URI")
    suspend fun getData64URI(
        mimeType: String? = definedExternally,
        options: EntryGetDataOptions? = definedExternally,
    ): String

    @JsName("getData64URI")
    fun getData64URIAsync(
        mimeType: String? = definedExternally,
        options: EntryGetDataOptions? = definedExternally,
    ): Promise<String>

    /**
     * Retrieves the content of the entry as a `Uint8Array` instance
     *
     * @param options The options.
     * @returns A promise resolving to a `Uint8Array` instance.
     */
    @JsName("getCompatibleUint8Array")
    suspend fun getCompatibleUint8Array(options: EntryGetDataOptions? = definedExternally): Uint8Array<out ArrayBufferLike>

    @JsName("getCompatibleUint8Array")
    fun getCompatibleUint8ArrayAsync(options: EntryGetDataOptions? = definedExternally): Promise<Uint8Array<out ArrayBufferLike>>

    /**
     * Retrieves the content of the entry via a `WritableStream` instance
     *
     * @param writable The `WritableStream` instance.
     * @param options The options.
     * @returns A promise resolving to the `WritableStream` instance.
     */
    @JsName("getWritable")
    suspend fun getWritable(
        writable: WritableStream<W>? = definedExternally,
        options: EntryGetDataOptions? = definedExternally
    ): WritableStream<W>

    @JsName("getWritable")
    fun getWritableAsync(
        writable: WritableStream<W>? = definedExternally,
        options: EntryGetDataOptions? = definedExternally
    ): Promise<WritableStream<W>>

    /**
     * Retrieves the content of the entry via a . instance
     *
     * @param writer The . instance.
     * @param options The options.
     * @returns A promise resolving to data associated to the . instance.
     */
    @JsName("getData")
    suspend fun getData(writer: Writer<W>, options: EntryGetDataOptions? = definedExternally): dynamic

    @JsName("getData")
    fun getDataAsync(writer: Writer<W>, options: EntryGetDataOptions? = definedExternally): Promise<dynamic>

    /**
     * Retrieves the content of the entry via a . instance
     *
     * @param writer The . instance.
     * @param options The options.
     * @returns A promise resolving to data associated to the . instance.
     */
    @JsName("getData")
    suspend fun getData(writer: WritableWriter<W>, options: EntryGetDataOptions? = definedExternally): dynamic

    @JsName("getData")
    fun getDataAsync(writer: WritableWriter<W>, options: EntryGetDataOptions? = definedExternally): Promise<dynamic>

    /**
     * Retrieves the content of the entry via a . instance
     *
     * @param writer The . instance.
     * @param options The options.
     * @returns A promise resolving to data associated to the . instance.
     */
    @JsName("getData")
    suspend fun getData(writer: WritableStream<W>, options: EntryGetDataOptions? = definedExternally): dynamic

    @JsName("getData")
    fun getDataAsync(writer: WritableStream<W>, options: EntryGetDataOptions? = definedExternally): Promise<dynamic>

    /**
     * Retrieves the content of the entry via a . instance
     *
     * @param writer The . instance.
     * @param options The options.
     * @returns A promise resolving to data associated to the . instance.
     */
    @JsName("getData")
    suspend fun getData(
        writer: AsyncGenerator<Writer<W>, dynamic, dynamic>,
        options: EntryGetDataOptions? = definedExternally
    ): dynamic

    @JsName("getData")
    fun getDataAsync(
        writer: AsyncGenerator<Writer<W>, dynamic, dynamic>,
        options: EntryGetDataOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Retrieves the content of the entry via a . instance
     *
     * @param writer The . instance.
     * @param options The options.
     * @returns A promise resolving to data associated to the . instance.
     */
    @JsName("getData")
    suspend fun getData(
        writer: AsyncGenerator<WritableWriter<W>, dynamic, dynamic>,
        options: EntryGetDataOptions? = definedExternally
    ): dynamic

    @JsName("getData")
    fun getDataAsync(
        writer: AsyncGenerator<WritableWriter<W>, dynamic, dynamic>,
        options: EntryGetDataOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Retrieves the content of the entry via a . instance
     *
     * @param writer The . instance.
     * @param options The options.
     * @returns A promise resolving to data associated to the . instance.
     */
    @JsName("getData")
    suspend fun getData(
        writer: AsyncGenerator<WritableStream<W>, dynamic, dynamic>,
        options: EntryGetDataOptions? = definedExternally
    ): dynamic

    @JsName("getData")
    fun getDataAsync(
        writer: AsyncGenerator<WritableStream<W>, dynamic, dynamic>,
        options: EntryGetDataOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Replaces the content of the entry with a `Blob` instance
     *
     * @param blob The `Blob` instance.
     */
    fun replaceBlob(blob: Blob)

    /**
     * Replaces the content of the entry with a `string`
     *
     * @param text The `string`.
     */
    fun replaceText(text: String)

    /**
     * Replaces the content of the entry with a Data URI `string` encoded in Base64
     *
     * @param dataURI The Data URI `string` encoded in Base64.
     */
    fun replaceData64URI(dataURI: String)

    /**
     * Replaces the content of the entry with a `Uint8Array` instance
     *
     * @param array The `Uint8Array` instance.
     */
    fun replaceCompatibleUint8Array(array: Uint8Array<out ArrayBufferLike>)

    /**
     * Replaces the content of the entry with a `ReadableStream` instance
     *
     * @param readable The `ReadableStream` instance.
     */
    fun replaceReadable(readable: ReadableStream<R>)
}

/**
 * Represents a directory entry in the zip (Filesystem API).
 */
open external class ZipDirectoryEntry : ZipEntry {
    /**
     * `true` for  . instances.
     */
    val directory: dynamic

    /**
     * Gets a [ZipEntry] child instance from its relative filename
     *
     * @param name The relative filename.
     * @returns A . or a . instance (use the . and . properties to differentiate entries).
     */
    fun getChildByName(name: String): ZipEntry?

    /**
     * Adds a directory
     *
     * @param name The relative filename of the directory.
     * @param options The options.
     * @returns A . instance.
     */
    fun addDirectory(
        name: String,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipDirectoryEntry

    /**
     * Adds an entry with content provided as text
     *
     * @param name The relative filename of the entry.
     * @param text The text.
     * @param options The options.
     * @returns A . instance.
     */
    fun addText(
        name: String,
        text: String,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipFileEntry<String, String>

    /**
     * Adds a entry entry with content provided as a `Blob` instance
     *
     * @param name The relative filename of the entry.
     * @param blob The `Blob` instance.
     * @param options The options.
     * @returns A . instance.
     */
    fun addBlob(
        name: String,
        blob: Blob,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipFileEntry<Blob, Blob>

    /**
     * Adds a entry entry with content provided as a Data URI `string` encoded in Base64
     *
     * @param name The relative filename of the entry.
     * @param dataURI The Data URI `string` encoded in Base64.
     * @param options The options.
     * @returns A . instance.
     */
    fun addData64URI(
        name: String,
        dataURI: String,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipFileEntry<String, String>

    /**
     * Adds an entry with content provided as a `Uint8Array` instance
     *
     * @param name The relative filename of the entry.
     * @param array The `Uint8Array` instance.
     * @param options The options.
     * @returns A . instance.
     */
    fun addCompatibleUint8Array(
        name: String,
        array: Uint8Array<out ArrayBufferLike>,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipFileEntry<Uint8Array<out ArrayBufferLike>, Uint8Array<out ArrayBufferLike>>

    /**
     * Adds an entry with content fetched from a URL
     *
     * @param name The relative filename of the entry.
     * @param url The URL.
     * @param options The options.
     * @returns A . instance.
     */
    fun addHttpContent(
        name: String,
        url: String,
        options: ZipWriterAddHttpOptions? = definedExternally,
    ): ZipFileEntry<String, Void>

    /**
     * Adds a entry entry with content provided via a `ReadableStream` instance
     *
     * @param name The relative filename of the entry.
     * @param readable The `ReadableStream` instance.
     * @param options The options.
     * @returns A . instance.
     */
    fun <T> addReadable(
        name: String,
        readable: ReadableStream<T>,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipFileEntry<ReadableStream<T>, Void>

    /**
     * Adds an entry with content provided via a `File` instance
     *
     * @param file The `File` instance.
     * @param options The options.
     * @returns A promise resolving to a . or a . instance.
     */
    @JsName("addFile")
    suspend fun addFile(
        file: File,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): ZipEntry

    @JsName("addFile")
    fun addFileAsync(
        file: File,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): Promise<ZipEntry>

    /**
     * Adds an entry with content provided via a `FileSystemEntry` instance
     *
     * @param fileSystemEntry The `FileSystemEntry` instance.
     * @param options The options.
     * @returns A promise resolving to an array of . or a . instances.
     */
    @JsName("addFileSystemEntry")
    suspend fun addFileSystemEntry(
        fileSystemEntry: FileSystemEntryLike,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("addFileSystemEntry")
    fun addFileSystemEntryAsync(
        fileSystemEntry: FileSystemEntryLike,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Adds an entry with content provided via a `FileSystemHandle` instance
     *
     * @param fileSystemHandle The `fileSystemHandle` instance.
     * @param options The options.
     * @returns A promise resolving to an array of . or a . instances.
     */
    @JsName("addFileSystemHandle")
    suspend fun addFileSystemHandle(
        fileSystemHandle: FileSystemHandleLike,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("addFileSystemHandle")
    fun addFileSystemHandleAsync(
        fileSystemHandle: FileSystemHandleLike,
        options: ZipWriterAddDataOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided as a `Blob` instance into the entry
     *
     * @param blob The `Blob` instance.
     * @param options  The options.
     */
    @JsName("importBlob")
    suspend fun importBlob(
        blob: Blob,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("importBlob")
    fun importBlobAsync(
        blob: Blob,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided as a Data URI `string` encoded in Base64 into the entry
     *
     * @param dataURI The Data URI `string` encoded in Base64.
     * @param options  The options.
     */
    @JsName("importData64URI")
    suspend fun importData64URI(
        dataURI: String,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("importData64URI")
    fun importData64URIAsync(
        dataURI: String,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided as a `Uint8Array` instance into the entry
     *
     * @param array The `Uint8Array` instance.
     * @param options  The options.
     */
    @JsName("importCompatibleUint8Array")
    suspend fun importCompatibleUint8Array(
        array: Uint8Array<out ArrayBufferLike>,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("importCompatibleUint8Array")
    fun importCompatibleUint8ArrayAsync(
        array: Uint8Array<out ArrayBufferLike>,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file fetched from a URL into the entry
     *
     * @param url The URL.
     * @param options  The options.
     */
    @JsName("importHttpContent")
    suspend fun importHttpContent(
        url: String,
        options: ZipDirectoryEntryImportHttpOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("importHttpContent")
    fun importHttpContentAsync(
        url: String,
        options: ZipDirectoryEntryImportHttpOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a `ReadableStream` instance into the entry
     *
     * @param readable The `ReadableStream` instance.
     * @param options  The options.
     */
    @JsName("importReadable")
    suspend fun <T> importReadable(
        readable: ReadableStream<T>,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Array<ZipEntry>

    @JsName("importReadable")
    fun <T> importReadableAsync(
        readable: ReadableStream<T>,
        options: ZipReaderConstructorOptions? = definedExternally,
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a custom . instance into the entry
     *
     * @param reader The . instance.
     * @param options  The options.
     */
    @JsName("importZip")
    suspend fun <T> importZip(
        reader: Reader<T>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Array<ZipEntry>

    @JsName("importZip")
    fun <T> importZipAsync(
        reader: Reader<T>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a custom . instance into the entry
     *
     * @param reader The . instance.
     * @param options  The options.
     */
    @JsName("importZip")
    suspend fun <T> importZip(
        reader: ReadableReader<T>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Array<ZipEntry>

    @JsName("importZip")
    fun <T> importZipAsync(
        reader: ReadableReader<T>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a custom . instance into the entry
     *
     * @param reader The . instance.
     * @param options  The options.
     */
    @JsName("importZip")
    suspend fun <T> importZip(
        reader: ReadableStream<T>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Array<ZipEntry>

    @JsName("importZip")
    fun <T> importZipAsync(
        reader: ReadableStream<T>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a custom . instance into the entry
     *
     * @param reader The . instance.
     * @param options  The options.
     */
    @JsName("importZip")
    suspend fun <T> importZip(
        reader: Array<Reader<T>>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Array<ZipEntry>

    @JsName("importZip")
    fun <T> importZipAsync(
        reader: Array<Reader<T>>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a custom . instance into the entry
     *
     * @param reader The . instance.
     * @param options  The options.
     */
    @JsName("importZip")
    suspend fun <T> importZip(
        reader: Array<ReadableReader<T>>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Array<ZipEntry>

    @JsName("importZip")
    fun <T> importZipAsync(
        reader: Array<ReadableReader<T>>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Promise<Array<ZipEntry>>

    /**
     * Extracts a zip file provided via a custom . instance into the entry
     *
     * @param reader The . instance.
     * @param options  The options.
     */
    @JsName("importZip")
    suspend fun <T> importZip(
        reader: Array<ReadableStream<T>>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Array<ZipEntry>

    @JsName("importZip")
    fun <T> importZipAsync(
        reader: Array<ReadableStream<T>>,
        options: ZipReaderConstructorOptions? = definedExternally
    ): Promise<Array<ZipEntry>>

    /**
     * Returns a `Blob` instance containing a zip file of the entry and its descendants
     *
     * @param options  The options.
     * @returns A promise resolving to the `Blob` instance.
     */
    @JsName("exportBlob")
    suspend fun exportBlob(
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Blob

    @JsName("exportBlob")
    fun exportBlobAsync(
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<Blob>

    /**
     * Returns a Data URI `string` encoded in Base64 containing a zip file of the entry and its descendants
     *
     * @param options  The options.
     * @returns A promise resolving to the Data URI `string` encoded in Base64.
     */
    @JsName("exportData64URI")
    suspend fun exportData64URI(
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): String

    @JsName("exportData64URI")
    fun exportData64URIAsync(
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<String>

    /**
     * Returns a `Uint8Array` instance containing a zip file of the entry and its descendants
     *
     * @param options  The options.
     * @returns A promise resolving to the `Uint8Array` instance.
     */
    @JsName("exportCompatibleUint8Array")
    suspend fun exportCompatibleUint8Array(
        options: ZipDirectoryEntryExportOptions? = definedExternally,
    ): Uint8Array<out ArrayBufferLike>

    @JsName("exportCompatibleUint8Array")
    fun exportCompatibleUint8ArrayAsync(
        options: ZipDirectoryEntryExportOptions? = definedExternally,
    ): Promise<Uint8Array<out ArrayBufferLike>>

    /**
     * Creates a zip file via a `WritableStream` instance containing the entry and its descendants
     *
     * @param writable The `WritableStream` instance.
     * @param options  The options.
     * @returns A promise resolving to the `Uint8Array` instance.
     */
    @JsName("exportWritable")
    suspend fun <T> exportWritable(
        writable: WritableStream<T>? = definedExternally,
        options: ZipDirectoryEntryExportOptions? = definedExternally,
    ): WritableStream<T>

    @JsName("exportWritable")
    fun <T> exportWritableAsync(
        writable: WritableStream<T>? = definedExternally,
        options: ZipDirectoryEntryExportOptions? = definedExternally,
    ): Promise<WritableStream<T>>

    /**
     * Creates a zip file via a custom . instance containing the entry and its descendants
     *
     * @param writer The . instance.
     * @param options  The options.
     * @returns A promise resolving to the data.
     */
    @JsName("exportZip")
    suspend fun <T> exportZip(
        writer: Writer<T>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): dynamic

    @JsName("exportZip")
    fun <T> exportZipAsync(
        writer: Writer<T>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Creates a zip file via a custom . instance containing the entry and its descendants
     *
     * @param writer The . instance.
     * @param options  The options.
     * @returns A promise resolving to the data.
     */
    @JsName("exportZip")
    suspend fun <T> exportZip(
        writer: WritableWriter<T>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): dynamic

    @JsName("exportZip")
    fun <T> exportZipAsync(
        writer: WritableWriter<T>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Creates a zip file via a custom . instance containing the entry and its descendants
     *
     * @param writer The . instance.
     * @param options  The options.
     * @returns A promise resolving to the data.
     */
    @JsName("exportZip")
    suspend fun <T> exportZip(
        writer: WritableStream<T>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): dynamic

    @JsName("exportZip")
    fun <T> exportZipAsync(
        writer: WritableStream<T>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Creates a zip file via a custom . instance containing the entry and its descendants
     *
     * @param writer The . instance.
     * @param options  The options.
     * @returns A promise resolving to the data.
     */
    @JsName("exportZip")
    suspend fun <T> exportZip(
        writer: AsyncGenerator<Writer<T>, dynamic, dynamic>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): dynamic

    @JsName("exportZip")
    fun <T> exportZipAsync(
        writer: AsyncGenerator<Writer<T>, dynamic, dynamic>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Creates a zip file via a custom . instance containing the entry and its descendants
     *
     * @param writer The . instance.
     * @param options  The options.
     * @returns A promise resolving to the data.
     */
    @JsName("exportZip")
    suspend fun <T> exportZip(
        writer: AsyncGenerator<WritableWriter<T>, dynamic, dynamic>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): dynamic

    @JsName("exportZip")
    fun <T> exportZipAsync(
        writer: AsyncGenerator<WritableWriter<T>, dynamic, dynamic>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<dynamic>

    /**
     * Creates a zip file via a custom . instance containing the entry and its descendants
     *
     * @param writer The . instance.
     * @param options  The options.
     * @returns A promise resolving to the data.
     */
    @JsName("exportZip")
    suspend fun <T> exportZip(
        writer: AsyncGenerator<WritableStream<T>, dynamic, dynamic>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): dynamic

    @JsName("exportZip")
    fun <T> exportZipAsync(
        writer: AsyncGenerator<WritableStream<T>, dynamic, dynamic>,
        options: ZipDirectoryEntryExportOptions? = definedExternally
    ): Promise<dynamic>
}

external interface ZipWriterAddHttpOptions : HttpOptions, ZipWriterAddDataOptions

/**
 * Represents the options passed to ..
 */
external interface ZipDirectoryEntryImportHttpOptions : ZipReaderConstructorOptions, HttpOptions

/**
 * Represents the options passed to `.#export*()`.
 */
external interface ZipDirectoryEntryExportOptions : ZipWriterConstructorOptions, EntryDataOnprogressOptions {
    /**
     * `true` to use filenames relative to the entry instead of full filenames.
     */
    var relativePath: Boolean?

    /**
     * The MIME type of the exported data when relevant.
     */
    var mimeType: String?

    /**
     * The options passed to the Reader instances
     */
    var readerOptions: ZipReaderConstructorOptions?
}

/**
 * Represents a Filesystem instance.
 */
external class FS : ZipDirectoryEntry {
    /**
     * The root directory.
     */
    val root: ZipDirectoryEntry

    /**
     * Removes a . instance and its children
     *
     * @param entry The . instance to remove.
     */
    fun remove(entry: ZipEntry)

    /**
     * Moves a . instance and its children into a . instance
     *
     * @param entry The . instance to move.
     * @param destination The . instance.
     */
    fun move(entry: ZipEntry, destination: ZipDirectoryEntry)

    /**
     * Returns a . instance from its full filename
     *
     * @param fullname The full filename.
     * @returns The . instance.
     */
    fun find(fullname: String): ZipEntry?

    /**
     * Returns a . instance from the value of .
     *
     * @param id The id of the . instance.
     * @returns The . instance.
     */
    fun getById(id: Number): ZipEntry?
}

external interface FsConstants {
    /**
     * The Filesystem constructor.
     *
     * @defaultValue .
     */
    var FS: () -> FS

    /**
     * The . constructor.
     *
     * @defaultValue .
     */
    var ZipDirectoryEntry: () -> ZipDirectoryEntry

    /**
     * The . constructor.
     *
     * @defaultValue .
     */
    var ZipFileEntry: () -> ZipFileEntry<dynamic, dynamic>
}

/**
 * The Filesystem API.
 */
external var fs: FsConstants

// The error messages.
/**
 * HTTP range error
 */
external val ERR_HTTP_RANGE: String

/**
 * Zip format error
 */
external val ERR_BAD_FORMAT: String

/**
 * End of Central Directory Record not found error
 */
external val ERR_EOCDR_NOT_FOUND: String

/**
 * Zip64 End of Central Directory Locator not found error
 */
external val ERR_EOCDR_LOCATOR_ZIP64_NOT_FOUND: String

/**
 * Central Directory not found error
 */
external val ERR_CENTRAL_DIRECTORY_NOT_FOUND: String

/**
 * Local file header not found error
 */
external val ERR_LOCAL_FILE_HEADER_NOT_FOUND: String

/**
 * Extra field Zip64 not found error
 */
external val ERR_EXTRAFIELD_ZIP64_NOT_FOUND: String

/**
 * Encrypted entry error
 */
external val ERR_ENCRYPTED: String

/**
 * Unsupported encryption error
 */
external val ERR_UNSUPPORTED_ENCRYPTION: String

/**
 * Unsupported compression error
 */
external val ERR_UNSUPPORTED_COMPRESSION: String

/**
 * Invalid signature error
 */
external val ERR_INVALID_SIGNATURE: String

/**
 * Invalid password error
 */
external val ERR_INVALID_PASSWORD: String

/**
 * Duplicate entry error
 */
external val ERR_DUPLICATED_NAME: String

/**
 * Invalid comment error
 */
external val ERR_INVALID_COMMENT: String

/**
 * Invalid entry name error
 */
external val ERR_INVALID_ENTRY_NAME: String

/**
 * Invalid entry comment error
 */
external val ERR_INVALID_ENTRY_COMMENT: String

/**
 * Invalid version error
 */
external val ERR_INVALID_VERSION: String

/**
 * Invalid extra field type error
 */
external val ERR_INVALID_EXTRAFIELD_TYPE: String

/**
 * Invalid extra field data error
 */
external val ERR_INVALID_EXTRAFIELD_DATA: String

/**
 * Invalid encryption strength error
 */
external val ERR_INVALID_ENCRYPTION_STRENGTH: String

/**
 * Invalid format error
 */
external val ERR_UNSUPPORTED_FORMAT: String

/**
 * Split zip file error
 */
external val ERR_SPLIT_ZIP_FILE: String

/**
 * Iteration completed too soon error
 */
external val ERR_ITERATOR_COMPLETED_TOO_SOON: String

/**
 * Undefined uncompressed size error
 */
external val ERR_UNDEFINED_UNCOMPRESSED_SIZE: String

/**
 * Writer not initialized error
 */
external val ERR_WRITER_NOT_INITIALIZED: String
