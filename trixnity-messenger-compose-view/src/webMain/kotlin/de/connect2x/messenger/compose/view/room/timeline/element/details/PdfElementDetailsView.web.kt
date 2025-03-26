package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.files.GlobalWorkerOptions
import de.connect2x.messenger.compose.view.files.PdfReaderWeb
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import net.folivo.trixnity.client.media.opfs.OpfsPlatformMedia
import web.blob.Blob

@OptIn(DelicateCoroutinesApi::class)
@Composable
actual fun PDFReader(
    media: PlatformMedia,
    scale: Float,
    isZooming: Boolean,
    offset: MutableState<Offset>,
    state: TransformableState,
    onError: (String?) -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
    val density = LocalDensity.current.density
    val dpi = remember { mutableStateOf<Float?>(null) }
    val temporaryFile = remember { mutableStateOf<Blob?>(null) }
    val fileDeleteFunction = remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    val reader = remember { mutableStateOf<PdfReaderWeb?>(null) }
    val viewSize = remember { mutableStateOf(IntSize.Zero) }

    GlobalWorkerOptions.workerSrc = "./pdf.worker.mjs"
    LaunchedEffect(Unit) {
        val temporaryFileResult =
            (media as? OpfsPlatformMedia)?.getTemporaryFile() ?: (media as IndexeddbPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess == true) {
            val newTemporaryFile = when (val result = temporaryFileResult.getOrNull()) {
                is OpfsPlatformMedia.TemporaryFile -> {
                    fileDeleteFunction.value = result::delete
                    result.file
                }

                is IndexeddbPlatformMedia.TemporaryFile -> {
                    fileDeleteFunction.value = result::delete
                    result.file
                }

                else -> null
            }
            try {
                temporaryFile.value = newTemporaryFile
                val createdReader = newTemporaryFile?.let { PdfReaderWeb(it) }
                reader.value = createdReader
            } catch (exception: Exception) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            reader.value?.clearCache()
            GlobalScope.launch { fileDeleteFunction.value?.invoke() }
        }
    }

    LaunchedEffect(reader.value?.documentWidth?.value, scale, viewSize) {
        reader.value?.documentWidth?.value?.toFloat()?.let {
            val maxDpi = 1f / it * 64f * 3600f
            dpi.value = (viewSize.value.width / it * scale / density * 64f).coerceAtMost(maxDpi)
        }
    }

    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(offset.value) {
        lazyListState.scrollBy(-offset.value.y)
        horizontalScroll.scrollBy(-offset.value.x)
        offset.value = Offset.Zero
    }
    Box(Modifier.fillMaxSize().onSizeChanged { viewSize.value = it }, contentAlignment = Alignment.Center) {
        reader.value?.let { reader ->
            val pageCount = reader.pageSize.value
            pageCount?.let {
                LazyColumn(
                    modifier = Modifier
                        .horizontalScroll(horizontalScroll)
                        .fillMaxSize()
                        .transformable(state),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(it) { pageId ->
                        val dpi = dpi.value
                        dpi?.let {
                            val currentBitmap = reader.cache["${pageId + 1}"]?.second?.collectAsState()?.value
                            LaunchedEffect(dpi) {
                                reader.renderPageWhenNecessary(pageId, dpi, scale)
                            }
                            if (currentBitmap != null) {
                                Image(
                                    currentBitmap,
                                    contentDescription = i18n.fileOverlayPdfPageDescriptor(pageId),
                                    modifier = Modifier
                                        .background(color = MaterialTheme.colorScheme.background) // Avoid performance drops on transparent images.
                                        .width(viewSize.value.width.dp / density * scale - MaterialTheme.messengerDpConstants.middle * 2),
                                    contentScale = ContentScale.FillWidth,
                                )
                            } else Box(
                                Modifier.height((reader.documentWidth.value?.dp?.times(2)) ?: 1000f.dp).width(
                                    reader.documentWidth.value?.dp ?: 1000f.dp
                                ), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
        if (dpi.value == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        HorizontalScrollbar(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalScroll)
        VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), lazyListState, false)
    }
}

