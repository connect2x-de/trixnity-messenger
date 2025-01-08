package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlinx.coroutines.flow.StateFlow


sealed interface BaseTimelineElementHolderViewModel {
    /**
     * Unique key of the element within the timeline.
     */
    val key: String

    /**
     * Event content of the element.
     */
    val element: StateFlow<TimelineElementViewModel<*>?>

    /**
     * Whether this has a [repliedElement].
     */
    val isReply: StateFlow<Boolean?>

    /**
     * Optional element (event or thread) which was replied to.
     */
    val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?>

    /**
     * Indicates that the message is the first in a sequence or consecutive group of messages from the same user.
     * This can be used to e.g. render a chat bubble edge.
     */
    val isFirstInUserSequence: StateFlow<Boolean?>

    /**
     * The formatted and localized time of the day at which an event has been created.
     */
    val formattedTime: String

    /**
     * The formatted and localized calendar date at which an event has been created.
     */
    val formattedDate: String

    /**
     * Tells whether the event is from us.
     */
    val isByMe: Boolean

    /**
     * The sender of this event.
     */
    val sender: StateFlow<UserInfoElement?>

    /**
     * Whether this element should show the sender.
     */
    val showSender: StateFlow<Boolean?>

    /**
     * Whether this element should render a big gap (e.g. due to a time gap or sender change).
     */
    val showBigGapBefore: StateFlow<Boolean?>
}
