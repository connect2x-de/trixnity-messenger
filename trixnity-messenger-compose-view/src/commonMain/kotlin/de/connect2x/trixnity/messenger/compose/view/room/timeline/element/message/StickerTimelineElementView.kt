package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file.FileBasedTimelineElement
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file.ImageTimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file.timelineImageAspectRatio
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.util.SupportedMimeTypes
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.StickerTimelineElementViewModel
import io.ktor.http.ContentType
import kotlin.reflect.KClass

interface StickerTimelineElementView : TimelineElementView<StickerTimelineElementViewModel>

class StickerTimelineElementViewImpl : StickerTimelineElementView {
    override val supports: KClass<StickerTimelineElementViewModel> = StickerTimelineElementViewModel::class

    override suspend fun waitFor(element: StickerTimelineElementViewModel) = Unit

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: StickerTimelineElementViewModel,
        index: Int,
    ) {
        StickerMessageBubble(holder, element, index, isPreview = false)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: StickerTimelineElementViewModel,
        index: Int,
    ) {
        StickerMessageBubble(holder, element, index, isPreview = true)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: StickerTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        StickerReply(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: StickerTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        StickerReply(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: StickerTimelineElementViewModel,
    ): ClipEntry? = null

    override fun a11yLabel(element: StickerTimelineElementViewModel, i18n: I18nView): String {
        return "${i18n.commonImage()}, ${element.name}"
    }
}

@Composable
private fun StickerMessageBubble(
    holder: BaseTimelineElementHolderViewModel,
    element: StickerTimelineElementViewModel,
    index: Int,
    isPreview: Boolean,
) {
    val defaultStyle =
        if (holder.isByMe) {
            MaterialTheme.components.messageBubbleOwn
        } else {
            MaterialTheme.components.messageBubbleOther
        }
    val transparentStyle =
        defaultStyle.copy(color = Color.Transparent, tonalElevation = 0.dp, shadowElevation = 0.dp, border = null)

    FileBasedTimelineElement(
        holder = holder,
        element = element,
        displayProgressOverElement = true,
        openSaveDialogOnClick = true,
        caption = {},
        details = {},
        content = { _, _ -> StickerImage(element, maxSize = 300.dp) },
        needsMaxWidth = false,
        isPreview = isPreview,
        isMentioned = element.isMentioned,
        index = index,
        messageBubbleStyle = transparentStyle,
    )
}

@Composable
private fun StickerReply(
    holder: TimelineElementHolderViewModel,
    element: StickerTimelineElementViewModel,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    ReferencedMessagePill(
        holder = holder,
        element = element,
        modifier = modifier,
        interactionSource = interactionSource,
    ) {
        StickerImage(element, maxSize = 100.dp)
    }
}

@Composable
private fun StickerImage(element: StickerTimelineElementViewModel, maxSize: Dp) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ImageTimelineElementView(
            element = element,
            contentDescription = element.name,
            modifier =
                Modifier.padding(3.dp)
                    .sizeIn(maxWidth = maxSize, maxHeight = maxSize)
                    .timelineImageAspectRatio(element)
                    .clip(MaterialTheme.shapes.medium),
        )

        if (shouldWarnAboutStickerMimeType(element.mimeType)) {
            Row(
                modifier = Modifier.widthIn(max = maxSize).padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    i18n.stickerAnimatedOrUnsupportedWarning(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun shouldWarnAboutStickerMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) {
        return true
    }
    val contentType = runCatching { ContentType.parse(mimeType) }.getOrNull() ?: return true
    val isAnimationCapable = ContentType.Image.GIF.match(contentType)
    val isSupported = SupportedMimeTypes.isSupportedImage(contentType)
    return isAnimationCapable || !isSupported
}
