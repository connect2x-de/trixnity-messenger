package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.common.blockPointerInput
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.files.PdfDocumentViewModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


interface PdfOverlayView {
    @Composable
    fun create(documentViewModel: PdfDocumentViewModel)
}

@Composable
fun PdfOverlay(documentViewModel: PdfDocumentViewModel) {
    DI.get<PdfOverlayView>().create(documentViewModel)
}

class PdfOverlayViewImpl : PdfOverlayView {
    @Composable
    @OptIn(ExperimentalLayoutApi::class)
    override fun create(documentViewModel: PdfDocumentViewModel) {
        val media = documentViewModel.documentFlow.collectAsState()
        val progress = documentViewModel.progress.collectAsState().value
        val error = documentViewModel.error.collectAsState().value
        println("Error is $error")
        println("Document flow is $media")
        var zoom by remember { mutableStateOf(1.0f) }
        val i18n = DI.current.get<I18nView>()
        BoxWithConstraints {
            Column(
                Modifier.fillMaxSize().blockPointerInput(),
            ) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                ) {
                    FlowRow {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onClick = { documentViewModel.closeMedia() },
                            ) {
                                Text(i18n.commonClose())
                            }
                            Text(documentViewModel.fileName)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onClick = {
                                    val newZoom = min(4.0f, zoom * 1.33f)
                                    zoom = if (newZoom > 1f && zoom < 1f) 1f else newZoom
                                }) {
                                Text("+")
                            }
                            Text("${ceil(zoom * 100)}%")
                            Button(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onClick = {
                                    val newZoom = max(0.1f, zoom * 0.66f)
                                    zoom = if (newZoom < 1f && zoom > 1f) 1f else newZoom
                                }) {
                                Text("-")
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                Box(
                    Modifier
                        .background(Color.Gray)
                        .fillMaxSize()
                        .weight(1f)
                        .focusable()
                ) {
                    if (media.value != null) {
                        PDFReader(documentViewModel, zoom)
                    } else if (progress != null) {
                        DownloadProgress(progress, documentViewModel::cancelMediaDownload)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                        ) {
                            Icon(
                                MaterialTheme.messengerIcons.typeFile,
                                i18n.commonFile(),
                                Modifier.size(96.dp).align(Alignment.CenterHorizontally)
                            )
                            if (error != null) {
                                Text(error)
                            } else Text(i18n.fileCouldNotBeLoaded())
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun PDFReader(documentViewModel: PdfDocumentViewModel, scale: Float = 1f)
