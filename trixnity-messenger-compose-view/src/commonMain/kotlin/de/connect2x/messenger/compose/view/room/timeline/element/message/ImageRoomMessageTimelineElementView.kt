package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.FileName
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.messenger.compose.view.room.timeline.element.util.shortenFileName
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.Image
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.reflect.KClass


class ImageRoomMessageTimelineElementView : TimelineElementView<Image> {
    override val supports: KClass<Image> =
        Image::class

    override suspend fun waitFor(element: Image) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Image,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            overlay = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${shortenFileName(element)} ${element.size}",
                        color = MaterialTheme.messengerColors.metaDataPreview,
                        maxLines = 1,
                    )
                }
            },
            displayProgressOverElement = true
        ) { showActionMenu, onSave ->
            MessageImage(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Image,
    ) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            isPreview = true,
            displayProgressOverElement = true,
            overlay = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${shortenFileName(element)} ${element.size}",
                        color = MaterialTheme.messengerColors.metaDataPreview,
                        maxLines = 1,
                    )
                }
            },
        ) { showActionMenu, onSave ->
            MessageImage(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(holder: TimelineElementHolderViewModel, element: Image) {
        ImageReplyElement(holder, element)
    }

    @Composable
    override fun createReplyInSendMessage(holder: TimelineElementHolderViewModel, element: Image) {
        ImageReplyElement(holder, element)
    }
}

@Composable
internal fun ImageMessageElementOverlay(element: Image) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${shortenFileName(element)} ${element.size}",
            color = MaterialTheme.messengerColors.metaDataPreview,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ColumnScope.MessageImage(
    element: Image,
    showActionMenu: () -> Unit,
    onSave: () -> Unit
) {
    val image = element.thumbnail.collectAsState().value
    val thumbnailWidth = element.thumbnailWidth ?: element.width
    val thumbnailHeight = element.thumbnailHeight ?: element.height
    val thumbnailLoading = element.thumbnailLoading.collectAsState().value
    val bitmap = remember(image) {
        image?.toImageBitmap()
            ?: if (thumbnailWidth != null && thumbnailHeight != null && thumbnailLoading) {
                ImageBitmap(thumbnailWidth, thumbnailHeight)
            } else null
    }
    bitmap?.let {
        MessageImageImpl(it, showActionMenu, onSave, thumbnailLoading)
    } ?: MessageImageFallback(element, showActionMenu, onSave)
}

@Composable
internal fun ColumnScope.MessageImageImpl(
    image: ImageBitmap,
    showActionMenu: () -> Unit,
    onSave: () -> Unit,
    thumbnailLoading: Boolean
) {
    Box {
        Image(
            image,
            "",
            Modifier
                .align(Alignment.Center)
                .padding(3.dp)
                .fillMaxWidth()
                .heightIn(
                    50.dp,
                    with(LocalDensity.current) { 300.dp })
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onSave()
                        },
                        onLongPress = { showActionMenu() },
                    )
                }
                .buttonPointerModifier(),
            contentScale = ContentScale.Fit,
        )
        if (thumbnailLoading) {
            LoadingSpinner(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
internal fun ColumnScope.MessageImageFallback(
    element: Image,
    showActionMenu: () -> Unit,
    onSave: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            MaterialTheme.messengerIcons.typeImage,
            i18n.commonImage(),
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onSave()
                        },
                        onLongPress = { showActionMenu() },
                    )
                }
                .size(64.dp)
                .buttonPointerModifier()
        )
        FileName(element.name)
    }
}

@Composable
@OptIn(ExperimentalResourceApi::class)
internal fun ImageReplyElement(holder: TimelineElementHolderViewModel, element: Image) {
    val i18n = DI.get<I18nView>()
    ReferencedMessagePill(
        holder = holder,
        content = {
            val image = element.thumbnail.collectAsState().value
            val thumbnailWidth = element.thumbnailWidth ?: element.width
            val thumbnailHeight = element.thumbnailHeight ?: element.height
            val thumbnailLoading = element.thumbnailLoading.collectAsState().value
            val bitmap = remember(image) {
                image?.toImageBitmap()
                    ?: if (thumbnailWidth != null && thumbnailHeight != null && thumbnailLoading) {
                        ImageBitmap(thumbnailWidth, thumbnailHeight)
                    } else null
            }

            if (bitmap != null) {
                Box {
                    Image(
                        bitmap,
                        "",
                        Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)).align(Alignment.Center),
                        contentScale = ContentScale.Fit
                    )
                    if (thumbnailLoading) {
                        LoadingSpinner(Modifier.align(Alignment.Center))
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                    Icon(
                        MaterialTheme.messengerIcons.typeImage,
                        i18n.commonImage(),
                        modifier = Modifier.size(MaterialTheme.typography.bodySmall.dp)
                    )
                    FileName(element.name)
                }
            }
        }
    )
}
