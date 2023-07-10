import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.kodein.mock.ArgConstraint
import org.kodein.mock.ArgConstraintsBuilder

fun ArgConstraintsBuilder.isTimelineEvent(
    thisTimelineEvent: TimelineEvent,
    capture: MutableList<TimelineEvent>? = null
): TimelineEvent =
    isValid(ArgConstraint(capture, { "isTimelineEvent" }) {
        if (it.eventId == thisTimelineEvent.eventId) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected timelineEvent with id ${thisTimelineEvent.eventId}, but got $it." }
    })

fun ArgConstraintsBuilder.isTimelineEvent(
    thisTimelineEventId: EventId,
    capture: MutableList<TimelineEvent>? = null
): TimelineEvent =
    isValid(ArgConstraint(capture, { "isTimelineEvent" }) {
        if (it.eventId == thisTimelineEventId) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected timelineEvent with id ${thisTimelineEventId}, but got ${it.event}." }
    })

inline fun <reified T> ArgConstraintsBuilder.isNot(
    other: T,
    capture: MutableList<T>? = null
): T =
    isValid(ArgConstraint(capture, { "isNot" }) {
        if (it != other) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected not to be ${other}, but is the same: ${it}." }
    })

inline fun <reified T> ArgConstraintsBuilder.isNot(
    others: List<T>,
    capture: MutableList<T>? = null
): T =
    isValid(ArgConstraint(capture, { "isNot" }) {
        if (others.contains(it).not()) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected not to be like ${others}, but is the same: ${it}." }
    })

inline fun <reified T> ArgConstraintsBuilder.contentEquals(
    other: Array<T>,
    capture: MutableList<Array<T>>? = null
): Array<T> =
    isValid(ArgConstraint(capture, { "contentEquals" }) {
        if (other contentEquals it) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected arrays to be equal: ${other}, but found: ${it}." }
    })

fun ArgConstraintsBuilder.contentEquals(
    other: ByteArray,
    capture: MutableList<ByteArray>? = null
): ByteArray =
    isValid(ArgConstraint(capture, { "contentEquals" }) {
        if (other contentEquals it) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected arrays to be equal: ${other}, but found: ${it}." }
    })

fun ArgConstraintsBuilder.isRoomOf(roomId: RoomId, capture: MutableList<Room>? = null): Room =
    isValid(ArgConstraint(capture, { "isRoomOf($roomId)" }) {
        if (it.roomId == roomId) ArgConstraint.Result.Success
        else ArgConstraint.Result.Failure { "Expected a room with a roomId $roomId, got ${it.roomId}" }
    })