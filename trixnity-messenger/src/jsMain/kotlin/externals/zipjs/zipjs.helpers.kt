@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("unused", "PropertyName", "KDocUnresolvedReference")

import externals.zipjs.Configuration
import externals.zipjs.EntryDataOnprogressOptions
import externals.zipjs.EntryExtraField
import externals.zipjs.EntryGetDataCheckPasswordOptions
import externals.zipjs.EntryGetDataOptions
import externals.zipjs.EntryMetaData
import externals.zipjs.EntryOnprogressOptions
import externals.zipjs.EventBasedZipLibrary
import externals.zipjs.FsConstants
import externals.zipjs.GetEntriesOptions
import externals.zipjs.HttpOptions
import externals.zipjs.HttpRangeOptions
import externals.zipjs.ReadableReader
import externals.zipjs.Reader
import externals.zipjs.URLString
import externals.zipjs.WorkerConfiguration
import externals.zipjs.WorkerScripts
import externals.zipjs.WritableWriter
import externals.zipjs.Writer
import externals.zipjs.ZipDirectoryEntryExportOptions
import externals.zipjs.ZipDirectoryEntryImportHttpOptions
import externals.zipjs.ZipLibrary
import externals.zipjs.ZipReaderConstructorOptions
import externals.zipjs.ZipReaderGetEntriesOptions
import externals.zipjs.ZipReaderOptions
import externals.zipjs.ZipWriterAddDataOptions
import externals.zipjs.ZipWriterAddHttpOptions
import externals.zipjs.ZipWriterCloseOptions
import externals.zipjs.ZipWriterConstructorOptions
import externals.zipjs.ZipWriterStream
import js.generator.AsyncGenerator
import js.objects.unsafeJso
import web.streams.ReadableStream
import web.streams.WritableStream
import web.url.URL

@JsExport.Ignore
inline fun Configuration(
    crossinline f: Configuration.() -> Unit
): Configuration = unsafeJso(f)

@JsExport.Ignore
inline fun WorkerScripts(
    crossinline f: WorkerScripts.() -> Unit
): WorkerScripts = unsafeJso(f)

@JsExport.Ignore
inline fun WorkerConfiguration(
    crossinline f: WorkerConfiguration.() -> Unit
): WorkerConfiguration = unsafeJso(f)

@JsExport.Ignore
inline fun EventBasedZipLibrary(
    crossinline f: EventBasedZipLibrary.() -> Unit
): EventBasedZipLibrary = unsafeJso(f)

@JsExport.Ignore
inline fun ZipLibrary(
    crossinline f: ZipLibrary.() -> Unit
): ZipLibrary = unsafeJso(f)

@JsExport.Ignore
inline fun HttpOptions(
    crossinline f: HttpOptions.() -> Unit
): HttpOptions = unsafeJso(f)

@JsExport.Ignore
inline fun HttpRangeOptions(
    crossinline f: HttpRangeOptions.() -> Unit
): HttpRangeOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipReaderConstructorOptions(
    crossinline f: ZipReaderConstructorOptions.() -> Unit
): ZipReaderConstructorOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipReaderGetEntriesOptions(
    crossinline f: ZipReaderGetEntriesOptions.() -> Unit
): ZipReaderGetEntriesOptions = unsafeJso(f)

@JsExport.Ignore
inline fun GetEntriesOptions(
    crossinline f: GetEntriesOptions.() -> Unit
): GetEntriesOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipReaderOptions(
    crossinline f: ZipReaderOptions.() -> Unit
): ZipReaderOptions = unsafeJso(f)

@JsExport.Ignore
inline fun EntryMetaData(
    crossinline f: EntryMetaData.() -> Unit
): EntryMetaData = unsafeJso(f)

@JsExport.Ignore
inline fun EntryExtraField(
    crossinline f: EntryExtraField.() -> Unit
): EntryExtraField = unsafeJso(f)

@JsExport.Ignore
inline fun EntryGetDataOptions(
    crossinline f: EntryGetDataOptions.() -> Unit
): EntryGetDataOptions = unsafeJso(f)

@JsExport.Ignore
inline fun EntryGetDataCheckPasswordOptions(
    crossinline f: EntryGetDataCheckPasswordOptions.() -> Unit
): EntryGetDataCheckPasswordOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipWriterAddDataOptions(
    crossinline f: ZipWriterAddDataOptions.() -> Unit
): ZipWriterAddDataOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipWriterCloseOptions(
    crossinline f: ZipWriterCloseOptions.() -> Unit
): ZipWriterCloseOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipWriterConstructorOptions(
    crossinline f: ZipWriterConstructorOptions.() -> Unit
): ZipWriterConstructorOptions = unsafeJso(f)

@JsExport.Ignore
inline fun EntryDataOnprogressOptions(
    crossinline f: EntryDataOnprogressOptions.() -> Unit
): EntryDataOnprogressOptions = unsafeJso(f)

@JsExport.Ignore
inline fun EntryOnprogressOptions(
    crossinline f: EntryOnprogressOptions.() -> Unit
): EntryOnprogressOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipWriterAddHttpOptions(
    crossinline f: ZipWriterAddHttpOptions.() -> Unit
): ZipWriterAddHttpOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipDirectoryEntryImportHttpOptions(
    crossinline f: ZipDirectoryEntryImportHttpOptions.() -> Unit
): ZipDirectoryEntryImportHttpOptions = unsafeJso(f)

@JsExport.Ignore
inline fun ZipDirectoryEntryExportOptions(
    crossinline f: ZipDirectoryEntryExportOptions.() -> Unit
): ZipDirectoryEntryExportOptions = unsafeJso(f)

@JsExport.Ignore
inline fun FsConstants(
    crossinline f: FsConstants.() -> Unit
): FsConstants = unsafeJso(f)
