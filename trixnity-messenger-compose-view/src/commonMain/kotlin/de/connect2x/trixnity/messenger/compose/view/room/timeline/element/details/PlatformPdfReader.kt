package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import de.connect2x.trixnity.client.media.PlatformMedia
import org.koin.core.module.Module

interface PDFReader {
    suspend fun getPage(pageId: Int, dpi: Float): ImageBitmap?

    fun onDispose()

    val numOfPages: MutableState<Int?>
    val documentWidth: MutableState<Int?>
}

interface PDFReaderFactory {

    suspend fun create(media: PlatformMedia, onError: (String?) -> Unit): PDFReader
}

expect fun getPlatformPdfReaderModule(): Module
