package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
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
        val previousEvent = event.unsigned?.previousContent as? PowerLevelsEventContent ?: return@flow

        val userDiff = findMapDifference(previousEvent.users, content.users)
        emitAll(matrixClient.user.getById(roomId, timelineEventSnapshot.sender).transform { userInfo ->
            val changer = userInfo?.name ?: timelineEventSnapshot.sender.full

            userDiff.newEntries.forEach { (userId, newPowerLevel) ->
                val user = matrixClient.user.getById(roomId, userId).first()?.name ?: userId.full
                emit(i18n.eventPowerLevelChange(changer, user, newPowerLevel))
            }
            userDiff.changedValues.forEach { (userId, levels) ->
                val (oldPowerLevel, newPowerLevel) = levels
                val user = matrixClient.user.getById(roomId, userId).first()?.name ?: userId.full
                emit(i18n.eventPowerLevelChange(changer, user, newPowerLevel, oldPowerLevel))
            }
            // userDiff.removedEntries means the user no longer exists so no power level change will be shown
        })
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
}

private data class MapDifference(
    val newEntries: Map<UserId, Long>,
    val removedEntries: Map<UserId, Long>,
    val changedValues: Map<UserId, Pair<Long, Long>> // Key -> (Old Value, New Value)
)

private fun findMapDifference(oldMap: Map<UserId, Long>, newMap: Map<UserId, Long>): MapDifference {
    // Keys that are in the new map but not in the old one
    val newKeys = newMap.keys.subtract(oldMap.keys)

    // Keys that were in the old map but are not in the new one
    val removedKeys = oldMap.keys.subtract(newMap.keys)

    // Keys that exist in both maps, which we need to check for changes
    val commonKeys = newMap.keys.intersect(oldMap.keys)

    // 1. Find new entries
    val newEntries = newMap.filterKeys { it in newKeys }

    // 2. Find removed entries
    val removedEntries = oldMap.filterKeys { it in removedKeys }

    // 3. Find changed values among common keys
    val changedValues = commonKeys.filter { key -> oldMap[key] != newMap[key] }
        .associateWith { key -> Pair(oldMap[key]!!, newMap[key]!!) } // !! is safe here

    return MapDifference(newEntries, removedEntries, changedValues)
}

