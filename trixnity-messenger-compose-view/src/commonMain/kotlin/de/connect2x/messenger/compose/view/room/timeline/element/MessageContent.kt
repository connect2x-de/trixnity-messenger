package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ClickableText
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.files.SaveFileDialog
import de.connect2x.messenger.compose.view.files.imageBitmapFromBytes
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isDesktop
import de.connect2x.messenger.compose.view.room.timeline.AudioReply
import de.connect2x.messenger.compose.view.room.timeline.FileReply
import de.connect2x.messenger.compose.view.room.timeline.ImageReply
import de.connect2x.messenger.compose.view.room.timeline.ImageReplyDefault
import de.connect2x.messenger.compose.view.room.timeline.LocationReply
import de.connect2x.messenger.compose.view.room.timeline.ReferencedMessagePill
import de.connect2x.messenger.compose.view.room.timeline.TextReply
import de.connect2x.messenger.compose.view.room.timeline.UnknownReply
import de.connect2x.messenger.compose.view.room.timeline.VideoReply
import de.connect2x.messenger.compose.view.room.timeline.VideoReplyDefault
import de.connect2x.messenger.compose.view.room.timeline.element.util.formatMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.mentionsUriHandler
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.AudioMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EmoteMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FallbackMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FileMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ImageMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.LocationMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.NoticeMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TextBasedViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TextMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.VideoMessageViewModel
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

@Composable
fun MessageContent(
    roomMessageViewModel: RoomMessageViewModel,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    when (roomMessageViewModel) {
        is TextMessageViewModel -> MessageText(roomMessageViewModel, onLongPress)
        is EmoteMessageViewModel -> MessageText(roomMessageViewModel, onLongPress)
        is NoticeMessageViewModel -> MessageText(roomMessageViewModel, onLongPress, isNotice = true)
        is RedactedMessageViewModel -> MessageRedacted(roomMessageViewModel)
        is ImageMessageViewModel -> MessageImage(roomMessageViewModel, baseTimelineElementHolderViewModel, onLongPress)
        is VideoMessageViewModel -> MessageVideo(roomMessageViewModel, onLongPress, baseTimelineElementHolderViewModel)
        is AudioMessageViewModel -> MessageAudio(roomMessageViewModel, baseTimelineElementHolderViewModel, onLongPress)
        is FileMessageViewModel -> MessageFile(roomMessageViewModel, baseTimelineElementHolderViewModel, onLongPress)
        is EncryptedMessageViewModel -> EncryptedMessage(roomMessageViewModel)
        is FallbackMessageViewModel -> MessageText(roomMessageViewModel, onLongPress)
        is LocationMessageViewModel -> MessageLocation(roomMessageViewModel, onLongPress)
    }
}

@Composable
private fun MessageText(
    textBasedViewModel: TextBasedViewModel,
    onLongPress: (Offset) -> Unit,
    isNotice: Boolean = false
) {
    if (Platform.current.isDesktop) {
        // on Desktop it makes sense to select text and copy it;
        // on Android, this will consume long tap events, which we use for the context menu
        SelectionContainer {
            MessageTextContent(textBasedViewModel, onLongPress, isNotice)
        }
    } else {
        MessageTextContent(textBasedViewModel, onLongPress, isNotice)
    }
}


@Composable
private fun MessageTextContent(
    textBasedViewModel: TextBasedViewModel,
    onLongPress: (Offset) -> Unit,
    isNotice: Boolean,
) {
    val referencedMessage = textBasedViewModel.referencedMessage.collectAsState().value

    val i18n = DI.get<I18nView>()

    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
        if (textBasedViewModel is NoticeMessageViewModel) {
            Row {
                Icon(Icons.Filled.SmartToy, i18n.automated())
                Text(i18n.automated())
            }

            Spacer(Modifier.size(5.dp))
        }

        if (referencedMessage != null) {
            val sender = referencedMessage.sender
            ReferencedMessagePill(
                senderName = sender.name,
                senderNameColor = MaterialTheme.messengerColors.getUserColor(sender.userId),
                content = {
                    when (referencedMessage) {
                        is ReferencedMessage.ReferencedTextMessage -> TextReply(referencedMessage.messageShortened())
                        is ReferencedMessage.ReferencedImageMessage ->
                            referencedMessage.thumbnail?.let { imageBitmapFromBytes(it) }
                                ?.let { imageBitmap ->
                                    ImageReply(imageBitmap)
                                } ?: ImageReplyDefault(referencedMessage.fileName)

                        is ReferencedMessage.ReferencedVideoMessage ->
                            referencedMessage.thumbnail?.let { imageBitmapFromBytes(it) }
                                ?.let { imageBitmap ->
                                    VideoReply(imageBitmap)
                                } ?: VideoReplyDefault(referencedMessage.fileName)

                        is ReferencedMessage.ReferencedAudioMessage -> AudioReply(referencedMessage.fileName)
                        is ReferencedMessage.ReferencedFileMessage -> FileReply(referencedMessage.fileName)
                        is ReferencedMessage.ReferencedLocationMessage -> LocationReply(
                            referencedMessage.name,
                            referencedMessage.geoUri,
                        )

                        is ReferencedMessage.ReferencedUnknownMessage -> UnknownReply()
                    }
                },
            )
            Spacer(Modifier.size(5.dp))
        }

        val mentions = (textBasedViewModel.mentionsInFormattedBody ?: textBasedViewModel.mentionsInMessage)
            .map {
                it.key to it.value.collectAsState().value
            }.sortedByDescending { it.first.first }

        val message = textBasedViewModel.formattedBody ?: textBasedViewModel.message
        val text = formatMessage(message, mentions, textBasedViewModel)

        val richTextState = rememberRichTextState()
        LaunchedEffect(text) {
            richTextState.setHtml(text)
        }
        richTextState.config.linkColor =
            if (textBasedViewModel.isByMe) MaterialTheme.messengerColors.linkByMe // Inherit link color from Messenger colors
            else MaterialTheme.messengerColors.link

        if (richTextState.toHtml().isNotBlank()) {
            if (mentions.any { it.second != null }) {
                val baseUriHandler = LocalUriHandler.current
                val uriHandler by remember {
                    mentionsUriHandler(baseUriHandler, textBasedViewModel, mentions.map { it.second })
                }

                MessageRichText(uriHandler, richTextState, textBasedViewModel.isByMe, onLongPress)
            } else {
                MessageRichText(LocalUriHandler.current, richTextState, textBasedViewModel.isByMe, onLongPress)
            }
        } else {
            // workaround for 1st rendering cycle where nothing is displayed since the RichText's HTML is set in an effect
            Text(textBasedViewModel.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MessageRichText(uriHandler: UriHandler, state: RichTextState, isByMe: Boolean, onLongPress: (Offset) -> Unit) {
    CompositionLocalProvider(
        LocalUriHandler provides uriHandler
    ) {
        BasicRichText(
            state = state,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = onLongPress
                )
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isByMe) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondary
            )
        )
    }
}

@Composable
private fun MessageRedacted(redactedMessageViewModel: RedactedMessageViewModel) {
    val i18n = DI.get<I18nView>()
    val formattedMessage = redactedMessageViewModel.formattedMessage.collectAsState().value
    Row(Modifier.padding(10.dp)) {
        Icon(
            Icons.Outlined.Delete, i18n.commonDeleted(),
            Modifier.align(Alignment.CenterVertically)
                .size(MaterialTheme.typography.bodySmall.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "$formattedMessage${redactedMessageViewModel.redactedAtDateTime?.let { " ($it)" } ?: ""}",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun EncryptedMessage(encryptedMessageViewModel: EncryptedMessageViewModel) {
    val i18n = DI.get<I18nView>()
    val waitForDecryption = encryptedMessageViewModel.waitForDecryption.collectAsState().value
    if (waitForDecryption) {
        Row(Modifier.padding(10.dp)) {
            Icon(
                Icons.Outlined.Lock, i18n.commonWaiting(),
                Modifier.align(Alignment.CenterVertically)
                    .size(MaterialTheme.typography.bodySmall.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                i18n.messageContentWaitForKeys(),
                Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
            )
        }
    } else {
        Text(
            i18n.messageContentNoDecryption(),
            Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall, // FIXME alpha?
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun MessageImage(
    imageMessageViewModel: ImageMessageViewModel,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    if (baseTimelineElementHolderViewModel is OutboxElementHolderViewModel) {
        OutboxMessageImage(imageMessageViewModel, onLongPress, baseTimelineElementHolderViewModel)
    } else {
        InboxMessageImage(imageMessageViewModel, onLongPress)
    }
}

@Composable
private fun OutboxMessageImage(
    imageMessageViewModel: ImageMessageViewModel,
    onLongPress: (Offset) -> Unit,
    outboxElementHolderViewModel: OutboxElementHolderViewModel
) {
    val uploadProgress = imageMessageViewModel.uploadProgress.collectAsState(null)
    val image = imageMessageViewModel.thumbnail.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 10.dp)
    ) {
        image.value?.let { imageBitmapFromBytes(it) }?.let {
            MessageImage(it, imageMessageViewModel, onLongPress)
        } ?: MessageImageFallback(imageMessageViewModel, onLongPress)
        uploadProgress.value?.let {
            MiddleSpacer()
            Box(Modifier.padding(all = 20.dp)) {
                DownloadProgress(it, color = Color.Black, cancel = { outboxElementHolderViewModel.abortSend() })
            }
        }
    }
}

@Composable
private fun InboxMessageImage(
    imageMessageViewModel: ImageMessageViewModel,
    onLongPress: (Offset) -> Unit
) {
    val i18n = DI.get<I18nView>()
    val uploadProgress = imageMessageViewModel.uploadProgress.collectAsState(null)
    val image = imageMessageViewModel.thumbnail.collectAsState()

    BoxWithConstraints(Modifier.padding(top = 10.dp)) {
        image.value?.let { imageBitmapFromBytes(it) }?.let {
            MessageImage(it, imageMessageViewModel, onLongPress)
        } ?: MessageImageFallback(imageMessageViewModel, onLongPress)
        uploadProgress.value?.let {
            if (image.value == null) {
                val height =
                    with(LocalDensity.current) {
                        imageMessageViewModel.getHeight(maxWidth.toPx()).toDp()
                    }
                val width =
                    with(LocalDensity.current) {
                        imageMessageViewModel.getWidth(
                            maxWidth.toPx(),
                            height.toPx()
                        ).toDp()
                    }
                Box(Modifier.height(height).width(width)) {
                    DownloadProgress(it, imageMessageViewModel::cancelThumbnailDownload)
                }
            }
        }
    }
}

@Composable
private fun MessageImage(
    image: ImageBitmap,
    imageMessageViewModel: ImageMessageViewModel,
    onLongPress: (Offset) -> Unit,
) {
    val showSender = imageMessageViewModel.showSender.collectAsState()
    val saveFileDialogOpen = imageMessageViewModel.saveFileDialogOpen.collectAsState().value
    if (saveFileDialogOpen) {
        SaveDialog(
            imageMessageViewModel.fileName,
            imageMessageViewModel.fileMimeType,
            imageMessageViewModel.downloadError.collectAsState().value,
            imageMessageViewModel::downloadFile,
            imageMessageViewModel::closeSaveFileDialog
        )
    }
    Image(
        image,
        "",
        Modifier
            .heightIn(
                50.dp,
                with(LocalDensity.current) { imageMessageViewModel.getMaxHeight().toDp() })
            .clip(
                if (showSender.value) {
                    RoundedCornerShape(0.dp)
                } else {
                    RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp)
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        imageMessageViewModel.openImage()
                    },
                    onLongPress = onLongPress,
                )
            }
            .buttonPointerModifier(),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun MessageImageFallback(
    imageMessageViewModel: ImageMessageViewModel,
    onLongPress: (Offset) -> Unit
) {
    val saveFileDialogOpen = imageMessageViewModel.saveFileDialogOpen.collectAsState().value
    if (saveFileDialogOpen) {
        SaveDialog(
            imageMessageViewModel.fileName,
            imageMessageViewModel.fileMimeType,
            imageMessageViewModel.downloadError.collectAsState().value,
            imageMessageViewModel::downloadFile,
            imageMessageViewModel::closeSaveFileDialog
        )
    }
    val i18n = DI.get<I18nView>()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(start = 30.dp)
    ) {
        Icon(
            Icons.Default.Image,
            i18n.commonImage(),
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            imageMessageViewModel.openImage()
                        },
                        onLongPress = onLongPress,
                    )
                }
                .size(64.dp)
                .buttonPointerModifier()
        )
        FileName(imageMessageViewModel.fileName)
    }
}

@Composable
fun MessageVideo(
    videoMessageViewModel: VideoMessageViewModel,
    onLongPress: (Offset) -> Unit,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    val i18n = DI.get<I18nView>()
    val thumbnail = videoMessageViewModel.thumbnail.collectAsState()

    val saveFileDialogOpen = videoMessageViewModel.saveFileDialogOpen.collectAsState()
    val uploadProgress = videoMessageViewModel.uploadProgress.collectAsState().value
    val downloadSuccessful = videoMessageViewModel.downloadSuccessful.collectAsState()
    val error = videoMessageViewModel.downloadError.collectAsState().value
    if (saveFileDialogOpen.value) SaveFileDialog(
        videoMessageViewModel.fileName,
        videoMessageViewModel.fileMimeType,
        error,
        videoMessageViewModel::downloadFile,
        videoMessageViewModel::closeSaveFileDialog,
    )

    BoxWithConstraints(Modifier.padding(top = 10.dp)) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                thumbnail.value?.let { imageBitmapFromBytes(it) }?.let {
                    Image(
                        it,
                        "",
                        Modifier
                            .heightIn(64.dp, videoMessageViewModel.getHeight(400f).dp)
                            .widthIn(64.dp, 400.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .openVideoOnTouch(videoMessageViewModel, onLongPress)
                            .buttonPointerModifier(),
                        contentScale = ContentScale.FillBounds
                    )
                } ?: run {
                    Icon(
                        Icons.Default.VideoFile,
                        i18n.commonVideo(),
                        Modifier
                            .size(64.dp)
                            .openVideoOnTouch(videoMessageViewModel, onLongPress)
                            .buttonPointerModifier(),
                        tint = Color.DarkGray,
                    )
                }
                FileName(videoMessageViewModel.fileName)
                SmallSpacer()
                if (uploadProgress != null) {
                    Box(
                        Modifier
                            .padding(horizontal = 10.dp),
                    ) {
                        DownloadProgress(
                            uploadProgress,
                            { if (baseTimelineElementHolderViewModel is OutboxElementHolderViewModel) baseTimelineElementHolderViewModel.abortSend() else Unit }
                        )
                    }
                }
            }
            if (downloadSuccessful.value == true) {
                Spacer(Modifier.size(10.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    i18n.messageContentDownloadCompleted(),
                    Modifier.align(Alignment.CenterVertically),
                    Color.DarkGray
                )
            }
        }
    }
}

@Composable
private fun Modifier.openVideoOnTouch(
    videoMessageViewModel: VideoMessageViewModel,
    onLongPress: (Offset) -> Unit
): Modifier {
    return this.then(pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                //Since openVideo only starts the saveDialog currently, restricting it doesn't make sense yet
                //if (uploadProgress != null && uploadProgress.percent >= 1.0f)
                videoMessageViewModel.openVideo()
            },
            onLongPress = onLongPress,
        )
    })
}

@Composable
private fun MessageAudio(
    audioMessageViewModel: AudioMessageViewModel,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val saveFileDialogOpen = audioMessageViewModel.saveFileDialogOpen.collectAsState()
    val downloadProgressElement = audioMessageViewModel.downloadProgress.collectAsState()
    val downloadSuccessful = audioMessageViewModel.downloadSuccessful.collectAsState()
    val uploadProgress = audioMessageViewModel.uploadProgress.collectAsState().value
    val error = audioMessageViewModel.downloadError.collectAsState().value
    if (saveFileDialogOpen.value) SaveFileDialog(
        audioMessageViewModel.fileName,
        audioMessageViewModel.fileMimeType,
        error,
        audioMessageViewModel::downloadFile,
        audioMessageViewModel::closeSaveFileDialog,
    )

    BoxWithConstraints(Modifier.padding(top = 10.dp)) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 30.dp),
            ) {
                Icon(Icons.Default.AudioFile, i18n.commonAudio(),
                    modifier = Modifier
                        .size(64.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    audioMessageViewModel.openAudio()
                                },
                                onLongPress = onLongPress,
                            )
                        }
                        .buttonPointerModifier())
                FileName(audioMessageViewModel.fileName)
                if (uploadProgress != null) {
                    Box {
                        DownloadProgress(uploadProgress,
                            cancel = {
                                if (baseTimelineElementHolderViewModel is OutboxElementHolderViewModel) {
                                    baseTimelineElementHolderViewModel.abortSend()
                                } else {
                                    Unit
                                }
                            })
                    }
                }
            }
            if (downloadSuccessful.value == true) {
                Spacer(Modifier.size(10.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    i18n.messageContentDownloadCompleted(),
                    Modifier.align(Alignment.CenterVertically),
                    Color.DarkGray
                )
            }
        }
        downloadProgressElement.value?.let {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp),
            ) {
                DownloadProgress(
                    it,
                    { audioMessageViewModel.cancelDownload() },
                    Color.DarkGray
                )
            }
        }
    }
}

@Composable
private fun MessageFile(
    fileMessageViewModel: FileMessageViewModel,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val saveFileDialogOpen = fileMessageViewModel.saveFileDialogOpen.collectAsState()
    val downloadProgressElement = fileMessageViewModel.downloadProgress.collectAsState()
    val downloadSuccessful = fileMessageViewModel.downloadSuccessful.collectAsState()
    val uploadProgress = fileMessageViewModel.uploadProgress.collectAsState().value
    val error = fileMessageViewModel.downloadError.collectAsState().value
    if (saveFileDialogOpen.value) SaveFileDialog(
        fileMessageViewModel.fileName,
        fileMessageViewModel.fileMimeType,
        error,
        fileMessageViewModel::downloadFile,
        fileMessageViewModel::closeSaveFileDialog,
    )

    Box(
        Modifier
            .padding(10.dp)
    ) {
        Column(
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { fileMessageViewModel.openFile() },
                        onLongPress = onLongPress,
                    )
                }
                .padding(10.dp)
                .buttonPointerModifier()
        ) {
            Row {
                Box(
                    Modifier
                        .align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    Icon(Icons.Default.Attachment, i18n.commonAttachment(), Modifier.padding(10.dp))
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    buildAnnotatedString {
                        append(fileMessageViewModel.fileName)
                        pushStyle(SpanStyle(Color.Gray))
                        append(fileMessageViewModel.formattedSize)
                    },
                    Modifier.align(Alignment.CenterVertically)
                )
                if (downloadSuccessful.value == true) {
                    Spacer(Modifier.size(10.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        i18n.messageContentDownloadCompleted(),
                        Modifier.align(Alignment.CenterVertically),
                        Color.DarkGray
                    )
                }
            }
            if (uploadProgress != null) {
                Box {
                    DownloadProgress(uploadProgress, cancel = {
                        if (baseTimelineElementHolderViewModel is OutboxElementHolderViewModel) {
                            baseTimelineElementHolderViewModel.abortSend()
                        } else {
                            Unit
                        }
                    })
                }
            }
            downloadProgressElement.value?.let {
                Spacer(Modifier.size(10.dp))
                Box(Modifier.height(40.dp)) {
                    DownloadProgress(
                        it,
                        { fileMessageViewModel.cancelDownload() },
                        Color.DarkGray
                    )
                }
                Spacer(Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun MessageLocation(viewmodel: LocationMessageViewModel, onLongPress: (Offset) -> Unit) {
    if (Platform.current.isDesktop) {
        // on Desktop it makes sense to select text and copy it;
        // on Android, this will consume long tap events, which we use for the context menu
        SelectionContainer {
            MessageLocationContent(viewmodel, onLongPress)
        }
    } else {
        MessageLocationContent(viewmodel, onLongPress)
    }
}

@Composable
fun MessageLocationContent(viewmodel: LocationMessageViewModel, onLongPress: (Offset) -> Unit) {
    val i18n = DI.get<I18nView>()
    val (geoUrl, pos) = viewmodel.geoUri
        .removePrefix("geo:").substringBefore(";").split(",")
        .let { (lat, lon) ->
            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon" to Pair(lat, lon)
        }

    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = AnnotatedString(i18n.locationClickText(pos)),
        onClick = {
            uriHandler.openUri(geoUrl)
        },
        onLongPress = onLongPress,
        style = MaterialTheme.typography.bodySmall
    )
}

internal val urlRegex =
    Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)")
