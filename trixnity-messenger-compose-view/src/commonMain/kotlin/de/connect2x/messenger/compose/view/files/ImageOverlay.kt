package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.media.ImageViewModel
import kotlinx.coroutines.delay


interface ImageOverlayView {
    @Composable
    fun create(imageViewModel: ImageViewModel)
}

@Composable
fun ImageOverlay(imageViewModel: ImageViewModel) {
    DI.get<ImageOverlayView>().create(imageViewModel)
}

class ImageOverlayViewImpl : ImageOverlayView {
    @Composable
    override fun create(imageViewModel: ImageViewModel) {
        val i18n = DI.get<I18nView>()
        val image = imageViewModel.image.collectAsState()
        val progress = imageViewModel.progress.collectAsState()
        val scale = remember { mutableStateOf(1f) }
        val move = remember { mutableStateOf(Offset(0f, 0f)) }
        val xMin = remember { mutableStateOf(0f) }
        val yMin = remember { mutableStateOf(0f) }
        val maxBoundsImage = remember { mutableStateOf(Offset(0f, 0f)) }

        // we need focus in the box to capture key events
        val focusRequester = remember { FocusRequester() }

        BoxWithConstraints {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    // performance when image is rendered with no alpha channel
                    .background(color = if (image.value == null) MaterialTheme.colorScheme.background else Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (image.value != null) {
                                val oldScale = scale.value
                                val newScale = oldScale * zoom
                                scale.value = newScale.coerceIn(1f, 10f)

                                xMin.value += maxWidth.toPx() * (oldScale - scale.value) / 2
                                yMin.value += maxHeight.toPx() * (oldScale - scale.value) / 2
                                move.value = Offset(
                                    if (xMin.value < 0) {
                                        (move.value.x + pan.x).coerceIn(xMin.value, -xMin.value)
                                    } else {
                                        move.value.x + pan.x
                                    },
                                    if (yMin.value < 0) {
                                        (move.value.y + pan.y).coerceIn(yMin.value, -yMin.value)
                                    } else {
                                        move.value.y + pan.y
                                    }
                                )
                            }
                        }
                        detectTapGestures { } // prevent interaction with background
                    }
                    .then(with(LocalDensity.current) {
                        mouseEventsForImageOverlay(
                            maxWidth.toPx(),
                            maxHeight.toPx(),
                            maxBoundsImage.value,
                            scale,
                            move,
                            xMin,
                            yMin
                        )
                    }).then(Modifier.onKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                            imageViewModel.closeMedia()
                            true
                        } else {
                            false
                        }
                    })
            ) {
                image.value?.let { imageBitmapFromBytes(it) }?.let { bitmap ->
                    Box(Modifier.align(Alignment.Center)) {
                        maxBoundsImage.value = Offset(bitmap.width.toFloat(), bitmap.height.toFloat())
                        Image(
                            bitmap,
                            "",
                            Modifier.graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                                translationX = move.value.x
                                translationY = move.value.y
                            }
                        )
                    }
                }
                progress.value?.let {
                    if (image.value == null) {
                        DownloadProgress(it, imageViewModel::cancelMediaDownload)
                    }
                }
                if (image.value == null && progress.value == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(MaterialTheme.messengerIcons.typeImage, i18n.commonImage(), Modifier.size(96.dp))
                        Text(i18n.imageCouldNotBeLoaded())
                    }
                }
            }
            IconButton(
                { imageViewModel.closeMedia() },
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .buttonPointerModifier()
            ) {
                Icon(Icons.Default.Close, i18n.commonClose(), tint = Color.LightGray)
            }
        }

        LaunchedEffect(Unit) {
            delay(200) // focusRequester needs some time to initialize
            focusRequester.requestFocus()
        }
    }
}

expect fun mouseEventsForImageOverlay(
    maxWidth: Float,
    maxHeight: Float,
    maxBoundsImage: Offset,
    scale: MutableState<Float>,
    move: MutableState<Offset>,
    xMin: MutableState<Float>,
    yMin: MutableState<Float>,
): Modifier
