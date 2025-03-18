package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import net.folivo.trixnity.client.media.PlatformMedia
import kotlin.reflect.KClass


class PdfTimelineElementDetailsView : TimelineElementDetailsView<RoomMessageTimelineElementViewModel.FileBased.File> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.File> =
        RoomMessageTimelineElementViewModel.FileBased.File::class

    override val supportedMimeTypes: List<String> = listOf(
        "application/pdf",
    )

    @Composable
    override fun create(
        element: RoomMessageTimelineElementViewModel.FileBased.File,
        onSave: () -> Unit,
        onClose: () -> Unit,
    ) {
        val media = element.downloadMediaResult.collectAsState().value
        val progress = element.downloadMediaProgress.collectAsState().value
        val (error, setError) = remember { mutableStateOf<String?>(null) }
        var zoom = remember { mutableStateOf(1.0f) }
        val canZoom = remember { mutableStateOf(false) }
        val i18n = DI.current.get<I18nView>()

        LaunchedEffect(Unit) {
            element.downloadMedia()
        }
        LaunchedEffect(Unit) {
            element.downloadMediaError.collect { setError(it) }
        }

        FileBasedDetailsDialog(element, onSave, onClose, additions = { ZoomButtons(zoom) }) {
            val focusRequester = remember { FocusRequester() }
            Column {
                Box(
                    Modifier
                        .background(Color.Black)
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            canZoom.value = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                            true
                        }
                        .zoomModifier(focusRequester, canZoom, zoom),
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

                        media != null -> PDFReader(media, zoom.value) {
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

@Composable
expect fun PDFReader(media: PlatformMedia, scale: Float = 1f, onError: (String?) -> Unit)
