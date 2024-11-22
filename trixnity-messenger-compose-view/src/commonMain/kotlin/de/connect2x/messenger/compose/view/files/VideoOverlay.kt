package de.connect2x.messenger.compose.view.files

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.blockPointerInput
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.files.VideoViewModel
import kotlinx.coroutines.delay


interface VideoOverlayView {
    @Composable
    fun create(videoViewModel: VideoViewModel)
}

@Composable
fun VideoOverlay(videoViewModel: VideoViewModel) {
    DI.get<VideoOverlayView>().create(videoViewModel)
}

class VideoOverlayViewImpl : VideoOverlayView {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun create(videoViewModel: VideoViewModel) {
        val i18n = DI.get<I18nView>()
        val video = videoViewModel.video.collectAsState()
        val progress = videoViewModel.progress.collectAsState()
        val error = videoViewModel.error.collectAsState().value
        val isFocused = IsFocused.current

        // we need focus in the box to capture key events
        val focusRequester = remember { FocusRequester() }

        BoxWithConstraints {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    // performance when image is rendered with no alpha channel
                    .background(color = if (video.value == null) MaterialTheme.colorScheme.background else Color.Black)
                    .blockPointerInput()
                    .onKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                            videoViewModel.closeMedia()
                            true
                        } else {
                            false
                        }
                    }
            ) {
                video.value?.let {
                    Box(Modifier.align(Alignment.Center).focusable(false)) {
                        with(LocalDensity.current) {
                        // FIXME stream video player
//                        VideoPlayer(
//                            this@BoxWithConstraints.maxWidth.toPx(),
//                            this@BoxWithConstraints.maxHeight.toPx(),
//                            it
//                        )
                        }
                    }
                }
                //TODO Remove once video playing works
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                ) {
                    Icon(MaterialTheme.messengerIcons.typeVideo, i18n.commonVideo(), Modifier.size(96.dp))
                    Text(i18n.fileOverlayPreviewNotSupported())
                }
                //TODO Uncomment once video playing works
                /*progress.value?.let {
                    if (video.value == null) {
                        DownloadProgress(it, videoViewModel::cancelMediaDownload)
                    }
                }
                if (video.value == null && progress.value == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                    ) {
                        Icon(MaterialTheme.messengerIcons.typeVideo, i18n.commonVideo(), Modifier.size(96.dp))
                        if (error != null) {
                            Text(error)
                        } else Text(i18n.videoCouldNotBeLoaded())
                    }

                }*/
                // TODO does not work
                //  see https://github.com/JetBrains/compose-jb/issues/1087 as a workaround, we need to render buttons in Swing
                FlowRow(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalArrangement = Arrangement.Top
                ) {
                    IconButton(
                        { videoViewModel.downloadMedia() },
                        Modifier.buttonPointerModifier()
                    ) {
                        Icon(Icons.Default.Download, i18n.downloadMessage())
                    }
                    IconButton(
                        { videoViewModel.closeMedia() },
                        Modifier.buttonPointerModifier()
                    ) {
                        Icon(Icons.Default.Close, i18n.commonClose(), tint = Color.LightGray)
                    }
                }
            }
        }

        LaunchedEffect(isFocused) {
            delay(200) // focusRequester needs some time to initialize
            focusRequester.requestFocus()
        }
    }
}
