package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.DownloadProgress
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.util.SupportedMimeTypes
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.ktor.http.*
import kotlin.reflect.KClass
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi

interface ImageTimelineElementDetailsView :
    TimelineElementDetailsView<RoomMessageTimelineElementViewModel.FileBased.Image>

class ImageTimelineElementDetailsViewImpl : ImageTimelineElementDetailsView {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.Image> =
        RoomMessageTimelineElementViewModel.FileBased.Image::class

    // JPEG, PNG, BMP, WEBP (based on decodeToImageBitmap())
    override fun supportsMimeType(mimeType: ContentType): Boolean = SupportedMimeTypes.isSupportedImage(mimeType)

    @OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
    @Composable
    override fun create(
        element: RoomMessageTimelineElementViewModel.FileBased.Image,
        onSave: () -> Unit,
        onClose: () -> Unit,
    ) {
        val i18n = DI.get<I18nView>()
        val media = element.loadMediaResultBytes.collectAsState().value
        val progress = element.loadMediaProgress.collectAsState().value
        val error = element.loadMediaError.collectAsState().value

        val offset = remember { mutableStateOf(Offset(0f, 0f)) }
        val zoom = remember { mutableStateOf(1f) }
        val canZoom = remember { mutableStateOf(false) }

        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            // note: scale goes by factor, not an absolute difference, so we need to multiply it
            // for this example, we don't allow downscaling, so cap it to 1f
            zoom.value = (zoom.value * zoomChange).coerceIn(0.2f, 4f)
            offset.value += offsetChange
        }

        LaunchedEffect(Unit) { element.loadMedia() }

        FileBasedDetailsDialog(element, onSave, onClose, additionalButtons = { ZoomButtons(zoom) }) {
            // we need focus in the box to capture key events
            val focusRequester = remember { FocusRequester() }
            BoxWithConstraints(Modifier.zIndex(0.0f)) {
                with(LocalDensity.current) {
                    val bitmap =
                        remember(media) {
                            media?.toImageBitmap(
                                width = this@BoxWithConstraints.maxWidth.roundToPx(),
                                height = this@BoxWithConstraints.maxHeight.roundToPx(),
                            )
                        }
                    Box(
                        Modifier.fillMaxSize()
                            .focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                canZoom.value = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                                false
                            }
                            .zoomModifier(focusRequester, canZoom, zoom)
                            // performance when image is rendered with no alpha channel
                            .background(color = Color.Black)
                            .transformable(state = state),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (media != null && bitmap == null) { // error decoding the image
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                            ) {
                                Icon(
                                    MaterialTheme.messengerIcons.typeFile,
                                    i18n.commonFile(),
                                    Modifier.size(96.dp).align(Alignment.CenterHorizontally),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(i18n.imageCouldNotBeLoaded(), color = Color.White)
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap,
                                "",
                                Modifier.graphicsLayer {
                                    translationX = offset.value.x
                                    translationY = offset.value.y
                                    scaleX = zoom.value
                                    scaleY = zoom.value
                                },
                            )
                        }
                        progress?.let {
                            if (media == null) {
                                DownloadProgress(it, element::cancelLoadMedia)
                            }
                        }
                        if (media == null && progress == null) {
                            Box(modifier = Modifier.align(Alignment.Center)) {
                                Column {
                                    Icon(
                                        MaterialTheme.messengerIcons.typeImage,
                                        i18n.commonImage(),
                                        Modifier.size(96.dp).align(Alignment.CenterHorizontally),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                    if (error != null) {
                                        Text(error, color = MaterialTheme.colorScheme.onBackground)
                                    } else
                                        Text(
                                            i18n.imageCouldNotBeLoaded(),
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                delay(200) // focusRequester needs some time to initialize
                focusRequester.requestFocus()
            }
        }
    }
}
