package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import kotlin.reflect.KClass

interface PowerLevelsTimelineElementViewModelFactory : TimelineElementViewModelFactory<PowerLevelsEventContent> {
    companion object : PowerLevelsTimelineElementViewModelFactory

    override val supports: KClass<PowerLevelsEventContent> get() = PowerLevelsEventContent::class

    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: PowerLevelsEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback
    ): PowerLevelsTimelineElementViewModel? = when (eventIdOrTransactionId) {
        is EventIdOrTransactionId.TransactionId -> null
        is EventIdOrTransactionId.EventId -> PowerLevelsTimelineElementViewModelFactoryImpl(
            viewModelContext = viewModelContext,
            content = content,
            roomId = roomId,
            eventId = eventIdOrTransactionId.eventId,
        )
    }
}

interface PowerLevelsTimelineElementViewModel : State<PowerLevelsEventContent> {
    val changeMessage: StateFlow<String?>
}

// https://spec.matrix.org/v1.10/client-server-api/#mroompower_levels

class PowerLevelsTimelineElementViewModelFactoryImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: PowerLevelsEventContent,
    roomId: RoomId,
    eventId: EventId,
) : PowerLevelsTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val changeMessage = flow {
        val timelineEventSnapshot = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
        val event = timelineEventSnapshot.event as? StateEvent ?: return@flow
        val previousContent = event.unsigned?.previousContent as? PowerLevelsEventContent

        val changes: MutableList<String> = mutableListOf()

        if (previousContent?.ban != content.ban) changes.add(i18n.powerLevelUpdateBan(content.ban))
        if (previousContent?.invite != content.invite) changes.add(i18n.powerLevelUpdateInvite(content.invite))
        if (previousContent?.kick != content.kick) changes.add(i18n.powerLevelUpdateKick(content.kick))
        if (previousContent?.redact != content.redact) changes.add(i18n.powerLevelUpdateRedact(content.redact))
        if (previousContent?.eventsDefault != content.eventsDefault)
            changes.add(i18n.powerLevelUpdateEventsDefault(content.eventsDefault))
        if (previousContent?.stateDefault != content.stateDefault) changes.add(i18n.powerLevelUpdateStateDefault(content.stateDefault))
        if (previousContent?.usersDefault != content.usersDefault) changes.add(i18n.powerLevelUpdateUsersDefault(content.usersDefault))

        val userDiff = findMapDifference(previousContent?.users.orEmpty(), content.users)
        userDiff.newEntries.forEach { (userId, newPowerLevel) ->
            val user = matrixClient.user.getById(roomId, userId).first()?.name ?: userId.full
            changes.add(i18n.eventPowerLevelChange(user, newPowerLevel))
        }
        userDiff.changedEntries.forEach { (userId, levels) ->
            val (oldPowerLevel, newPowerLevel) = levels
            val user = matrixClient.user.getById(roomId, userId).first()?.name ?: userId.full
            changes.add(i18n.eventPowerLevelChange(user, newPowerLevel))
        }
        userDiff.removedEntries.forEach { (userId, oldLevel) ->
            val user = matrixClient.user.getById(roomId, userId).first()?.name ?: userId.full
            changes.add(i18n.eventPowerLevelChange(user, content.usersDefault))
        }

        val eventsDiff = findMapDifference(previousContent?.events.orEmpty(), content.events)
        eventsDiff.newEntries.forEach { (eventType, newPowerLevel) ->
            changes.add(i18n.powerLevelUpdateEvent(eventType.name, newPowerLevel))
        }
        eventsDiff.changedEntries.forEach { (eventType, levels) ->
            val (oldPowerLevel, newPowerLevel) = levels
            changes.add(i18n.powerLevelUpdateEvent(eventType.name, newPowerLevel))

        }
        eventsDiff.removedEntries.forEach { (eventType, oldLevel) ->
            changes.add(i18n.powerLevelUpdateEvent(eventType.name, content.eventsDefault))
        }

        emit(
            when (val len = changes.size) {
                0 -> i18n.powerLevelUpdateNoChanges()
                1 -> changes.first()
                2 -> i18n.commonAnd(changes[0], changes[1])
                else -> i18n.powerLevelUpdateNChanges(len)
            }
        )
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
}

private data class MapDifference<T, V : Any>(
    val newEntries: Map<T, V>,
    val removedEntries: Map<T, V>,
    val changedEntries: Map<T, Pair<V, V>> // Key -> (Old Value, New Value)
)

private fun <T, V : Any> findMapDifference(oldMap: Map<T, V>, newMap: Map<T, V>): MapDifference<T, V> {
    val allKeys = (oldMap.keys + newMap.keys).associateWith { key -> oldMap[key] to newMap[key] }

    val removedEntries = allKeys.mapValueNotNull { (old, new) ->
        if (new == null) old
        else null
    }
    val newEntries = allKeys.mapValueNotNull { (old, new) ->
        if (old == null) new
        else null
    }

    val changedEntries = allKeys.mapValueNotNull { (old, new) ->
        if (old != null && new != null && old != new) old to new
        else null
    }

    return MapDifference(newEntries, removedEntries, changedEntries)
}

private inline fun <K, V, R : Any> Map<out K, V>.mapValueNotNull(transform: (V) -> R?): Map<K, R> {
    return this.mapNotNull { (key, value) ->
        val newValue = transform(value)
        when {
            newValue != null -> key to newValue
            else -> null
        }
    }.toMap()
}
