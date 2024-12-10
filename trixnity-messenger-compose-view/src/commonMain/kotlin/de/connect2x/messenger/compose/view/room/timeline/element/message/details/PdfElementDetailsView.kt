package de.connect2x.messenger.compose.view.room.timeline.element.message.details

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.common.blockPointerInput
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import net.folivo.trixnity.client.media.PlatformMedia
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class PdfElementDetailsView : ElementDetailsView<RoomMessageTimelineElementViewModel.FileBased.File> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.File> =
        RoomMessageTimelineElementViewModel.FileBased.File::class

    override val supportedMimeTypes: List<String> = listOf(
        "application/pdf",
    )

    @OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class)
    @Composable
    override fun create(
        element: RoomMessageTimelineElementViewModel.FileBased.File,
        onSave: () -> Unit,
        onClose: () -> Unit,
    ) {
        val media = element.downloadMedia.collectAsState().value
        val progress = element.downloadMediaProgress.collectAsState().value
        val (error, setError) = remember { mutableStateOf<String?>(null) }
        var zoom by remember { mutableStateOf(1.0f) }
        val i18n = DI.current.get<I18nView>()

        LaunchedEffect(Unit) {
            element.downloadMedia()
        }
        LaunchedEffect(Unit) {
            element.downloadMediaError.collect { setError(it) }
        }

        ElementDetailsDialog(onClose) {
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
                                    modifier = Modifier.padding(horizontal = 8.dp).buttonPointerModifier(),
                                    onClick = { onClose() },
                                ) {
                                    Text(i18n.commonClose())
                                }
                                Text(element.name)
                            }
                            Button(
                                modifier = Modifier.padding(horizontal = 8.dp).buttonPointerModifier(),
                                onClick = { onSave() }
                            ) {
                                Text(i18n.downloadMessage())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    modifier = Modifier.padding(horizontal = 8.dp).buttonPointerModifier(),
                                    onClick = {
                                        val newZoom = min(4.0f, zoom * 1.33f)
                                        zoom = if (newZoom > 1f && zoom < 1f) 1f else newZoom
                                    }) {
                                    Text("+")
                                }
                                Text("${ceil(zoom * 100)}%")
                                Button(
                                    modifier = Modifier.padding(horizontal = 8.dp).buttonPointerModifier(),
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
                        when {
                            error != null -> {
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
                                    Text(error)
                                }
                            }

                            progress != null -> {
                                DownloadProgress(progress, element::cancelDownloadMedia)
                            }

                            media != null -> PDFReader(media, zoom) {
                                setError(it ?: i18n.fileCouldNotBeLoaded())
                            }

                            else -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun PDFReader(media: PlatformMedia, scale: Float = 1f, onError: (String?) -> Unit)
