package de.connect2x.trixnity.messenger.export

import kotlinx.datetime.Instant
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.originTimestamp

fun interface ExportRoomRangeStartCondition {
    /**
     * Returns true, when the start event has been found.
     */
    operator fun invoke(timelineEvent: TimelineEvent): Boolean

    companion object {
        /**
         * Use the first visible event of the room.
         */
        fun first(): ExportRoomRangeStartCondition = ExportRoomRangeStartCondition { false }

        /**
         * Use a maximal number of events starting from the end of the timeline.
         */
        fun max(count: Long): ExportRoomRangeStartCondition {
            var counter = 0
            return ExportRoomRangeStartCondition {
                counter++
                counter >= count
            }
        }

        /**
         * Use all events after [instant].
         */
        fun from(instant: Instant): ExportRoomRangeStartCondition = ExportRoomRangeStartCondition {
            Instant.fromEpochMilliseconds(it.originTimestamp) < instant
        }
    }
}

fun interface ExportRoomRangeEndCondition {
    /**
     * Returns true, when the end event has been found.
     */
    operator fun invoke(timelineEvent: TimelineEvent): Boolean

    companion object {
        /**
         * Use the last known event of the room.
         */
        fun last(): ExportRoomRangeEndCondition = ExportRoomRangeEndCondition { false }

        /**
         * Use a maximal number of events.
         */
        fun max(count: Long): ExportRoomRangeEndCondition {
            var counter = 0
            return ExportRoomRangeEndCondition {
                counter++
                counter >= count
            }
        }

        /**
         * Use all events before [instant].
         */
        fun until(instant: Instant): ExportRoomRangeEndCondition = ExportRoomRangeEndCondition {
            Instant.fromEpochMilliseconds(it.originTimestamp) > instant
        }
    }
}
