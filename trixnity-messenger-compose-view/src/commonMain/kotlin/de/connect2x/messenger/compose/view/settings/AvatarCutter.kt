package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Popup
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.files.imageBitmapFromBytes
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.checkFileSizeExceedsLimit
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.utils.toByteArray

private val log = KotlinLogging.logger {}

interface AvatarCutterView {
    @Composable
    fun create(avatarCutterViewModel: AvatarCutterViewModel)
}

@Composable
fun AvatarCutter(avatarCutterViewModel: AvatarCutterViewModel) {
    DI.get<AvatarCutterView>().create(avatarCutterViewModel)
}

class AvatarCutterViewImpl : AvatarCutterView {
    @Composable
    override fun create(avatarCutterViewModel: AvatarCutterViewModel) {
        val i18n = DI.get<I18nView>()
        val upload = avatarCutterViewModel.upload.collectAsState().value
        val error = avatarCutterViewModel.error.collectAsState().value
        var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        val maxAllowedSize = 10

        if (!checkFileSizeExceedsLimit(avatarCutterViewModel.file.fileSize, maxAllowedSize)) {
            LaunchedEffect(true) {
                val byteArray = avatarCutterViewModel.file.content.toByteArray()
                bitmap = imageBitmapFromBytes(byteArray)
            }

            bitmap?.let { bitmap ->
                val (maxWidth, maxHeight) = if (bitmap.width > bitmap.height) Pair(800.dp, 600.dp) else Pair(
                    600.dp,
                    800.dp
                )

                val maxScaleX = maxWidth / bitmap.width
                val maxScaleY = maxHeight / bitmap.height

                val scale = min(maxScaleX, maxScaleY)

                val width = scale * bitmap.width
                val height = scale * bitmap.height

                log.debug { "image (${bitmap.width},${bitmap.height}), scale ($scale), dim ($width,$height)" }
                Popup(onDismissRequest = { avatarCutterViewModel.cancel() }) {
                    Box(Modifier.fillMaxSize()) {
                        BoxWithConstraints(
                            Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(8.dp))
                                .width(maxWidth)
                        ) {
                            Box(Modifier.background(Color.Black)) {
                                Column {
                                    AvatarCutterHeader(avatarCutterViewModel)

                                    error?.let { ErrorView(it) }

                                    Box {
                                        Image(
                                            bitmap,
                                            i18n.commonAvatar(),
                                            Modifier
                                                .align(Alignment.Center)
                                                .height(height)
                                                .width(width)
                                                .clip(RectangleShape)
                                        )

                                        log.debug { "maxWidth (${this@BoxWithConstraints.maxWidth}), maxHeight (${this@BoxWithConstraints.maxHeight})" }
                                        val scaleX = this@BoxWithConstraints.maxWidth / width
                                        val scaleY = this@BoxWithConstraints.maxHeight / height
                                        val scaleDiameter = when {
                                            scaleX < 1f && scaleX < scaleY -> scaleX
                                            scaleY < 1f -> scaleY
                                            else -> 1f
                                        }

                                        val diameter = (if (height >= width) width else height) * scaleDiameter

                                        Box(
                                            Modifier
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .width(diameter)
                                                .height(diameter)
                                                .background(Color.White.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }

                            FloatingActionButton(
                                avatarCutterViewModel::accept,
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 18.dp, end = 18.dp)
                                    .buttonPointerModifier(),
                            ) {
                                if (upload) CircularProgressIndicator()
                                else Icon(Icons.Default.Check, i18n.commonOk())
                            }
                        }
                    }
                }
            } ?: run {
                log.error { "failed to create bitmap image " }
            }
        } else {
            // TODO: show error dialog
            log.warn { "Size is greater $maxAllowedSize MB" }
        }
    }
}

@Composable
fun AvatarCutterHeader(avatarCutterViewModel: AvatarCutterViewModel) {
    val i18n = DI.get<I18nView>()
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(start = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            avatarCutterViewModel.avatarCutterHeading,
            Modifier.weight(1.0f, fill = true),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        IconButton(
            avatarCutterViewModel::cancel,
            Modifier.buttonPointerModifier()
        ) {
            Icon(Icons.Default.Close, i18n.commonCancel())
        }
    }
}
