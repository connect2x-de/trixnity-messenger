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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
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
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.AudioMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EmoteMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedWaitTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FallbackMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.LocationMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.MessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.NoticeMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TextMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.VideoMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.FileMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.ImageRoomMessageTimelineElementViewModelImpl
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

@Composable
fun MessageContent(
    messageTimelineElementViewModel: MessageTimelineElementViewModel,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    when (messageTimelineElementViewModel) {
        is TextMessageViewModel -> MessageText(messageTimelineElementViewModel, onLongPress)
        is EmoteMessageViewModel -> MessageText(messageTimelineElementViewModel, onLongPress)
        is NoticeMessageViewModel -> MessageText(messageTimelineElementViewModel, onLongPress, isNotice = true)
        is RedactedTimelineElementViewModel -> MessageRedacted(messageTimelineElementViewModel)
        is ImageRoomMessageTimelineElementViewModelImpl -> MessageImage(
            messageTimelineElementViewModel,
            baseTimelineElementHolderViewModel,
            onLongPress
        )

        is VideoMessageViewModel -> MessageVideo(
            messageTimelineElementViewModel,
            onLongPress,
            baseTimelineElementHolderViewModel
        )

        is AudioMessageViewModel -> MessageAudio(
            messageTimelineElementViewModel,
            baseTimelineElementHolderViewModel,
            onLongPress
        )

        is FileMessageViewModel -> MessageFile(
            messageTimelineElementViewModel,
            baseTimelineElementHolderViewModel,
            onLongPress
        )

        is EncryptedWaitTimelineElementViewModel -> EncryptedMessage(messageTimelineElementViewModel)
        is FallbackMessageViewModel -> MessageText(messageTimelineElementViewModel, onLongPress)
        is LocationMessageViewModel -> MessageLocation(messageTimelineElementViewModel, onLongPress)
    }
}


@Composable
private fun MessageRedacted(redactedTimelineElementViewModel: RedactedTimelineElementViewModel) {
    val i18n = DI.get<I18nView>()
    val formattedMessage = redactedTimelineElementViewModel.message.collectAsState().value
    Row(Modifier.padding(10.dp)) {
        Icon(
            Icons.Outlined.Delete, i18n.commonDeleted(),
            Modifier.align(Alignment.CenterVertically)
                .size(MaterialTheme.typography.bodySmall.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "$formattedMessage${redactedTimelineElementViewModel.redactedAtDateTime?.let { " ($it)" } ?: ""}",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun EncryptedMessage(encryptedWaitTimelineElementViewModel: EncryptedWaitTimelineElementViewModel) {
    val i18n = DI.get<I18nView>()
    val waitForDecryption = encryptedWaitTimelineElementViewModel.waitForDecryption.collectAsState().value
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
    roomMessageImageTimelineElementViewModelImpl: ImageRoomMessageTimelineElementViewModelImpl,
    baseTimelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    onLongPress: (Offset) -> Unit,
) {
    if (baseTimelineElementHolderViewModel is OutboxElementHolderViewModel) {
        OutboxMessageImage(
            roomMessageImageTimelineElementViewModelImpl,
            onLongPress,
            baseTimelineElementHolderViewModel
        )
    } else {
        InboxMessageImage(roomMessageImageTimelineElementViewModelImpl, onLongPress)
    }
}

@Composable
private fun OutboxMessageImage(
    roomMessageImageTimelineElementViewModelImpl: ImageRoomMessageTimelineElementViewModelImpl,
    onLongPress: (Offset) -> Unit,
    outboxElementHolderViewModel: OutboxElementHolderViewModel
) {
    val uploadProgress = roomMessageImageTimelineElementViewModelImpl.uploadProgress.collectAsState(null)
    val image = roomMessageImageTimelineElementViewModelImpl.thumbnail.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 10.dp)
    ) {
        image.value?.let { imageBitmapFromBytes(it) }?.let {
            MessageImage(it, roomMessageImageTimelineElementViewModelImpl, onLongPress)
        } ?: MessageImageFallback(roomMessageImageTimelineElementViewModelImpl, onLongPress)
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
    roomMessageImageTimelineElementViewModelImpl: ImageRoomMessageTimelineElementViewModelImpl,
    onLongPress: (Offset) -> Unit
) {
    val i18n = DI.get<I18nView>()
    val uploadProgress = roomMessageImageTimelineElementViewModelImpl.uploadProgress.collectAsState(null)
    val image = roomMessageImageTimelineElementViewModelImpl.thumbnail.collectAsState()

    BoxWithConstraints(Modifier.padding(top = 10.dp)) {
        image.value?.let { imageBitmapFromBytes(it) }?.let {
            MessageImage(it, roomMessageImageTimelineElementViewModelImpl, onLongPress)
        } ?: MessageImageFallback(roomMessageImageTimelineElementViewModelImpl, onLongPress)
        uploadProgress.value?.let {
            if (image.value == null) {
                val height =
                    with(LocalDensity.current) {
                        roomMessageImageTimelineElementViewModelImpl.getHeight(maxWidth.toPx()).toDp()
                    }
                val width =
                    with(LocalDensity.current) {
                        roomMessageImageTimelineElementViewModelImpl.getWidth(
                            maxWidth.toPx(),
                            height.toPx()
                        ).toDp()
                    }
                Box(Modifier.height(height).width(width)) {
                    DownloadProgress(it, roomMessageImageTimelineElementViewModelImpl::cancelThumbnailDownload)
                }
            }
        }
    }
}

@Composable
private fun MessageImage(
    image: ImageBitmap,
    roomMessageImageTimelineElementViewModelImpl: ImageRoomMessageTimelineElementViewModelImpl,
    onLongPress: (Offset) -> Unit,
) {
    val showSender = roomMessageImageTimelineElementViewModelImpl.showSender.collectAsState()
    val saveFileDialogOpen = roomMessageImageTimelineElementViewModelImpl.saveFileDialogOpen.collectAsState().value
    if (saveFileDialogOpen) {
        SaveFileDialog(
            roomMessageImageTimelineElementViewModelImpl.fileName,
            roomMessageImageTimelineElementViewModelImpl.fileMimeType,
            roomMessageImageTimelineElementViewModelImpl.downloadError.collectAsState().value,
            roomMessageImageTimelineElementViewModelImpl::downloadFile,
            roomMessageImageTimelineElementViewModelImpl::closeSaveFileDialog
        )
    }
    Image(
        image,
        "",
        Modifier
            .heightIn(
                50.dp,
                with(LocalDensity.current) { roomMessageImageTimelineElementViewModelImpl.getMaxHeight().toDp() })
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
                        roomMessageImageTimelineElementViewModelImpl.openImage()
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
    roomMessageImageTimelineElementViewModelImpl: ImageRoomMessageTimelineElementViewModelImpl,
    onLongPress: (Offset) -> Unit
) {
    val saveFileDialogOpen = roomMessageImageTimelineElementViewModelImpl.saveFileDialogOpen.collectAsState().value
    if (saveFileDialogOpen) {
        SaveFileDialog(
            roomMessageImageTimelineElementViewModelImpl.fileName,
            roomMessageImageTimelineElementViewModelImpl.fileMimeType,
            roomMessageImageTimelineElementViewModelImpl.downloadError.collectAsState().value,
            roomMessageImageTimelineElementViewModelImpl::downloadFile,
            roomMessageImageTimelineElementViewModelImpl::closeSaveFileDialog
        )
    }
    val i18n = DI.get<I18nView>()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(start = 30.dp)
    ) {
        Icon(
            MaterialTheme.messengerIcons.typeImage,
            i18n.commonImage(),
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            roomMessageImageTimelineElementViewModelImpl.openImage()
                        },
                        onLongPress = onLongPress,
                    )
                }
                .size(64.dp)
                .buttonPointerModifier()
        )
        FileName(roomMessageImageTimelineElementViewModelImpl.fileName)
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
                        MaterialTheme.messengerIcons.typeVideo,
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
                Icon(
                    MaterialTheme.messengerIcons.typeAudio, i18n.commonAudio(),
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
                        DownloadProgress(
                            uploadProgress,
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
