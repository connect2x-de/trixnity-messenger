package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.PathFileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.files.PdfDocumentViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.folivo.trixnity.utils.BYTE_ARRAY_FLOW_CHUNK_SIZE
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.write
import okio.FileSystem
import okio.Path.Companion.toPath
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skia.Image
import simpleVerticalScrollbar
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min


private val log = KotlinLogging.logger {}

actual fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(encodedImageData).toComposeImageBitmap()
    } catch (e: Exception) {
        log.error(e) { "cannot create imageBitmapFromBytes" }
        null
    }
}

private var oldPosition: Offset? = null

@OptIn(ExperimentalComposeUiApi::class)
actual fun mouseEventsForImageOverlay(
    maxWidth: Float,
    maxHeight: Float,
    maxBoundsImage: Offset,
    scale: MutableState<Float>,
    move: MutableState<Offset>,
    xMin: MutableState<Float>,
    yMin: MutableState<Float>,
): Modifier {
    val scaleFactorX = maxBoundsImage.x / maxWidth
    val scaleFactorY = maxBoundsImage.y / maxHeight
    return Modifier.mouseScrollFilter { event, _ ->
        val newScale = when (val delta = event.delta) {
            is MouseScrollUnit.Line -> scale.value - (0.1f * delta.value)
            is MouseScrollUnit.Page -> scale.value - (0.1f * delta.value)
        }
        scale.value = newScale.coerceIn(1f, 10f)
        val width = if (scaleFactorX * scale.value > 1) maxWidth else 0f
        val height = if (scaleFactorY * scale.value > 1) maxHeight else 0f
        xMin.value = width * (scale.value - 1) / 2
        yMin.value = height * (scale.value - 1) / 2

        true
    }.then(Modifier.pointerMoveFilter(onMove = { position: Offset ->
        if (oldPosition == null) {
            oldPosition = position
        }// to get the same amount of distance
        // when scaled, we have to move more pixels
        oldPosition?.let { // to get the same amount of distance
            // when scaled, we have to move more pixels
            val deltaX = it.x - position.x
            val deltaY = it.y - position.y
            val newMoveX =
                move.value.x + (deltaX * scaleFactorX.coerceAtMost(1f) * scale.value) // when scaled, we have to move more pixels
            val newMoveY =
                move.value.y + (deltaY * scaleFactorY.coerceAtMost(1f) * scale.value) // to get the same amount of distance
            move.value = Offset(
                newMoveX.coerceIn(-xMin.value, xMin.value),
                newMoveY.coerceIn(-yMin.value, yMin.value)
            )// to get the same amount of distance
            // when scaled, we have to move more pixels
            oldPosition = position
        }
        true
    }))
}

/**
 * [DesktopVideoPlayer](https://github.com/JetBrains/compose-jb/blob/master/components/VideoPlayer/library/src/desktopMain/kotlin/org/jetbrains/compose/videoplayer/DesktopVideoPlayer.kt)
 */
@Composable
actual fun VideoPlayer(width: Float, height: Float, url: String) {
}

@Composable
actual fun SaveDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    var isOpened by remember { mutableStateOf(false) }
    if (error != null) AlertDialog(
        modifier = Modifier.defaultMinSize(minWidth = 400.dp),
        onDismissRequest = {
            onCloseSaveFileDialog()
        },
        title = { Text(i18n.fileDialogDownloadErrorSave()) },
        dismissButton = {
            Button({
                onCloseSaveFileDialog()
            }, Modifier.buttonPointerModifier()) { Text(i18n.commonOk()) }
        },
        confirmButton = {},
        shape = RoundedCornerShape(8.dp),
        text = { Text(error) },
    )
    LaunchedEffect(Unit) {
        if (isOpened) return@LaunchedEffect
        isOpened = true
        downloadFile {
            val file = FileKit.saveFile(
                baseName = fileName.substringBeforeLast("."),
                extension = fileName.substringAfterLast("."),
                // TODO: set initialDirectory to OS dependent default pictures directory
            )
            try {
                val path = file?.path?.toPath()
                if (path != null) FileSystem.SYSTEM.write(path, it)
                else log.warn { "no valid path selected" }
            } finally {
                onCloseSaveFileDialog()
            }
        }
    }
}

@Composable
actual fun LoadDialog(
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
) {
    val i18n = DI.get<I18nView>()
    var isOpened by remember { mutableStateOf(false) }
    val fileSystem = DI.get<FileSystem>()
    // Due to compose life cycles the launcher needs to be set up even if launch() is skipped.
    val launcher = rememberFilePickerLauncher(
        type = when (mode) {
            LoadFileMode.Picture -> PickerType.Image
            LoadFileMode.AnyFile -> PickerType.File()
        },
        mode = PickerMode.Single,
        title = i18n.fileDialogTitleLoad(),
        // TODO: set initialDirectory to OS dependent default pictures directory
    ) { file ->
        log.debug { "selected file: $file" }
        file?.let {
            file.path?.toPath()
                ?.let { onFileSelect(PathFileDescriptor(it, fileSystem)) }
                ?: run { log.error { "can't resolve path for selected file: $file" } }
            isOpened = false
            onCloseLoadFileDialog()
        }
    }
    LaunchedEffect(Unit) {
        if (isOpened) return@LaunchedEffect
        isOpened = true
        launcher.launch()
    }
}

fun ByteBuffer.toByteArrayFlow(): ByteArrayFlow = flow {
    val buffer = ByteArray(BYTE_ARRAY_FLOW_CHUNK_SIZE.toInt())

    while (remaining() >= buffer.size) {
        get(buffer)
        emit(buffer.copyOf())
    }
    val remaining = remaining()
    get(buffer, 0, remaining)
    emit(buffer.copyOf(remaining))
}

fun InputStream.toByteArrayFlow(): ByteArrayFlow = flow {
    val buffer = ByteArray(BYTE_ARRAY_FLOW_CHUNK_SIZE.toInt())
    var bytesRead: Int

    while (read(buffer).also { bytesRead = it } != -1) {
        emit(buffer.copyOf(bytesRead))
    }
}
    .flowOn(Dispatchers.IO)

private data class RawFileDescriptor(
    override val fileName: String,
    override val fileSize: Int?,
    override val mimeType: ContentType?,
    override val content: ByteArrayFlow,
) : FileDescriptor

actual fun getClipboardFile(fileSystem: FileSystem): FileDescriptor? {
    log.debug { "access clipboard" }
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    clipboard.availableDataFlavors.forEach { flavor ->
        log.info { "primary: ${flavor.primaryType}, sub: ${flavor.subType}, repr: ${flavor.representationClass}" }
    }

    if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
        val files = clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>

        // TODO: find out if multiple files should be supported in a single paste operation
        return files.firstOrNull()?.let { file ->
            return PathFileDescriptor((file as File).absolutePath.toPath(), fileSystem = fileSystem)
        } ?: run {
            log.info { "the selected files list is empty" }
            return null
        }
    }

    for (flavor in clipboard.availableDataFlavors) {
        if (flavor.primaryType != "image") continue
        val data = clipboard.getData(flavor)

        if (flavor.isRepresentationClassInputStream) {
            val inputStream = data as InputStream

            return RawFileDescriptor(
                "Image from Clipboard",
                inputStream.available(),
                ContentType.parse(flavor.mimeType),
                inputStream.toByteArrayFlow()
            )
        } else if (flavor.isRepresentationClassByteBuffer) {
            val byteBuffer = data as ByteBuffer

            return RawFileDescriptor(
                "Image from Clipboard",
                byteBuffer.remaining(),
                ContentType.parse(flavor.mimeType),
                byteBuffer.toByteArrayFlow()
            )
        } else {
            log.warn { "unknown representation class for image: ${flavor.representationClass}" }
        }
    }

    log.info { "content in clipboard was not paste-able" }
    return null
}

@Composable
actual fun PDFReader(documentViewModel: PdfDocumentViewModel, scale: Float) {
    val i18nView = DI.current.get<I18nView>()
    val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()
    val media = documentViewModel.document.collectAsState()
    val error = documentViewModel.error.collectAsState()
    val filename = documentViewModel.fileName
    var document by remember { mutableStateOf<Pair<PDDocument, PDFRenderer>?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var documentWidth: Int? by remember { mutableStateOf(null) }

    val errorText = error.value
    if (errorText != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(32.dp),
        ) { Text(errorText) }
        return
    }

    val renderCache by remember {
        mutableStateOf<MutableMap<String, Pair<Long, ImageBitmap>>>(mutableMapOf())
    }

    DisposableEffect(Unit) {
        onDispose {
            document?.first?.close()
            renderCache.clear()
            document = null
        }
    }

    if (document == null)
        media.value?.let { bytes ->
            val documentData = org.apache.pdfbox.Loader.loadPDF(bytes)
            document = Pair(documentData, PDFRenderer(documentData))
        }
    else if (documentWidth == null) {
        documentWidth = document?.second?.renderImage(0)?.width
    }

    val density = LocalDensity.current.density
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()

    Box(Modifier
        .fillMaxSize()
        .onSizeChanged { viewSize = it }
    ) {
        val documentData = document?.first
        val renderer = document?.second
        if (viewSize != IntSize.Zero && documentWidth != null && documentData != null && renderer != null) {
            val dwidth: Float = documentWidth?.toFloat() ?: 1f
            val maxDpi = 1f / dwidth * 64f * 3600f
            val newDpi = (viewSize.width / dwidth * scale / density * 64f).coerceAtMost(maxDpi)
            LazyColumn(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .simpleVerticalScrollbar(lazyListState, MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                state = lazyListState,
                content = {
                    items(count = documentData.numberOfPages, key = { it }) { pageId ->
                        val cacheKey = "$filename:$pageId:${newDpi.toInt()}"
                        val img = renderCache[cacheKey]?.second
                            ?: renderer.renderImageWithDPI(pageId, newDpi).let {
                                val img = it.toComposeImageBitmap()
                                log.debug {
                                    "render pdf page $pageId " +
                                            "to bitmap (${img.width}x${img.height}) " +
                                            "at scale factor: $newDpi " +
                                            "with ${renderCache.size} pages already cached"
                                }
                                renderCache[cacheKey] = Pair<Long, ImageBitmap>(System.currentTimeMillis(), img)
                                renderCache.toList().sortedBy { it.second.first }
                                    .subList(0, Math.max(0, renderCache.size - pageCacheSize))
                                    .forEach { renderCache.remove(it.first) }
                                img
                            }
                        Image(
                            bitmap = img,
                            contentDescription = i18nView.fileOverlayPdfPageDescriptor(pageId),
                            modifier = Modifier
                                .background(color = Color.White) // Avoid performance drops on transparent images.
                                .width(viewSize.width.dp / density * scale - 16.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            )
        } else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
        HorizontalScrollbar(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalScroll,
        )
    }
}
