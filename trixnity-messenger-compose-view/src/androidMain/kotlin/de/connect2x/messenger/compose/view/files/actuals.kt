package de.connect2x.messenger.compose.view.files

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.net.toUri
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.UriFileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.files.PdfDocumentViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.utils.ByteArrayFlow
import okio.FileSystem
import simpleVerticalScrollbar
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min


private val log = KotlinLogging.logger { }
actual fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap? {
    val bitmap: Bitmap? = try {
        BitmapFactory.decodeByteArray(encodedImageData, 0, encodedImageData.size)
    } catch (e: Exception) {
        log.error(e) { "cannot create imageBitmapFromBytes" }
        null
    }
    return bitmap?.asImageBitmap()
}

actual fun mouseEventsForImageOverlay(
    maxWidth: Float,
    maxHeight: Float,
    maxBoundsImage: Offset,
    scale: MutableState<Float>,
    move: MutableState<Offset>,
    xMin: MutableState<Float>,
    yMin: MutableState<Float>,
): Modifier {
    // do nothing in Android
    return Modifier
}

@Composable
actual fun VideoPlayer(width: Float, height: Float, url: String) {
    // TODO add impl
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
    var disposed by remember { mutableStateOf(false) } // only download file once
    if (error != null) AlertDialog(
        modifier = Modifier.defaultMinSize(minWidth = 400.dp),
        onDismissRequest = onCloseSaveFileDialog,
        title = { Text(i18n.fileDialogDownloadErrorSave()) },
        dismissButton = {
            Button(
                onCloseSaveFileDialog,
                Modifier.buttonPointerModifier()
            ) { Text(i18n.commonOk()) }
        },
        confirmButton = {},
        shape = RoundedCornerShape(8.dp),
        text = { Text(error) },
    )

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (disposed) return@LaunchedEffect
        disposed = true
        downloadFile { byteArrayFlow ->
            withContext(Dispatchers.IO) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        mimeType ?: ContentType.Application.OctetStream.toString()
                    )
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                var uri: Uri? = null

                runCatching {
                    with(context.contentResolver) {
                        if (Build.VERSION.SDK_INT < 29) {
                            val permission =
                                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            if (permission == PackageManager.PERMISSION_DENIED) {
                                throw IOException("Insufficient permissions to save files.")
                            }
                            Uri.fromFile(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    .resolve(fileName)
                            )
                        } else {
                            insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        }?.also {
                            uri = it // Keep uri reference so it can be removed on failure
                            openOutputStream(it)?.use { stream ->
                                byteArrayFlow.collect {
                                    stream.write(it)
                                }
                            }?.also {
                                uri?.let { uri1 ->
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        type = mimeType.toString()
                                        uri = uri1
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (exc: ActivityNotFoundException) {
                                        println(exc)
                                    }
                                }
                            } ?: throw IOException("Failed to open output stream.")
                        } ?: throw IOException("Failed to create new MediaStore record.")
                    }
                }.getOrElse {
                    // Don't leave an orphan entry in the MediaStore
                    uri?.let {
                        context.contentResolver.delete(it, null, null)
                    }

                    throw it
                }

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
    val context = LocalContext.current
    val i18n = DI.get<I18n>()
    val i18nView = DI.get<I18nView>()
    val visualMediaResult = remember { mutableStateOf<Uri?>(null) }
    val fileAttachmentResult = remember { mutableStateOf<Uri?>(null) }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        visualMediaResult.value = it
    }

    val fileAttachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        fileAttachmentResult.value = it
    }

    if (mode == LoadFileMode.Picture) {
        LaunchedEffect(mediaLauncher) {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    } else {
        val offsetY = with(LocalDensity.current) { -(98.dp).roundToPx() }
        Popup(
            alignment = Alignment.CenterEnd,
            offset = IntOffset(0, offsetY),
            onDismissRequest = onCloseLoadFileDialog,
        ) {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    UploadButton(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = i18nView.fileDialogLoadFileButton(),
                        iconButtonClick = {
                            fileAttachmentLauncher.launch((arrayOf("*/*")))
                        }
                    )
                    UploadButton(
                        Icons.Default.Image,
                        i18nView.fileDialogLoadImageButton(),
                        iconButtonClick = {
                            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }

                    )
                }
            }
        }
    }
    fileAttachmentResult.value?.let { fileAttachmentUri ->
        onFileSelect(UriFileDescriptor(context = context, fileUri = fileAttachmentUri, i18n = i18n))
        onCloseLoadFileDialog()
    }
    visualMediaResult.value?.let { uri ->
        onFileSelect(UriFileDescriptor(context, fileUri = uri, i18n = i18n))
        onCloseLoadFileDialog()
    }
}

@Composable
fun UploadButton(
    imageVector: ImageVector,
    contentDescription: String,
    iconButtonClick: () -> Unit,
) {
    IconButton(
        onClick = iconButtonClick,
        Modifier.size(60.dp).clip(CircleShape)
    ) {
        Box(Modifier.fillMaxSize()) {
            Icon(
                imageVector,
                contentDescription,
                Modifier.align(Alignment.Center).size(24.dp),
            )
        }
    }
}

actual fun getClipboardFile(fileSystem: FileSystem): FileDescriptor? {
    return null
}

@Composable
actual fun PDFReader(documentViewModel: PdfDocumentViewModel, scale: Float) {
    val i18n = DI.current.get<I18n>()
    val i18nView = DI.current.get<I18nView>()
    val document = documentViewModel.documentFlow.collectAsState().value
    val mediaError = documentViewModel.error.collectAsState().value
    var reader by remember { mutableStateOf<PdfRender?>(null) }
    val filename = documentViewModel.fileName
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(Unit) {
        onDispose {
            reader?.close()
        }
    }

    var readerError by remember { mutableStateOf<String?>(null) }
    var forceReloadFile by remember { mutableStateOf(false) }

    if (reader == null && document != null) LaunchedEffect(filename, forceReloadFile) {
        val tempForceReload = forceReloadFile
        forceReloadFile = false
        readerError = null
        saveToCache(
            context, document, filename, tempForceReload,
            onCompletion = {
                try {
                    reader = PdfRender(
                        fileDescriptor = context.contentResolver
                            .openFileDescriptor(it, "r")!!,
                    )
                } catch (e: Exception) {
                    reader = null
                    forceReloadFile = true
                    readerError = i18n.mediaCouldNotBeRead()
                    // TODO: check file hash to avoid endless reload loops for broken files
                }
            },
            onFailure = {
                reader = null
                readerError = i18n.mediaCouldNotBeRead()
            },
        )
    }

    Box(Modifier
        .fillMaxSize()
        .onSizeChanged { viewSize = it }
    ) {
        (mediaError ?: readerError)?.let {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(32.dp)
                    .background(Color.Cyan),
            ) { Text(it) }
        } ?: reader?.let { pdfReader ->
            val dwidth: Float = pdfReader.documentWidth?.toFloat()
                ?: return
            val maxDpi = 1f / dwidth * 1800f
            val newDpi = (viewSize.width / dwidth * scale / density * 2f).coerceAtMost(maxDpi)
            if (pdfReader.dpi != newDpi) {
                pdfReader.dpi = newDpi
            }
            val lazyListState = rememberLazyListState()
            val horizontalScroll = rememberScrollState()
            LazyColumn(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .simpleVerticalScrollbar(lazyListState, MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                state = lazyListState,
                content = {
                    items(count = pdfReader.pageCount, key = { it }) { pageId ->
                        pdfReader[pageId]?.pageContent?.let { img ->
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
                }
            )
            HorizontalScrollbar(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalScroll,
            )
        } ?: Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
    }
}

suspend fun saveToCache(
    context: Context,
    bytes: ByteArrayFlow,
    fileName: String,
    forceReload: Boolean = false,
    onCompletion: (Uri) -> Unit,
    onFailure: ((Throwable) -> Unit)?,
) {
    var uri: Uri? = null
    try {
        val tempFileName = "$fileName.temp"
        val file = File(context.cacheDir, tempFileName)
        file.toUri().let {
            uri = it // Keep uri reference so it can be removed on failure.
            if (!file.exists() || forceReload) {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    // TODO: use stream.writeBytes once Trixnity SDK allows for it.
                    bytes.collect { bytes ->
                        stream.write(bytes)
                    }
                }
            }
            onCompletion(it)
        }
    } catch (e: Exception) {
        try {
            context.contentResolver.delete(uri!!, null, null)
        } catch (_: Exception) {
        }
        log.error { e }
        onFailure?.let { it(e) }
    }
}

// https://medium.com/telepass-digital/how-to-show-a-pdf-with-jetpack-compose-74fc773adbd0
internal class PdfRender(private val fileDescriptor: ParcelFileDescriptor) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount

    private val pageCacheSize: Int
        get() = max(2f, min(16f, 8f / (dpi ?: 1f))).toInt()

    private var _dpi: Float? = null
    var dpi: Float?
        get() = _dpi
        set(value) {
            clearPageCache()
            _dpi = value
        }

    private var pageCache = mutableMapOf<Int, Page>()
    operator fun get(pageId: Int): Page? {
        trimPageCache()
        return try {
            if (!pageCache.containsKey(pageId)) pageCache[pageId] = Page(
                pageId = pageId,
                pdfRenderer = pdfRenderer,
                dpi = _dpi ?: 1f,
                { pageCache.size },
            )
            pageCache[pageId]
        } catch (e: Exception) {
            log.error { e }
            null
        }
    }

    private fun trimPageCache() {
        pageCache.toList().sortedBy { it.second.creationTime }
            .subList(0, Math.max(0, pageCache.size - pageCacheSize))
            .forEach { pageCache.remove(it.first) }
    }

    val documentWidth: Int?
        get() = _documentWidth
            ?: this[0]?.originalWidth?.also {
                if (it > 0) _documentWidth = it
            }
    private var _documentWidth: Int? = null

    fun close() {
        clearPageCache()
        pdfRenderer.close()
        fileDescriptor.close()
    }

    private fun clearPageCache() {
        pageCache.values.forEach {
            it.recycle()
        }
        pageCache.clear()
    }

    class Page(
        val pageId: Int,
        private val pdfRenderer: PdfRenderer,
        private val dpi: Float,
        private val getCacheSize: () -> Int,
    ) {
        val pageContent get() = _pageContent.asImageBitmap()
        private val _pageContent = createBitmap()
        val creationTime = System.currentTimeMillis()
        var originalWidth: Int = 0

        private fun createBitmap(): Bitmap {
            val newBitmap: Bitmap
            pdfRenderer.openPage(pageId).use { currentPage ->
                log.debug {
                    "render pdf page $pageId " +
                            "to bitmap (${currentPage.width}x${currentPage.height}) " +
                            "at scale factor: $dpi " +
                            "with ${getCacheSize()} pages already cached"
                }
                originalWidth = currentPage.width
                newBitmap = createBlankBitmap(
                    width = (currentPage.width * dpi).toInt(),
                    height = (currentPage.height * dpi).toInt(),
                )
                currentPage.render(
                    newBitmap, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                )
            }
            return newBitmap
        }

        fun recycle() {
            _pageContent.recycle()
        }

        private fun createBlankBitmap(
            width: Int,
            height: Int,
        ): Bitmap = androidx.core.graphics.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888,
        ).apply {
            val canvas = Canvas(this)
            canvas.drawBitmap(this, 0f, 0f, null)
        }
    }
}
