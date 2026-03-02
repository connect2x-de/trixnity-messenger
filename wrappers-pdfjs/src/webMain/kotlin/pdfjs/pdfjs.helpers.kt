@file:OptIn(ExperimentalJsExport::class, ExperimentalWasmJsInterop::class)
@file:Suppress(
    "PropertyName", "LocalVariableName", "FunctionName",
    "unused", "NOTHING_TO_INLINE", "DuplicatedCode",
)

package pdfjs

import js.buffer.BufferSource
import js.collections.JsMap
import js.objects.unsafeJso
import js.promise.Promise
import web.canvas.CanvasRenderingContext2D
import web.html.HTMLCanvasElement
import web.html.HTMLDocument
import web.workers.Worker
import kotlin.js.ExperimentalJsExport
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsExport
import kotlin.js.JsNumber
import kotlin.js.JsString

@JsExport.Ignore
inline fun RefProxy(
    num: JsNumber? = null,
    gen: JsNumber? = null,
) = unsafeJso<RefProxy> {
    num?.let { this.num = it }
    gen?.let { this.gen = it }
}

@JsExport.Ignore
inline fun DocumentInitParameters(
    url: JsAny? = null,
    data: BufferSource? = null,
    httpHeaders: JsAny? = null,
    withCredentials: Boolean? = null,
    password: String? = null,
    length: JsNumber? = null,
    range: PDFDataRangeTransport? = null,
    rangeChunkSize: JsNumber? = null,
    worker: PDFWorker? = null,
    verbosity: JsNumber? = null,
    docBaseUrl: String? = null,
    cMapUrl: String? = null,
    cMapPacked: Boolean? = null,
    CMapReaderFactory: JsAny? = null,
    iccUrl: String? = null,
    useSystemFonts: Boolean? = null,
    standardFontDataUrl: String? = null,
    StandardFontDataFactory: JsAny? = null,
    wasmUrl: String? = null,
    WasmFactory: JsAny? = null,
    useWorkerFetch: Boolean? = null,
    useWasm: Boolean? = null,
    stopAtErrors: Boolean? = null,
    maxImageSize: JsNumber? = null,
    isEvalSupported: Boolean? = null,
    isOffscreenCanvasSupported: Boolean? = null,
    isImageDecoderSupported: Boolean? = null,
    canvasMaxAreaInBytes: JsNumber? = null,
    disableFontFace: Boolean? = null,
    fontExtraProperties: Boolean? = null,
    enableXfa: Boolean? = null,
    ownerDocument: HTMLDocument? = null,
    disableRange: Boolean? = null,
    disableStream: Boolean? = null,
    disableAutoFetch: Boolean? = null,
    pdfBug: JsAny? = null,
    CanvasFactory: JsAny? = null,
    FilterFactory: JsAny? = null,
    enableHWA: Boolean? = null,
) = unsafeJso<DocumentInitParameters> {
    url?.let { this.url = it }
    data?.let { this.data = it }
    httpHeaders?.let { this.httpHeaders = it }
    withCredentials?.let { this.withCredentials = it }
    password?.let { this.password = it }
    length?.let { this.length = it }
    range?.let { this.range = it }
    rangeChunkSize?.let { this.rangeChunkSize = it }
    worker?.let { this.worker = it }
    verbosity?.let { this.verbosity = it }
    docBaseUrl?.let { this.docBaseUrl = it }
    cMapUrl?.let { this.cMapUrl = it }
    cMapPacked?.let { this.cMapPacked = it }
    CMapReaderFactory?.let { this.CMapReaderFactory = it }
    iccUrl?.let { this.iccUrl = it }
    useSystemFonts?.let { this.useSystemFonts = it }
    standardFontDataUrl?.let { this.standardFontDataUrl = it }
    StandardFontDataFactory?.let { this.StandardFontDataFactory = it }
    wasmUrl?.let { this.wasmUrl = it }
    WasmFactory?.let { this.WasmFactory = it }
    useWorkerFetch?.let { this.useWorkerFetch = it }
    useWasm?.let { this.useWasm = it }
    stopAtErrors?.let { this.stopAtErrors = it }
    maxImageSize?.let { this.maxImageSize = it }
    isEvalSupported?.let { this.isEvalSupported = it }
    isOffscreenCanvasSupported?.let { this.isOffscreenCanvasSupported = it }
    isImageDecoderSupported?.let { this.isImageDecoderSupported = it }
    canvasMaxAreaInBytes?.let { this.canvasMaxAreaInBytes = it }
    disableFontFace?.let { this.disableFontFace = it }
    fontExtraProperties?.let { this.fontExtraProperties = it }
    enableXfa?.let { this.enableXfa = it }
    ownerDocument?.let { this.ownerDocument = it }
    disableRange?.let { this.disableRange = it }
    disableStream?.let { this.disableStream = it }
    disableAutoFetch?.let { this.disableAutoFetch = it }
    pdfBug?.let { this.pdfBug = it }
    CanvasFactory?.let { this.CanvasFactory = it }
    FilterFactory?.let { this.FilterFactory = it }
    enableHWA?.let { this.enableHWA = it }
}

@JsExport.Ignore
inline fun OnProgressParameters(
    loaded: JsNumber? = null,
    total: JsNumber? = null,
) = unsafeJso<OnProgressParameters> {
    loaded?.let { this.loaded = it }
    total?.let { this.total = it }
}

@JsExport.Ignore
inline fun GetViewportParameters(
    scale: JsNumber? = null,
    rotation: JsNumber? = null,
    offsetX: JsNumber? = null,
    offsetY: JsNumber? = null,
    dontFlip: Boolean? = null,
) = unsafeJso<GetViewportParameters> {
    scale?.let { this.scale = it }
    rotation?.let { this.rotation = it }
    offsetX?.let { this.offsetX = it }
    offsetY?.let { this.offsetY = it }
    dontFlip?.let { this.dontFlip = it }
}

@JsExport.Ignore
inline fun GetTextContentParameters(
    includeMarkedContent: Boolean? = null,
    disableNormalization: Boolean? = null,
) = unsafeJso<GetTextContentParameters> {
    includeMarkedContent?.let { this.includeMarkedContent = it }
    disableNormalization?.let { this.disableNormalization = it }
}

@JsExport.Ignore
inline fun TextContent(
    items: JsArray<JsAny?>? = null,
    styles: JsAny? = null,
    lang: String? = null,
) = unsafeJso<TextContent> {
    items?.let { this.items = it }
    styles?.let { this.styles = it }
    lang?.let { this.lang = it }
}

@JsExport.Ignore
inline fun TextItem(
    str: String? = null,
    dir: String? = null,
    transform: JsArray<JsNumber>? = null,
    width: JsNumber? = null,
    height: JsNumber? = null,
    fontName: String? = null,
    hasEOL: Boolean? = null,
) = unsafeJso<TextItem> {
    str?.let { this.str = it }
    dir?.let { this.dir = it }
    transform?.let { this.transform = it }
    width?.let { this.width = it }
    height?.let { this.height = it }
    fontName?.let { this.fontName = it }
    hasEOL?.let { this.hasEOL = it }
}

@JsExport.Ignore
inline fun TextMarkedContent(
    type: String? = null,
    id: String? = null,
) = unsafeJso<TextMarkedContent> {
    type?.let { this.type = it }
    id?.let { this.id = it }
}

@JsExport.Ignore
inline fun TextStyle(
    ascent: JsNumber? = null,
    descent: JsNumber? = null,
    vertical: Boolean? = null,
    fontFamily: String? = null,
) = unsafeJso<TextStyle> {
    ascent?.let { this.ascent = it }
    descent?.let { this.descent = it }
    vertical?.let { this.vertical = it }
    fontFamily?.let { this.fontFamily = it }
}

@JsExport.Ignore
inline fun GetAnnotationsParameters(
    intent: String? = null,
) = unsafeJso<GetAnnotationsParameters> {
    intent?.let { this.intent = it }
}

@JsExport.Ignore
inline fun RenderParameters(
    canvasContext: CanvasRenderingContext2D? = null,
    viewport: PageViewport? = null,
    intent: String? = null,
    annotationMode: JsNumber? = null,
    transform: JsArray<JsNumber>? = null,
    background: JsAny? = null,
    pageColors: JsAny? = null,
    optionalContentConfigPromise: Promise<OptionalContentConfig>? = null,
    annotationCanvasMap: JsMap<JsString, HTMLCanvasElement>? = null,
    printAnnotationStorage: AnnotationStorage? = null,
    isEditing: Boolean? = null,
) = unsafeJso<RenderParameters> {
    canvasContext?.let { this.canvasContext = it }
    viewport?.let { this.viewport = it }
    intent?.let { this.intent = it }
    annotationMode?.let { this.annotationMode = it }
    transform?.let { this.transform = it }
    background?.let { this.background = it }
    pageColors?.let { this.pageColors = it }
    optionalContentConfigPromise?.let { this.optionalContentConfigPromise = it }
    annotationCanvasMap?.let { this.annotationCanvasMap = it }
    printAnnotationStorage?.let { this.printAnnotationStorage = it }
    isEditing?.let { this.isEditing = it }
}

@JsExport.Ignore
inline fun GetOperatorListParameters(
    intent: String? = null,
    annotationMode: JsNumber? = null,
    printAnnotationStorage: PrintAnnotationStorage? = null,
    isEditing: Boolean? = null,
) = unsafeJso<GetOperatorListParameters> {
    intent?.let { this.intent = it }
    annotationMode?.let { this.annotationMode = it }
    printAnnotationStorage?.let { this.printAnnotationStorage = it }
    isEditing?.let { this.isEditing = it }
}

@JsExport.Ignore
inline fun StructTreeNode(
    children: JsArray<StructTreeElement>? = null,
    role: String? = null,
) = unsafeJso<StructTreeNode> {
    children?.let { this.children = it }
    role?.let { this.role = it }
}

@JsExport.Ignore
inline fun StructTreeContent(
    type: String? = null,
    id: String? = null,
) = unsafeJso<StructTreeContent> {
    type?.let { this.type = it }
    id?.let { this.id = it }
}

@JsExport.Ignore
inline fun PDFOperatorList(
    fnArray: JsArray<JsNumber>? = null,
    argsArray: JsArray<JsAny?>? = null,
) = unsafeJso<PDFOperatorList> {
    fnArray?.let { this.fnArray = it }
    argsArray?.let { this.argsArray = it }
}

@JsExport.Ignore
inline fun PDFWorkerParameters(
    name: String? = null,
    port: Worker? = null,
    verbosity: JsNumber? = null,
) = unsafeJso<PDFWorkerParameters> {
    name?.let { this.name = it }
    port?.let { this.port = it }
    verbosity?.let { this.verbosity = it }
}

@JsExport.Ignore
inline fun PageViewportParameters(
    viewBox: JsArray<JsNumber>? = null,
    userUnit: JsNumber? = null,
    scale: JsNumber? = null,
    rotation: JsNumber? = null,
    offsetX: JsNumber? = null,
    offsetY: JsNumber? = null,
    dontFlip: Boolean? = null,
) = unsafeJso<PageViewportParameters> {
    viewBox?.let { this.viewBox = it }
    userUnit?.let { this.userUnit = it }
    scale?.let { this.scale = it }
    rotation?.let { this.rotation = it }
    offsetX?.let { this.offsetX = it }
    offsetY?.let { this.offsetY = it }
    dontFlip?.let { this.dontFlip = it }
}

@JsExport.Ignore
inline fun PageViewportCloneParameters(
    scale: JsNumber? = null,
    rotation: JsNumber? = null,
    offsetX: JsNumber? = null,
    offsetY: JsNumber? = null,
    dontFlip: Boolean? = null,
) = unsafeJso<PageViewportCloneParameters> {
    scale?.let { this.scale = it }
    rotation?.let { this.rotation = it }
    offsetX?.let { this.offsetX = it }
    offsetY?.let { this.offsetY = it }
    dontFlip?.let { this.dontFlip = it }
}

@JsExport.Ignore
inline fun GetOptionalContentConfigParameters(
    intent: String? = null,
) = unsafeJso<GetOptionalContentConfigParameters> {
    intent?.let { this.intent = it }
}
