package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationCancelTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationDoneTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.AvatarStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CanonicalAliasStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CreateStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.EncryptionStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.HistoryVisibilityStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.MemberStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.NameStateTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.TopicStateTimelineElementViewModel
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.RoomEventContent
import kotlin.reflect.KClass

interface TimelineElementViewModelFactory<C : RoomEventContent> {
    val supports: KClass<C>
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: C,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementViewModel<C>?
}
