package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogContent
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedAdaptiveDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

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
        val byteArray = avatarCutterViewModel.avatarImage.collectAsState().value

        LaunchedEffect(byteArray) {
            bitmap = byteArray?.toImageBitmap() ?: run {
                log.error { "failed to create bitmap image " }
                null
            }
        }

        ThemedAdaptiveDialog({ avatarCutterViewModel.cancel() }) {
            AdaptiveDialogHeader(onClose = avatarCutterViewModel::cancel) {
                Text(avatarCutterViewModel.avatarCutterHeading)
            }
            AdaptiveDialogContent {
                error?.let { ErrorView(it) }
                bitmap?.let { bitmap ->
                    Box(Modifier.weight(1.0f)) {
                        Image(
                            bitmap,
                            i18n.commonAvatar(),
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxSize()
                                .circleCrop(
                                    MaterialTheme.components.adaptiveDialog.container.color,
                                    bitmap.width,
                                    bitmap.height,
                                )
                        )
                    }
                }
            }

            AdaptiveDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = avatarCutterViewModel::accept,
                    enabled = bitmap != null && !upload,
                ) {
                    if (upload) {
                        ThemedProgressIndicator(
                            style = MaterialTheme.components.extraSmallCircularProgressIndicator.copy(
                                size = MaterialTheme.components.primaryButton.iconSize,
                                padding = PaddingValues(0.dp)
                            )
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.Companion.size(MaterialTheme.components.primaryButton.iconSize)
                        )
                    }
                    Spacer(Modifier.Companion.size(MaterialTheme.components.commonButton.iconSpacing))
                    Text(i18n.commonAccept())
                }
            }
        }
    }
}

@Composable
private fun Modifier.circleCrop(
    color: Color,
    contentWidth: Int,
    contentHeight: Int,
) = drawWithContent {

    drawContent()
    drawIntoCanvas {
        val (canvasWidth, canvasHeight) = size
        val contentAspect = contentWidth.toFloat() / contentHeight.toFloat()
        val canvasAspect = canvasWidth / canvasHeight

        val scaledContentWidth = if (contentAspect >= canvasAspect) canvasWidth else canvasHeight * contentAspect
        val scaledContentHeight = if (contentAspect >= canvasAspect) canvasWidth / contentAspect else canvasHeight

        clipPath(
            path = Path().apply {
                addOval(
                    Rect(
                        center = center,
                        radius = minOf(scaledContentWidth, scaledContentHeight) / 2f,
                    )
                )
            },
            clipOp = ClipOp.Difference,
        ) {
            drawRect(color, alpha = 0.5f)
        }
    }
}
