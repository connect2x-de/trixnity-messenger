package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.MSC2448
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.FileName
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.modifier.customClickable
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.util.shortenFileName
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.compose.view.util.BlurHashDecoder
import de.connect2x.trixnity.messenger.compose.view.util.animateImage
import de.connect2x.trixnity.messenger.compose.view.util.ifNotNull
import de.connect2x.trixnity.messenger.compose.view.util.rememberComputation
import de.connect2x.trixnity.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.FileBased.Image
import kotlin.reflect.KClass
import org.jetbrains.compose.resources.ExperimentalResourceApi

interface ImageRoomMessageTimelineElementView : TimelineElementView<Image>

class ImageRoomMessageTimelineElementViewImpl : ImageRoomMessageTimelineElementView {
    override val supports: KClass<Image> = Image::class

    override suspend fun waitFor(element: Image) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: Image, index: Int) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            overlay = { ImageMessageElementOverlay(element) },
            displayProgressOverElement = true,
            index = index,
        ) { showActionMenu, onSave ->
            MessageImage(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createAsPreview(holder: TimelineElementHolderViewModel, element: Image, index: Int) {
        FileBasedRoomMessageTimelineElement(
            holder,
            element,
            isPreview = true,
            displayProgressOverElement = true,
            index = index,
            overlay = { ImageMessageElementOverlay(element) },
        ) { showActionMenu, onSave ->
            MessageImage(element, showActionMenu, onSave)
        }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Image,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ImageReplyElement(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Image,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ImageReplyElement(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(holder: BaseTimelineElementHolderViewModel, element: Image): ClipEntry? =
        element.toClipEntry()

    override fun a11yLabel(element: Image, i18n: I18nView): String {
        return "${i18n.commonImage()}, ${element.name} ${element.size}"
    }
}

@Composable
internal fun ImageMessageElementOverlay(element: Image) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${shortenFileName(element)}${element.size.ifNotNull { " $it" }}",
            Modifier.basicMarquee(),
            color = MaterialTheme.messengerColors.metaDataPreview,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ColumnScope.MessageImage(element: Image, showActionMenu: () -> Unit, onSave: () -> Unit) {
    val i18n = DI.get<I18nView>()

    val thumbnail = rememberImagePainter(element)
    val fallback = rememberFallbackPainter(element)
    val imagePainter = animateImage(thumbnail, fallback)
    val thumbnailLoading = element.thumbnailLoading.collectAsState().value

    val aspectRatioModifier =
        element.width?.let { width ->
            element.height?.let { height -> Modifier.aspectRatio(width.toFloat() / height.toFloat()) }
        } ?: Modifier

    Box(
        modifier =
            Modifier.padding(3.dp)
                .requiredHeightIn(50.dp, with(LocalDensity.current) { 300.dp })
                .then(aspectRatioModifier)
                .clip(RoundedCornerShape(8.dp))
                .customClickable(
                    onLongClickLabel = i18n.commonContextMenu(),
                    onLongClick = { showActionMenu() },
                    onClickLabel = i18n.downloadMessage(),
                    onClick = { onSave() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePainter != null) {
            Image(
                imagePainter,
                null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            )
            if (thumbnailLoading) {
                ThemedSurface(style = MaterialTheme.components.background) {
                    ThemedProgressIndicator(
                        style = MaterialTheme.components.circularProgressIndicator,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        } else {
            Column {
                Column(
                    Modifier.width(IntrinsicSize.Max).padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp))
                    FileName(element.name)
                }

                if (thumbnailLoading) {
                    Spacer(Modifier.height(8.dp))
                    ThemedProgressIndicator(style = MaterialTheme.components.linearProgressIndicator)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalResourceApi::class)
internal fun ImageReplyElement(
    holder: TimelineElementHolderViewModel,
    element: Image,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val i18n = DI.get<I18nView>()
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            val thumbnail = rememberImagePainter(element)
            val fallback = rememberFallbackPainter(element)
            val imagePainter = animateImage(thumbnail, fallback)
            val thumbnailLoading = element.thumbnailLoading.collectAsState().value

            Column {
                if (imagePainter != null) {
                    Box {
                        Image(
                            imagePainter,
                            null,
                            Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)).align(Alignment.Center),
                            contentScale = ContentScale.Fit,
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
                            modifier = Modifier.size(MaterialTheme.typography.bodySmall.dp),
                        )
                        FileName(element.name)
                    }
                }
                if (element.hasCaption) {
                    TextReply(element, maxLines = 2)
                }
            }
        },
    )
}

@OptIn(MSC2448::class)
@Composable
private fun rememberFallbackPainter(element: Image): Painter? {
    val thumbnailSize =
        element.thumbnailWidth?.let { width -> element.thumbnailHeight?.let { height -> IntSize(width, height) } }
            ?: element.width?.let { width -> element.height?.let { height -> IntSize(width, height) } }

    return rememberComputation(element.blurhash, thumbnailSize) {
        BlurHashDecoder.decode(element.blurhash, thumbnailSize)?.let { BitmapPainter(it) }
    }
}

@OptIn(MSC2448::class)
@Composable
private fun rememberImagePainter(element: Image): Painter? {
    val thumbnail = element.thumbnail.collectAsState().value
    return rememberComputation(thumbnail) { thumbnail?.toImageBitmap()?.let { BitmapPainter(it) } }
}
