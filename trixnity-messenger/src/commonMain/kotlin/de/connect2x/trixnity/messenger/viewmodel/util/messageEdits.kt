package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.TimelineEventRelation
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelationType.Replace
import kotlin.time.Duration.Companion.milliseconds


private val log = KotlinLogging.logger {}

fun messageEdits(
    client: MatrixClient,
    eventId: EventId,
    roomId: RoomId,
): Flow<List<TimelineEvent>> = flow {
    log.debug { "fetching edits for event with id: $eventId in room with id: $roomId" }

    // TODO: Rewrite to using nice flows instead.
    // TODO: Maybe return a wrapper to allow flow cancellation?

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
}
