package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.EventId

interface ITimelineElementViewModel {
    val eventId: EventId?
    val viewModel: StateFlow<BaseTimelineElementViewModel?>
    val shouldShowUnreadMarkerFlow: StateFlow<Boolean>
    val showLoadingIndicatorBefore: StateFlow<Boolean>
    val showLoadingIndicatorAfter: StateFlow<Boolean>

    val canBeEdited: StateFlow<Boolean>
    val canBeRedacted: StateFlow<Boolean>
    val redactionInProgress: StateFlow<Boolean>
    val redactionError: StateFlow<String?>
    val canBeRepliedTo: StateFlow<Boolean>
    val highlight: StateFlow<Boolean>
    fun edit()
    fun endEdit()
    fun redact()
    fun replyTo()
    fun endReplyTo()
}