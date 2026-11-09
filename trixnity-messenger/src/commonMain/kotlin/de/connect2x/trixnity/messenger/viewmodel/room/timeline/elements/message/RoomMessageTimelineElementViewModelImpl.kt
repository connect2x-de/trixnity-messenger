package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.bodyWithoutFallback
import de.connect2x.trixnity.core.model.events.m.room.formattedBodyWithoutFallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.MatrixReferences
import de.connect2x.trixnity.messenger.util.html.AutoLinkifyVisitor
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.util.isMatrixMediaUri
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.MentionHelper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.isUserMentioned
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

abstract class RoomMessageTimelineElementViewModelImpl<C : RoomMessageEventContent>(
    private val viewModelContext: MatrixClientViewModelContext,
    content: C,
    roomId: RoomId,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext { // Do not inherit from RoomMessageTimelineElementViewModel to
    // simplify pattern matching of the sealed interface, etc.
    private val mentionHelper =
        MentionHelper(
            coroutineScope,
            matrixClient,
            roomId,
            get<Initials>(),
            get<RoomName>(),
            get<MatrixMessengerConfiguration>().downloadLimits.thumbnail,
        )

    val body: String = content.bodyWithoutFallback
    val formattedBody: String? = content.formattedBodyWithoutFallback
    val formattedBodyContent: HtmlNode.HtmlElement =
        content.formattedBodyWithoutFallback?.let(HtmlVisitor::process)?.let(AutoLinkifyVisitor::process)
            ?: HtmlNode.HtmlElement("#root", emptyMap(), listOf(HtmlNode.TextContent(content.body)))
                .let(AutoLinkifyVisitor::process)

    val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> by lazy {
        MatrixReferences.findInText(body).mapValues { (_, mention) -> mentionHelper.processMention(mention) }
    }

    val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
        mentionHelper.processMentions(formattedBodyContent).stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    val isMentioned: Boolean = content.isUserMentioned(userId)

    suspend fun loadInlineImage(uri: String, width: Int, height: Int): ByteArray? {
        if (!isMatrixMediaUri(uri)) return null
        val maxSize = get<MatrixMessengerConfiguration>().downloadLimits.thumbnail
        return get<Thumbnails>()
            .loadThumbnail(
                coroutineScope = coroutineScope,
                matrixClient = matrixClient,
                thumbnailFile = null,
                thumbnailUrl = null,
                thumbnailInfo = null,
                file = null,
                fileUrl = uri,
                fileInfo = null,
                sizeInBytes = maxSize,
                thumbnailProgressFlow = MutableStateFlow(null),
                maxMediaSizeInMemory = maxSize,
                maxDownloadSize = maxSize,
                width = width.toLong(),
                height = height.toLong(),
            )
    }

    fun openMention(mention: TimelineElementMention) {
        onOpenMention(userId, mention)
    }
}
