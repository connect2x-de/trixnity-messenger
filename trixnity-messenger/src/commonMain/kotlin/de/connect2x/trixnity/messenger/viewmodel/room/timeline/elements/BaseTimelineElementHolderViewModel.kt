package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlinx.coroutines.flow.StateFlow

sealed interface BaseTimelineElementHolderViewModel {
    /**
     * The unique key of the element within the timeline.
     */
    val key: String

    /**
     * The actual content of the element.
     */
    val element: StateFlow<TimelineElementViewModel<*>?>

    /**
     * Is going to have an repliedElement.
     */
    val isReply: StateFlow<Boolean?>

    /**
     * Can be an element that is replied to or a thread.
     */
    val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?>

    /**
     * Indicates, that the message is the first in a sequence (group) of messages from the same user.
     * This can be used to e.g. render a chat bubble edge.
     */
    val isFirstInUserSequence: StateFlow<Boolean?>

    /**
     * The time, when an event has been created.
     */
    val formattedTime: String

    /**
     * The date, when an event has been created.
     */
    val formattedDate: String

    /**
     * This event is from us.
     */
    val isByMe: Boolean

    /**
     * The sender of this event.
     */
    val sender: StateFlow<UserInfoElement?>

    /**
     * This element should show the sender.
     */
    val showSender: StateFlow<Boolean?>

    /**
     * This element show render a big gap (e.g. due to a time gap or sender change)
     */
    val showBigGapBefore: StateFlow<Boolean?>
}
