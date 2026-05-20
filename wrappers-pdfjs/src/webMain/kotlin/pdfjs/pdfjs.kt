/* Copyright 2024 Mozilla Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JsModule("pdfjs-dist")
@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalJsStatic::class)
@file:Suppress(
    "PropertyName",
    "LocalVariableName",
    "FunctionName",
    "unused",
    "NOTHING_TO_INLINE",
    "DuplicatedCode",
    "KDocUnresolvedReference",
)

package pdfjs

import js.array.Tuple2
import js.buffer.ArrayBufferLike
import js.buffer.BufferSource
import js.collections.JsMap
import js.collections.JsSet
import js.core.Void
import js.errors.JsError
import js.iterable.JsIterable
import js.promise.Promise
import js.typedarrays.Uint8Array
import js.typedarrays.Uint8ClampedArray
import kotlin.js.ExperimentalJsStatic
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsBoolean
import kotlin.js.JsModule
import kotlin.js.JsNumber
import kotlin.js.JsStatic
import kotlin.js.JsString
import kotlin.js.definedExternally
import web.canvas.CanvasRenderingContext2D
import web.html.HTMLCanvasElement
import web.html.HTMLDivElement
import web.html.HTMLDocument
import web.streams.ReadableStream
import web.url.URL
import web.workers.Worker

external interface RefProxy : JsAny {
    var num: JsNumber
    var gen: JsNumber
}

/** Document initialization / loading parameters object. */
external interface DocumentInitParameters : JsAny {
    /** The URL of the PDF. */
    var url: JsAny?

    /**
     * Binary PDF data. Use TypedArrays (Uint8Array) to improve the memory usage. If PDF data is BASE64-encoded, use
     * `atob()` to convert it to a binary string first.
     *
     * NOTE: If TypedArrays are used they will generally be transferred to the worker-thread. This will help reduce
     * main-thread memory usage, however it will take ownership of the TypedArrays.
     */
    var data: BufferSource

    /** Basic authentication headers. */
    var httpHeaders: JsAny?

    /**
     * Indicates whether or not cross-site Access-Control requests should be made using credentials such as cookies or
     * authorization headers.
     *
     * The default is `false`.
     */
    var withCredentials: Boolean

    /** For decrypting password-protected PDFs. */
    var password: String

    /** The PDF file length. It's used for progress reports and range requests operations. */
    var length: JsNumber

    /** Allows for using a custom range transport implementation. */
    var range: PDFDataRangeTransport

    /** Specify maximum number of bytes fetched per range request. The default value is [DEFAULT_RANGE_CHUNK_SIZE]. */
    var rangeChunkSize: JsNumber

    /** The worker that will be used for loading and parsing the PDF data. */
    var worker: PDFWorker

    /** Controls the logging level; the constants from [VerbosityLevel] should be used. */
    var verbosity: JsNumber

    /**
     * The base URL of the document, used when attempting to recover valid absolute URLs for annotations, and outline
     * items, that (incorrectly) only specify relative URLs.
     */
    var docBaseUrl: String

    /** The URL where the predefined Adobe CMaps are located. Include the trailing slash. */
    var cMapUrl: String

    /**
     * Specifies if the Adobe CMaps are binary packed or not.
     *
     * The default value is `true`.
     */
    var cMapPacked: Boolean

    /**
     * The factory that will be used when reading built-in CMap files.
     *
     * The default value is [DOMCMapReaderFactory].
     */
    var CMapReaderFactory: JsAny?

    /** The URL where the predefined ICC profiles are located. Include the trailing slash. */
    var iccUrl: String

    /**
     * When `true`, fonts that aren't embedded in the PDF document will fallback to a system font.
     *
     * The default value is `true` in web environments and `false` in Node.js; unless `disableFontFace === true` in
     * which case this defaults to `false` regardless of the environment (to prevent completely broken fonts).
     */
    var useSystemFonts: Boolean

    /** The URL where the standard font files are located. Include the trailing slash. */
    var standardFontDataUrl: String

    /**
     * The factory that will be used when reading the standard font files.
     *
     * The default value is [DOMStandardFontDataFactory].
     */
    var StandardFontDataFactory: JsAny?

    /** The URL where the wasm files are located. Include the trailing slash. */
    var wasmUrl: String

    /**
     * The factory that will be used when reading the wasm files.
     *
     * The default value is [DOMWasmFactory].
     */
    var WasmFactory: JsAny?

    /**
     * Enable using the Fetch API in the worker-thread when reading CMap and standard font files. When `true`, the
     * `CMapReaderFactory`, `StandardFontDataFactory`, and `WasmFactory` options are ignored.
     *
     * The default value is `true` in web environments and `false` in Node.js.
     */
    var useWorkerFetch: Boolean

    /**
     * Attempt to use WebAssembly in order to improve e.g. image decoding performance.
     *
     * The default value is `true`.
     */
    var useWasm: Boolean

    /**
     * Reject certain promises, e.g. `getOperatorList`, `getTextContent`, and `RenderTask`, when the associated PDF data
     * cannot be successfully parsed, instead of attempting to recover whatever possible of the data.
     *
     * The default value is `false`.
     */
    var stopAtErrors: Boolean

    /**
     * The maximum allowed image size in total pixels, i.e. width * height. Images above this value will not be
     * rendered.
     *
     * Use -1 for no limit, which is also the default value.
     */
    var maxImageSize: JsNumber

    /**
     * Determines if we can evaluate strings as JavaScript. Primarily used to improve performance of PDF functions.
     *
     * The default value is `true`.
     */
    var isEvalSupported: Boolean?

    /**
     * Determines if we can use `OffscreenCanvas` in the worker. Primarily used to improve performance of image
     * conversion/rendering.
     *
     * The default value is `true` in web environments and `false` in Node.js.
     */
    var isOffscreenCanvasSupported: Boolean?

    /**
     * Determines if we can use `ImageDecoder` in the worker. Primarily used to improve performance of image
     * conversion/rendering.
     *
     * The default value is `true` in web environments and `false` in Node.js.
     *
     * NOTE: Also temporarily disabled in Chromium browsers, until we no longer support the affected browser versions,
     * because of various bugs:
     *
     * - Crashes when using the BMP decoder with huge images, e.g. issue6741.pdf; see
     *   https://issues.chromium.org/issues/374807001
     *
     * - Broken images when using the JPEG decoder with images that have custom colour profiles, e.g. GitHub discussion
     *   19030; see https://issues.chromium.org/issues/378869810
     */
    var isImageDecoderSupported: Boolean?

    /**
     * The integer value is used to know when an image must be resized (uses `OffscreenCanvas` in the worker). If it's
     * -1 then a possibly slow algorithm is used to guess the max value.
     */
    var canvasMaxAreaInBytes: JsNumber?

    /**
     * By default fonts are converted to OpenType fonts and loaded via the Font Loading API or `@font-face` rules. If
     * disabled, fonts will be rendered using a built-in font renderer that constructs the glyphs with primitive path
     * commands.
     *
     * The default value is `false` in web environments and `true` in Node.js.
     */
    var disableFontFace: Boolean?

    /**
     * Include additional properties, which are unused during rendering of PDF documents, when exporting the parsed font
     * data from the worker-thread. This may be useful for debugging purposes (and backwards compatibility), but note
     * that it will lead to increased memory usage.
     *
     * The default value is `false`.
     */
    var fontExtraProperties: Boolean?

    /**
     * Render Xfa forms if any.
     *
     * The default value is `false`.
     */
    var enableXfa: Boolean?

    /**
     * Specify an explicit document context to create elements with and to load resources, such as fonts, into.
     *
     * Defaults to the current document.
     */
    var ownerDocument: HTMLDocument?

    /**
     * Disable range request loading of PDF files. When enabled, and if the server supports partial content requests,
     * then the PDF will be fetched in chunks.
     *
     * The default value is `false`.
     */
    var disableRange: Boolean?

    /**
     * Disable streaming of PDF file data. By default PDF.js attempts to load PDF files in chunks.
     *
     * The default value is `false`.
     */
    var disableStream: Boolean?

    /**
     * Disable pre-fetching of PDF file data. When range requests are enabled PDF.js will automatically keep fetching
     * more data even if it isn't needed to display the current page.
     *
     * The default value is `false`.
     *
     * NOTE: It is also necessary to disable streaming, see above, in order for disabling of pre-fetching to work
     * correctly.
     */
    var disableAutoFetch: Boolean?

    /**
     * Enables special hooks for debugging PDF.js (see `web/debugger.js`).
     *
     * The default value is `false`.
     */
    var pdfBug: JsAny?

    /**
     * The factory that will be used when creating canvases.
     *
     * The default value is [DOMCanvasFactory].
     */
    var CanvasFactory: JsAny?

    /**
     * The factory that will be used to create SVG filters when rendering some images on the main canvas.
     *
     * The default value is [DOMFilterFactory].
     */
    var FilterFactory: JsAny?

    /**
     * Enables hardware acceleration for rendering.
     *
     * The default value is `false`.
     */
    var enableHWA: Boolean?
}

/**
 * This is the main entry point for loading a PDF and interacting with it.
 *
 * NOTE: If a URL is used to fetch the PDF data a standard Fetch API call (or XHR as fallback) is used, which means it
 * must follow same origin rules, e.g. no cross-domain requests without CORS.
 */
external fun getDocument(src: String): PDFDocumentLoadingTask

external fun getDocument(src: URL): PDFDocumentLoadingTask

external fun getDocument(src: BufferSource): PDFDocumentLoadingTask

external fun getDocument(src: DocumentInitParameters): PDFDocumentLoadingTask

external interface OnProgressParameters : JsAny {
    /** Currently loaded number of bytes. */
    var loaded: JsNumber

    /** Total number of bytes in the PDF file. */
    var total: JsNumber
}

/**
 * The loading task controls the operations required to load a PDF document (such as network requests) and provides a
 * way to listen for completion, after which individual pages can be rendered.
 */
open external class PDFDocumentLoadingTask {
    /** Unique identifier for the document loading task. */
    var docId: String

    /** Whether the loading task is destroyed or not. */
    var destroyed: Boolean

    /**
     * Callback to request a password if a wrong or no password was provided. The callback receives two parameters: a
     * function that should be called with the new password, and a reason (see [PasswordResponses]).
     */
    var onPassword: (updatePassword: (String) -> Unit, reason: JsNumber) -> Unit

    /**
     * Callback to be able to monitor the loading progress of the PDF file (necessary to implement e.g. a loading bar).
     * The callback receives an [OnProgressParameters] argument.
     */
    var onProgress: (OnProgressParameters) -> Unit

    /** Promise for document loading task completion. */
    val promise: Promise<PDFDocumentProxy>

    /** Abort all network requests and destroy the worker. */
    fun destroy(): Promise<Void>

    /**
     * Attempt to fetch the raw data of the PDF document, when e.g.
     * - An exception was thrown during document initialization.
     * - An `onPassword` callback is delaying initialization.
     */
    fun getData(): Promise<Uint8Array<out ArrayBufferLike>>
}

/**
 * Abstract class to support range requests file loading.
 *
 * NOTE: The TypedArrays passed to the constructor and relevant methods below will generally be transferred to the
 * worker-thread. This will help reduce main-thread memory usage, however it will take ownership of the TypedArrays.
 */
open external class PDFDataRangeTransport {
    constructor(
        length: JsNumber,
        initialData: BufferSource?,
        progressiveDone: Boolean = definedExternally,
        contentDispositionFilename: String? = definedExternally,
    )

    fun addRangeListener(listener: (begin: JsNumber, chunk: BufferSource?) -> Unit)

    fun addProgressListener(listener: (loaded: JsNumber, total: JsNumber?) -> Unit)

    fun addProgressiveReadListener(listener: (chunk: BufferSource?) -> Unit)

    fun addProgressiveDoneListener(listener: () -> Unit)

    fun onDataRange(begin: JsNumber, chunk: BufferSource?)

    fun onDataProgress(loaded: JsNumber, total: JsNumber?)

    fun onDataProgressiveRead(chunk: BufferSource?)

    fun onDataProgressiveDone()

    fun transportReady()

    fun requestDataRange(begin: JsNumber, end: JsNumber)

    fun abort()
}

open external class AnnotationStorage : JsIterable<JsAny?> {
    val onSetModified: () -> Unit
    val onResetModified: () -> Unit
    val onAnnotationEditor: (String?) -> Unit

    /**
     * Get the value for a given key if it exists, or return the default value.
     *
     * @param {string} key
     * @param {Object} defaultValue
     * @returns {Object}
     */
    fun getValue(key: String, defaultValue: JsAny?): JsAny?

    /**
     * Get the value for a given key.
     *
     * @param {string} key
     * @returns {Object}
     */
    fun getRawValue(key: String): JsAny?

    /**
     * Remove a value from the storage.
     *
     * @param {string} key
     */
    fun remove(key: String)

    /**
     * Set the value for a given key
     *
     * @param {string} key
     * @param {Object} value
     */
    fun setValue(key: String, value: JsAny?)

    /**
     * Check if the storage contains the given key.
     *
     * @param {string} key
     * @returns {boolean}
     */
    fun has(key: String): Boolean

    val size: JsNumber

    /** @returns {PrintAnnotationStorage} */
    val print: PrintAnnotationStorage

    fun resetModifiedIds()

    interface ModifiedIds {
        var ids: JsSet<JsString>
        var hash: String
    }

    /** @returns {{ids: Set<string>, hash: string}} */
    val modifiedIds: ModifiedIds
}

/**
 * A special `AnnotationStorage` for use during printing, where the serializable data is *frozen* upon initialization,
 * to prevent scripting from modifying its contents. (Necessary since printing is triggered synchronously in browsers.)
 */
external class PrintAnnotationStorage : AnnotationStorage

external interface GetOptionalContentConfigParameters : JsAny {
    /**
     * Determines the optional content groups that are visible by default; valid values are:
     * - 'display' (viewable groups).
     * - 'print' (printable groups).
     * - 'any' (all groups).
     *
     *   The default value is 'display'.
     */
    var intent: String
}

/** Proxy to a `PDFDocument` in the worker thread. */
open external class PDFDocumentProxy : JsAny {
    /** @type {AnnotationStorage} Storage for annotation data in forms. */
    val annotationStorage: AnnotationStorage

    /** @type {Object} The canvas factory instance. */
    val canvasFactory: JsAny?

    /** @type {Object} The filter factory instance. */
    val filterFactory: JsAny?

    /** Total number of pages in the PDF file. */
    val numPages: JsNumber

    /**
     * A (not guaranteed to be) unique ID to identify the PDF document. NOTE: The first element will always be defined
     * for all PDF documents, whereas the second element is only defined for *modified* PDF documents.
     */
    val fingerprints: JsArray<JsString?>

    /** True if only XFA form. */
    val isPureXfa: Boolean

    /**
     * NOTE: This is (mostly) intended to support printing of XFA forms.
     *
     * An object representing a HTML tree structure to render the XFA, or `null` when no XFA form exists.
     */
    val allXfaHtml: JsAny?

    fun getPage(pageNumber: JsNumber): Promise<PDFPageProxy>

    fun getPageIndex(ref: RefProxy): Promise<JsNumber>

    /**
     * Returns a mapping from named destinations to references.
     *
     * This can be slow for large documents. Use `getDestination` instead.
     */
    fun getDestinations(): Promise<JsAny?>

    /**
     * Returns all information of the given named destination, or `null` when the named destination is not present in
     * the PDF file.
     */
    fun getDestination(id: String): Promise<JsArray<JsAny?>?>

    /**
     * Returns an {Array} containing the page labels that correspond to the page indexes, or `null` when no page labels
     * are present in the PDF file.
     */
    fun getPageLabels(): Promise<JsArray<JsString>?>

    /** Returns the page layout name. */
    fun getPageLayout(): Promise<JsString>

    /** Returns the page mode name */
    fun getPageMode(): Promise<JsString>

    /**
     * Returns an {Object} containing the viewer preferences, or `null` when no viewer preferences are present in the
     * PDF file.
     */
    fun getViewerPreferences(): Promise<JsAny?>

    /** Returns an {Array} containing the destination, or `null` when no open action is present in the PDF. */
    fun getOpenAction(): Promise<JsArray<JsAny?>?>

    /** Returns a lookup table for mapping named attachments to their content. */
    fun getAttachments(): Promise<JsAny?>

    /**
     * Returns an {Object} with the JavaScript actions:
     * - from the name tree.
     * - from A or AA entries in the catalog dictionary. or `null` if no JavaScript exists.
     */
    fun getJSActions(): Promise<JsAny?>

    interface OutlineNode : JsAny {
        var title: String
        var bold: Boolean
        var italic: Boolean

        /** The color in RGB format to use for display purposes. */
        var color: Uint8ClampedArray<out ArrayBufferLike>
        var dest: String?
        var url: String?
        var unsafeUrl: String?
        var newWindow: Boolean?
        var count: JsNumber?
        var items: JsArray<OutlineNode>
    }

    /** Returns a tree outline (if it has one) of the PDF file. */
    fun getOutline(): Promise<JsArray<OutlineNode>>

    /** Returns all the optional content groups (assuming that the document has any). */
    fun getOptionalContentConfig(
        params: GetOptionalContentConfigParameters = definedExternally
    ): Promise<OptionalContentConfig>

    /** Returns the permission flags for the PDF document, or `null` when no permissions are present in the PDF file. */
    fun getPermissions(): Promise<JsArray<JsNumber>?>

    interface MetadataInfo : JsAny {
        /** information dictionary */
        val info: JsAny?

        /** information from the metadata section of the PDF. */
        val metadata: Metadata
    }

    fun getMetadata(): Promise<MetadataInfo>

    /** Properties correspond to Table 321 of the PDF 32000-1:2008 spec. */
    interface MarkInfo : JsAny {
        var Marked: Boolean
        var UserProperties: Boolean
        var Suspects: Boolean
    }

    /**
     * Returns the MarkInfo flags for the PDF document, or `null` when no MarkInfo values are present in the PDF file.
     */
    fun getMarkInfo(): Promise<MarkInfo?>

    /** Returns the raw data of the PDF document. */
    fun getData(): Promise<Uint8ClampedArray<out ArrayBufferLike>>

    /** Returns the full data of the saved document. */
    fun saveDocument(): Promise<Uint8ClampedArray<out ArrayBufferLike>>

    interface DownloadInfo : JsAny {
        /** Indicates size of the PDF data in bytes. */
        val length: JsNumber
    }

    /** Returns when the document's data is loaded. */
    fun getDownloadInfo(): Promise<DownloadInfo>

    /**
     * Cleans up resources allocated by the document on both the main and worker threads.
     *
     * NOTE: Do not, under any circumstances, call this method when rendering is currently ongoing since that may lead
     * to rendering errors.
     */
    fun cleanup(keepLoadedFonts: Boolean = definedExternally): Promise<Void>

    /** Destroys the current document instance and terminates the worker. */
    fun destroy()

    fun cachedPageNumber(ref: RefProxy): JsNumber?

    /** A subset of the current [DocumentInitParameters], which are needed in the viewer. */
    val loadingParams: DocumentInitParameters

    /** @type {PDFDocumentLoadingTask} The loadingTask for the current document. */
    val loadingTask: PDFDocumentLoadingTask

    /**
     * Returns an {Object} containing /AcroForm field data for the JS sandbox, or `null` when no field data is present
     * in the PDF file.
     */
    fun getFieldObjects(): Promise<JsAny?>

    /** Returns `true` if some /AcroForm fields have JavaScript actions. */
    fun hasJSActions(): Promise<JsBoolean>

    /**
     * Returns IDs of annotations that have a calculation action, or `null` when no such annotations are present in the
     * PDF file.
     */
    fun getCalculationOrderIds(): Promise<JsArray<JsString>?>
}

/** Page getViewport parameters. */
external interface GetViewportParameters : JsAny {
    /** The desired scale of the viewport. */
    var scale: JsNumber

    /**
     * The desired rotation, in degrees, of the viewport.
     *
     * If omitted it defaults to the page rotation.
     */
    var rotation: JsNumber

    /**
     * The horizontal, i.e. x-axis, offset.
     *
     * The default value is `0`.
     */
    var offsetX: JsNumber

    /**
     * The vertical, i.e. y-axis, offset.
     *
     * The default value is `0`.
     */
    var offsetY: JsNumber

    /**
     * If true, the y-axis will not be flipped.
     *
     * The default value is `false`.
     */
    var dontFlip: Boolean
}

/** Page getTextContent parameters. */
external interface GetTextContentParameters : JsAny {
    /**
     * When true include marked content items in the items array of TextContent.
     *
     * The default is `false`.
     */
    var includeMarkedContent: Boolean

    /**
     * When true the text is *not* normalized in the worker-thread.
     *
     * The default is `false`.
     */
    var disableNormalization: Boolean
}

/** Page text content. */
external interface TextContent : JsAny {
    /**
     * Array of [TextItem] and [TextMarkedContent] objects. TextMarkedContent items are included when
     * includeMarkedContent is true.
     */
    var items: JsArray<JsAny?>

    /** [TextStyle] objects, indexed by font name. */
    var styles: JsAny?

    /** The document /Lang attribute. */
    var lang: String?
}

/** Page text content part. */
external interface TextItem : JsAny {
    /** Text content. */
    var str: String

    /** Text direction: 'ttb', 'ltr' or 'rtl'. */
    var dir: String

    /** Transformation matrix. */
    var transform: JsArray<JsNumber>

    /** Width in device space. */
    var width: JsNumber

    /** Height in device space. */
    var height: JsNumber

    /** Font name used by PDF.js for converted font. */
    var fontName: String

    /** Indicating if the text content is followed by a line-break. */
    var hasEOL: Boolean
}

/** Page text marked content part. */
external interface TextMarkedContent : JsAny {
    /** Either 'beginMarkedContent', 'beginMarkedContentProps', or 'endMarkedContent'. */
    var type: String

    /** The marked content identifier. Only used for type 'beginMarkedContentProps'. */
    var id: String
}

/** Text style. */
external interface TextStyle : JsAny {
    /** Font ascent. */
    var ascent: JsNumber

    /** Font descent. */
    var descent: JsNumber

    /** Whether or not the text is in vertical mode. */
    var vertical: Boolean

    /** The possible font family. */
    var fontFamily: String
}

/** Page annotation parameters. */
external interface GetAnnotationsParameters : JsAny {
    /**
     * Determines the annotations that are fetched, can be 'display' (viewable annotations), 'print' (printable
     * annotations), or 'any' (all annotations). The default value is 'display'.
     */
    var intent: String
}

/** Page render parameters. */
external interface RenderParameters : JsAny {
    /** A 2D context of a DOM Canvas object. */
    var canvasContext: CanvasRenderingContext2D

    /** Rendering viewport obtained by calling the `PDFPageProxy.getViewport` method. */
    var viewport: PageViewport

    /**
     * Rendering intent, can be 'display', 'print', or 'any'.
     *
     * The default value is 'display'.
     */
    var intent: String

    /**
     * Controls which annotations are rendered onto the canvas, for annotations with appearance-data; the values from
     * [AnnotationMode] should be used. The following values are supported:
     * - `AnnotationMode.DISABLE`, which disables all annotations.
     * - `AnnotationMode.ENABLE`, which includes all possible annotations (thus it also depends on the `intent`-option,
     *   see above).
     * - `AnnotationMode.ENABLE_FORMS`, which excludes annotations that contain interactive form elements (those will be
     *   rendered in the display layer).
     * - `AnnotationMode.ENABLE_STORAGE`, which includes all possible annotations (as above) but where interactive form
     *   elements are updated with data from the [AnnotationStorage]-instance; useful e.g. for printing. The default
     *   value is `AnnotationMode.ENABLE`.
     */
    var annotationMode: JsNumber

    /** Additional transform, applied just before viewport transform. */
    var transform: JsArray<JsNumber>

    /**
     * @property {CanvasGradient | CanvasPattern | string} [background] - Background to use for the canvas. Any valid
     *   `canvas.fillStyle` can be used: a `DOMString` parsed as CSS <color> value, a `CanvasGradient` object (a linear
     *   or radial gradient) or a `CanvasPattern` object (a repetitive image). The default value is 'rgb(255,255,255)'.
     *
     *   NOTE: This option may be partially, or completely, ignored when the `pageColors`-option is used.
     */
    var background: JsAny?

    /**
     * Overwrites background and foreground colors with user defined ones in order to improve readability in high
     * contrast mode.
     */
    var pageColors: JsAny?

    /**
     * A promise that should resolve with an [OptionalContentConfig] created from
     * `PDFDocumentProxy.getOptionalContentConfig`. If `null`, the configuration will be fetched automatically with the
     * default visibility states set.
     */
    var optionalContentConfigPromise: Promise<OptionalContentConfig>

    /** Map some annotation ids with canvases used to render them. */
    var annotationCanvasMap: JsMap<JsString, HTMLCanvasElement>
    var printAnnotationStorage: AnnotationStorage

    /** Render the page in editing mode. */
    var isEditing: Boolean
}

/** Page GetOperatorList parameters. */
external interface GetOperatorListParameters : JsAny {
    /**
     * @property {string} [intent] - Rendering intent, can be 'display', 'print', or 'any'. The default value is
     *   'display'.
     */
    var intent: String

    /**
     * @property {number} [annotationMode] Controls which annotations are included in the operatorList, for annotations
     *   with appearance-data; the values from [AnnotationMode] should be used. The following values are supported:
     *     - `AnnotationMode.DISABLE`, which disables all annotations.
     *     - `AnnotationMode.ENABLE`, which includes all possible annotations (thus it also depends on the
     *       `intent`-option, see above).
     *     - `AnnotationMode.ENABLE_FORMS`, which excludes annotations that contain interactive form elements (those
     *       will be rendered in the display layer).
     *     - `AnnotationMode.ENABLE_STORAGE`, which includes all possible annotations (as above) but where interactive
     *       form elements are updated with data from the [AnnotationStorage]-instance; useful e.g. for printing. The
     *       default value is `AnnotationMode.ENABLE`.
     */
    var annotationMode: JsNumber

    /** @property {PrintAnnotationStorage} [printAnnotationStorage] */
    var printAnnotationStorage: PrintAnnotationStorage

    /** @property {boolean} [isEditing] - Render the page in editing mode. */
    var isEditing: Boolean
}

external interface StructTreeElement : JsAny

/** Structure tree node. The root node will have a role "Root". */
external interface StructTreeNode : StructTreeElement {
    /** Array of [StructTreeNode] and [StructTreeContent] objects. */
    var children: JsArray<StructTreeElement>

    /** @property {string} role - element's role, already mapped if a role map exists in the PDF. */
    var role: String
}

/** Structure tree content. */
external interface StructTreeContent : StructTreeElement {
    /**
     * @property {string} type - either "content" for page and stream structure elements or "object" for object
     *   references.
     */
    var type: String

    /** @property {string} id - unique id that will map to the text layer. */
    var id: String
}

/** PDF page operator list. */
external interface PDFOperatorList : JsAny {
    /** Array containing the operator functions. */
    var fnArray: JsArray<JsNumber>

    /** Array containing the arguments of the functions. */
    var argsArray: JsArray<JsAny?>
}

/** Proxy to a `PDFPage` in the worker thread. */
open external class PDFPageProxy : JsAny {
    /** Page number of the page. First page is 1. */
    val pageNumber: JsNumber

    /** The number of degrees the page is rotated clockwise. */
    val rotate: JsNumber

    /** The reference that points to this page. */
    val ref: RefProxy?

    /** The default size of units in 1/72nds of an inch. */
    val userUnit: JsNumber

    /** An array of the visible portion of the PDF page in user space units [x1, y1, x2, y2]. */
    val view: JsArray<JsNumber>

    /**
     * @param {GetViewportParameters} params - Viewport parameters.
     * @returns {PageViewport} Contains 'width' and 'height' properties along with transforms required for rendering.
     */
    fun getViewport(params: GetViewportParameters = definedExternally): PageViewport

    /**
     * @param {GetAnnotationsParameters} [params] - Annotation parameters.
     * @returns {Promise<Array<any>>} A promise that is resolved with an {Array} of the annotation objects.
     */
    fun getAnnotations(params: GetAnnotationsParameters = definedExternally): Promise<JsArray<JsAny?>>

    /** @returns {Promise<Object>} A promise that is resolved with an {Object} with JS actions. */
    fun getJSActions(): Promise<JsAny?>

    /** @type {Object} The filter factory instance. */
    val filterFactory: JsAny?

    /** @type {boolean} True if only XFA form. */
    val isPureXfa: Boolean

    /**
     * @returns an {Object} with a fake DOM object (a tree structure where elements are {Object} with a name, attributes
     *   (class, style, ...), value and children, very similar to a HTML DOM tree), or `null` if no XFA exists.
     */
    fun getXfa(): Promise<JsAny?>

    /**
     * Begins the process of rendering a page to the desired context.
     *
     * @param [RenderParameters] params - Page render parameters.
     * @returns [RenderTask] An object that contains a promise that is resolved when the page finishes rendering.
     */
    fun render(params: RenderParameters): RenderTask

    /**
     * @param [GetOperatorListParameters] params - Page getOperatorList parameters.
     * @returns an [PDFOperatorList] object that represents the page's operator list.
     */
    fun getOperatorList(params: GetOperatorListParameters): Promise<PDFOperatorList>

    /**
     * NOTE: All occurrences of whitespace will be replaced by standard spaces (0x20).
     *
     * @param [GetTextContentParameters] params - getTextContent parameters.
     * @returns [ReadableStream] Stream for reading text content chunks.
     */
    fun streamTextContent(params: GetTextContentParameters): ReadableStream<JsString>

    /**
     * NOTE: All occurrences of whitespace will be replaced by standard spaces (0x20).
     *
     * @param [GetTextContentParameters] params - getTextContent parameters.
     * @returns a [TextContent] object that represents the page's text content.
     */
    fun getTextContent(params: GetTextContentParameters): Promise<TextContent>

    /**
     * @returns a [StructTreeNode] object that represents the page's structure tree, or `null` when no structure tree is
     *   present for the current page.
     */
    fun getStructTree(): Promise<StructTreeNode>

    /**
     * Cleans up resources allocated by the page.
     *
     * @param {boolean} [resetStats] - Reset page stats, if enabled. The default value is `false`.
     * @returns {boolean} Indicates if clean-up was successfully run.
     */
    fun cleanup(resetStats: Boolean = definedExternally): Boolean
}

external interface PDFWorkerParameters : JsAny {
    /** The name of the worker. */
    var name: String

    /** The `workerPort` object. */
    var port: Worker

    /** Controls the logging level; the constants from [VerbosityLevel] should be used. */
    var verbosity: JsNumber
}

/**
 * PDF.js web worker abstraction that controls the instantiation of PDF documents. Message handlers are used to pass
 * information from the main thread to the worker thread and vice versa. If the creation of a web worker is not
 * possible, a "fake" worker will be used instead.
 */
open external class PDFWorker {
    constructor(params: PDFWorkerParameters = definedExternally)

    /** Promise for worker initialization completion. */
    val promise: Promise<Void>

    /** The current `workerPort`, when it exists. */
    val port: Worker

    /** The current MessageHandler-instance. */
    val messageHandler: MessageHandler

    /** Destroys the worker instance. */
    fun destroy()

    companion object {
        @JsStatic fun create(params: PDFWorkerParameters): PDFWorker

        /** The current `workerSrc`, when it exists. */
        @JsStatic val workerSrc: String
    }
}

/** Allows controlling of the rendering tasks. */
open external class RenderTask {
    /**
     * Callback for incremental rendering -- a function that will be called each time the rendering is paused. To
     * continue rendering call the function that is the first argument to the callback.
     */
    var onContinue: ((resume: () -> Unit) -> Unit)? = definedExternally

    /**
     * A function that will be synchronously called when the rendering tasks finishes with an error (either because of
     * an actual error, or because the rendering is cancelled).
     */
    var onError: ((error: JsError) -> Unit)? = definedExternally

    /** Promise for rendering task completion. */
    val promise: Promise<Void>

    /**
     * Cancels the rendering task. If the task is currently rendering it will not be cancelled until graphics pauses
     * with a timeout. The promise that this object extends will be rejected when cancelled.
     */
    fun cancel(extraDelay: JsNumber = definedExternally)

    /** Whether form fields are rendered separately from the main operatorList. */
    val separateAnnots: Boolean
}

external interface PageViewportParameters : JsAny {
    /** The xMin, yMin, xMax and yMax coordinates. */
    var viewBox: JsArray<JsNumber>

    /** The size of units. */
    var userUnit: JsNumber

    /**
     * The scale, overriding the one in the cloned viewport.
     *
     * The default value is `this.scale`.
     */
    var scale: JsNumber

    /**
     * The rotation, in degrees, overriding the one in the cloned viewport.
     *
     * The default value is `this.rotation`.
     */
    var rotation: JsNumber

    /**
     * The horizontal, i.e. x-axis, offset.
     *
     * The default value is `this.offsetX`.
     */
    var offsetX: JsNumber

    /**
     * The vertical, i.e. y-axis, offset.
     *
     * The default value is `this.offsetY`.
     */
    var offsetY: JsNumber

    /**
     * If true, the x-axis will not be flipped.
     *
     * The default value is `false`.
     */
    var dontFlip: Boolean
}

external interface PageViewportCloneParameters : JsAny {
    /**
     * The scale, overriding the one in the cloned viewport.
     *
     * The default value is `this.scale`.
     */
    var scale: JsNumber

    /**
     * The rotation, in degrees, overriding the one in the cloned viewport.
     *
     * The default value is `this.rotation`.
     */
    var rotation: JsNumber

    /**
     * The horizontal, i.e. x-axis, offset.
     *
     * The default value is `this.offsetX`.
     */
    var offsetX: JsNumber

    /**
     * The vertical, i.e. y-axis, offset.
     *
     * The default value is `this.offsetY`.
     */
    var offsetY: JsNumber

    /**
     * If true, the x-axis will not be flipped.
     *
     * The default value is `false`.
     */
    var dontFlip: Boolean
}

/** PDF page viewport created based on scale, rotation and offset. */
open external class PageViewport {
    /** The xMin, yMin, xMax and yMax coordinates. */
    val viewBox: JsArray<JsNumber>

    /** The size of units. */
    val userUnit: JsNumber

    /**
     * The scale, overriding the one in the cloned viewport.
     *
     * The default value is `this.scale`.
     */
    var scale: JsNumber

    /**
     * The rotation, in degrees, overriding the one in the cloned viewport.
     *
     * The default value is `this.rotation`.
     */
    var rotation: JsNumber

    /**
     * The horizontal, i.e. x-axis, offset.
     *
     * The default value is `this.offsetX`.
     */
    var offsetX: JsNumber

    /**
     * The vertical, i.e. y-axis, offset.
     *
     * The default value is `this.offsetY`.
     */
    var offsetY: JsNumber
    var width: JsNumber
    var height: JsNumber

    constructor(params: PageViewportParameters)

    interface Dimensions {
        var pageWidth: JsNumber
        var pageHeight: JsNumber
        var pageX: JsNumber
        var pageY: JsNumber
    }

    /** The original, un-scaled, viewport dimensions. */
    var rawDims: Dimensions

    /** Clones viewport, with optional additional properties. */
    fun clone(params: PageViewportCloneParameters): PageViewport

    /**
     * Converts PDF point to the viewport coordinates. For examples, useful for converting PDF location into canvas
     * pixel coordinates.
     *
     * @param [JsNumber] x - The x-coordinate.
     * @param [JsNumber] y - The y-coordinate.
     * @returns [Array] Array containing `x`- and `y`-coordinates of the point in the viewport coordinate space.
     * @see [convertToPdfPoint]
     * @see [convertToViewportRectangle]
     */
    fun convertToViewportPoint(x: JsNumber, y: JsNumber): JsArray<JsNumber>

    /**
     * Converts PDF rectangle to the viewport coordinates.
     *
     * @param [Array] rect - The xMin, yMin, xMax and yMax coordinates.
     * @returns [Array] Array containing corresponding coordinates of the rectangle in the viewport coordinate space.
     * @see [convertToViewportPoint]
     */
    fun convertToViewportRectangle(rect: JsArray<JsNumber>): JsArray<JsNumber>

    /**
     * Converts viewport coordinates to the PDF location. For examples, useful for converting canvas pixel location into
     * PDF one.
     *
     * @param [JsNumber] x - The x-coordinate.
     * @param [JsNumber] y - The y-coordinate.
     * @returns [Array] Array containing `x`- and `y`-coordinates of the point in the PDF coordinate space.
     * @see [convertToViewportPoint]
     */
    fun convertToPdfPoint(x: JsNumber, y: JsNumber): JsArray<JsNumber>
}

/**
 * @param {HTMLDivElement} div
 * @param {PageViewport} viewport
 * @param {boolean} mustFlip
 * @param {boolean} mustRotate
 */
external fun setLayerDimensions(
    div: HTMLDivElement,
    viewport: PageViewport,
    mustFlip: Boolean = definedExternally,
    mustRotate: Boolean = definedExternally,
)

external object AnnotationMode {
    val DISABLE: JsNumber
    val ENABLE: JsNumber
    val ENABLE_FORMS: JsNumber
    val ENABLE_STORAGE: JsNumber
}

external object VerbosityLevel {
    val ERRORS: JsNumber
    val WARNINGS: JsNumber
    val INFOS: JsNumber
}

external object PasswordResponses {
    val NEED_PASSWORD: JsNumber
    val INCORRECT_PASSWORD: JsNumber
}

external object GlobalWorkerOptions {
    /** Defines global port for worker process. Overrides the `workerSrc` option. */
    var workerPort: Worker?

    /**
     * A string containing the path and filename of the worker file.
     *
     * NOTE: The `workerSrc` option should always be set, in order to prevent any issues when using the PDF.js library.
     */
    var workerSrc: String
}

external class OptionalContentGroup : JsAny {
    val name: String
    val intent: String
    val usage: String
    val rbGroups: JsArray<JsAny?>
    val visible: Boolean
}

external class OptionalContentConfig : JsIterable<OptionalContentGroup> {
    val renderingIntent: String
    val name: String?
    val creator: String?

    fun isVisible(group: JsAny?): Boolean

    fun setVisibility(id: String, visible: Boolean = definedExternally, preserveRB: Boolean = definedExternally)

    fun setOCGState(params: JsAny?)

    val hasInitialVisibility: Boolean

    fun getOrder(): JsArray<JsString>?

    fun getGroup(id: String): OptionalContentGroup?

    fun getHash(): String
}

external class MessageHandler {
    fun on(actionName: String, handler: (data: JsAny?) -> JsAny?)

    /**
     * Sends a message to the comObj to invoke the action with the supplied data.
     *
     * @param {string} actionName - Action to call.
     * @param {JSON} data - JSON data to send.
     * @param {Array} [transfers] - List of transfers/ArrayBuffers.
     */
    fun send(actionName: String, data: JsAny?, transfers: JsArray<ArrayBufferLike>)

    /**
     * Sends a message to the comObj to invoke the action with the supplied data. Expects that the other side will
     * callback with the response.
     *
     * @param {string} actionName - Action to call.
     * @param {JSON} data - JSON data to send.
     * @param {Array} [transfers] - List of transfers/ArrayBuffers.
     * @returns {Promise} Promise to be resolved with response data.
     */
    fun sendWithPromise(actionName: String, data: JsAny?, transfers: JsArray<ArrayBufferLike>): Promise<JsAny?>

    /**
     * Sends a message to the comObj to invoke the action with the supplied data. Expect that the other side will
     * callback to signal 'start_complete'.
     *
     * @param {string} actionName - Action to call.
     * @param {JSON} data - JSON data to send.
     * @param {Object} queueingStrategy - Strategy to signal backpressure based on internal queue.
     * @param {Array} [transfers] - List of transfers/ArrayBuffers.
     * @returns {ReadableStream} ReadableStream to read data in chunks.
     */
    fun sendWithStream(
        actionName: String,
        data: JsAny?,
        queueingStrategy: JsAny?,
        transfers: JsArray<ArrayBufferLike>,
    ): ReadableStream<JsAny?>

    fun destroy()
}

external class Metadata : JsIterable<Tuple2<JsString, JsAny?>> {
    fun getRaw(): JsAny?

    fun get(name: String): JsAny?
}

open external class BaseException : JsError {
    val name: String
}
