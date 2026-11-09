package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberEventContent
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlin.reflect.KClass

@OptIn(MSC4143::class)
interface RtcMemberMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<RtcMemberEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RtcMemberEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementViewModel<RtcMemberEventContent>? {
        return RtcMemberMessageTimelineElementViewModelImpl(viewModelContext, content)
    }

    override val supports: KClass<RtcMemberEventContent>
        get() = RtcMemberEventContent::class

    companion object : RtcMemberMessageTimelineElementViewModelFactory
}

@OptIn(MSC4143::class)
interface RtcMemberMessageTimelineElementViewModel : TimelineElementViewModel.Message<RtcMemberEventContent> {
    val message: String
}

@OptIn(MSC4143::class)
class RtcMemberMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: RtcMemberEventContent,
) : RtcMemberMessageTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val isMentioned: Boolean = false

    override val message: String =
        when (content) {
            is RtcMemberEventContent.Join -> i18n.rtcMemberJoinedSession(content.application.type)
            is RtcMemberEventContent.Leave -> i18n.rtcMemberLeftSession(content.slotId.value.substringBefore('#'))
        }
}
