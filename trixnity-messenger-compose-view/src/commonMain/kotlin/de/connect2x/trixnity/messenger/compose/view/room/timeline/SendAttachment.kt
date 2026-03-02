package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize

interface SendAttachmentView {
    @Composable
    fun create(sendAttachmentViewModel: SendAttachmentViewModel)
}

@Composable
fun SendAttachment(sendAttachmentViewModel: SendAttachmentViewModel) {
    with(DI.get<SendAttachmentView>()) { create(sendAttachmentViewModel) }
}

class SendAttachmentViewImpl : SendAttachmentView {
    @Composable
    override fun create(sendAttachmentViewModel: SendAttachmentViewModel) {
        val i18n = DI.get<I18nView>()
        val error = sendAttachmentViewModel.error.collectAsState().value
        val fileSize = "(" + (sendAttachmentViewModel.file.fileSize?.let { size -> formatSize(size) }
            ?: i18n.commonUnknown()) + ")"
        val isImage = sendAttachmentViewModel.isImage
        val isVideo = sendAttachmentViewModel.isVideo
        val isAudio = sendAttachmentViewModel.isAudio
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        val fileContent = sendAttachmentViewModel.previewFileContent.collectAsState().value
        val messengerConfiguration = DI.get<MatrixMessengerConfiguration>()

        Column(Modifier.fillMaxSize()) {
            SendAttachmentTitle(sendAttachmentViewModel)
            if (error != null) {
                ErrorView(error)
            } else {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 20.dp)
                        .weight(1.0f, false)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val maxPreviewSize = messengerConfiguration.maxMediaSizeInMemory
                    when {
                        isImage ?: false -> {
                            if (fileContent != null) {
                                LaunchedEffect(isImage) {
                                    imageBitmap = fileContent.toImageBitmap()
                                }
                                imageBitmap?.let {
                                    Image(
                                        it,
                                        i18n.commonAttachment(),
                                        Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .weight(1.0f, false)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Inside,
                                    )
                                }
                            } else {
                                Text(i18n.filePreviewErrorTooBig(maxPreviewSize))
                            }
                        }

                        isVideo ?: false -> FileIcon(MaterialTheme.messengerIcons.typeVideo, i18n.commonVideo())
                        isAudio ?: false -> FileIcon(MaterialTheme.messengerIcons.typeAudio, i18n.commonAudio())
                        else -> FileIcon(MaterialTheme.messengerIcons.typeFile, i18n.commonFile())
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(buildAnnotatedString {
                        append(sendAttachmentViewModel.file.fileName)
                        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
                        append(" ")
                        append(fileSize)
                    })
                }
            }
            Spacer(Modifier.size(20.dp))
            HorizontalDivider(Modifier.fillMaxWidth())

            SendAttachmentSendButton(sendAttachmentViewModel)
        }
    }
}

@Composable
private fun ColumnScope.FileIcon(icon: ImageVector, contentDescription: String) {
    Icon(icon, contentDescription, Modifier.align(Alignment.CenterHorizontally).size(96.dp))
}
