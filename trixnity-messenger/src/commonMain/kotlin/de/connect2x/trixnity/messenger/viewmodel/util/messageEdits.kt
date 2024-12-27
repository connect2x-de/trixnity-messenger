package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.TimelineEventAggregation
import net.folivo.trixnity.client.room.getTimelineEventReplaceAggregation
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.TimelineEventRelation
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelationType
import net.folivo.trixnity.core.model.events.m.replace
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes


private val log = KotlinLogging.logger {}

@OptIn(ExperimentalCoroutinesApi::class)
fun messageEdits(
    client: MatrixClient,
    eventId: EventId,
    roomId: RoomId,
): Flow<List<TimelineEvent>> =
// The replace-aggregation on Trixnity currently supports a local-only fetch.
// This means there could be future edge cases of history items missing. But for now it should do.
//    client.room.getTimelineEventReplaceAggregation(roomId, eventId).flatMapLatest {
    getTimelineEventReplaceAggregation(client.room, roomId, eventId).flatMapLatest { aggregation ->
        (aggregation.history + eventId).distinct().map { historicEventId ->
            client.room.getTimelineEvent(roomId, historicEventId) {
                fetchSize = 1
                allowReplaceContent = false
                fetchTimeout = 1.minutes
                decryptionTimeout = 1.minutes
            }.filterNotNull()
        }.let { flows ->
            combine(flows) { events ->
                events.toList().sortedBy { it.originTimestamp }.reversed()
            }
        }
    }

private fun messageEditsOld(
    client: MatrixClient,
    eventId: EventId,
    roomId: RoomId,
): Flow<List<TimelineEvent>> = flow {
    log.debug { "fetching edits for event with id: $eventId in room with id: $roomId" }

    // TODO: Rewrite to using nice flows instead.
    // TODO: Maybe return a wrapper to allow flow cancellation?

    emit(client.room.getTimelineEventReplaceAggregation(roomId, eventId).map {
        (it.history + eventId).distinct().mapNotNull { relationId ->
            client.room.getTimelineEvent(roomId, relationId) {
                fetchSize = 1
                allowReplaceContent = false
                fetchTimeout = 1.minutes
                decryptionTimeout = 1.minutes
            }.first()
        }
    }.first())


    /*
    var relations: Map<EventId, Flow<TimelineEventRelation?>>? = null
    while (relations == null) {
        try {
            relations = client.room.getTimelineEventRelations(roomId, eventId, Replace).first()
        } catch (e: Exception) {
            log.error(e) { "failed to gather event relations for id: $eventId" }
        }
        if (relations !is Map<EventId, Flow<TimelineEventRelation?>>) {
            val delay = 500.milliseconds
            log.debug { "retrying fetch of event relations for id $eventId after $delay" }
            delay(delay)
        }
    }

    val relatedEventIds: List<EventId> = listOf(eventId) + relations.map { entry ->
        var relation: TimelineEventRelation? = null
        while (relation == null) {
            relation = entry.value.first()
            if (relation !is TimelineEventRelation) {
                val delay = 500.milliseconds
                log.debug { "retrying fetch of related message for id ${entry.key} after $delay" }
                delay(delay)
            }
        }
        relation.eventId
    }

    val messages = mutableListOf<TimelineEvent>()
    relatedEventIds.forEach { relationId ->
        var message: TimelineEvent? = null
        while (message == null) {
            message = client.room.getTimelineEvent(
                roomId = roomId,
                eventId = relationId,
                config = {
                    fetchSize = 1
                    allowReplaceContent = false
                },
            ).first()
            if (message !is TimelineEvent) {
                val delay = 500.milliseconds
                log.debug { "retrying fetch of related message for id ${relationId} after $delay" }
                delay(delay)
            }
        }
        messages += message
        emit(messages)
    }
    emit(messages)

     */
}
