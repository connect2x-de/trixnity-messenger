package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.StickerEventContent
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.ImageTimelineElementViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.isUserMentioned
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import kotlin.reflect.KClass

interface StickerTimelineElementViewModelFactory : TimelineElementViewModelFactory<StickerEventContent> {
    override val supports: KClass<StickerEventContent>
        get() = StickerEventContent::class

    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: StickerEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): StickerTimelineElementViewModel = StickerTimelineElementViewModelImpl(viewModelContext, content)

    companion object : StickerTimelineElementViewModelFactory
}

interface StickerTimelineElementViewModel :
    TimelineElementViewModel.Message<StickerEventContent>, FileBasedTimelineElementViewModel.Image

class StickerTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: StickerEventContent,
) :
    StickerTimelineElementViewModel,
    MatrixClientViewModelContext by viewModelContext,
    FileBasedTimelineElementViewModel.Image by ImageTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = content.url,
        encryptedFile = null,
        info = content.info,
        fileName = stickerFileName(content.body, content.info.mimeType),
    ) {
    override val isMentioned: Boolean = content.isUserMentioned(userId)
}

internal fun stickerFileName(description: String, mimeType: String?): String {
    val extensions =
        mimeType
            ?.let { runCatching { ContentType.parse(it).withoutParameters().fileExtensions() }.getOrNull() }
            .orEmpty()
    val name =
        description.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F\\x7F]"), "_").trim().trimEnd('.').ifBlank { "sticker" }
    if (extensions.any { name.endsWith(".$it", ignoreCase = true) }) return name
    return "$name.${extensions.firstOrNull() ?: "bin"}"
}
