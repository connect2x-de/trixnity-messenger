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

@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("unused", "PropertyName", "KDocUnresolvedReference")

package zipjs

import js.objects.unsafeJso

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
