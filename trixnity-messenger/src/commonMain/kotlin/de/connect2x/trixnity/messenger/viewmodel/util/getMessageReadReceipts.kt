package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction.FORWARDS
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import net.folivo.trixnity.utils.concurrentMutableMap


private val log = KotlinLogging.logger {}

fun getMessageIsRead(
    client: MatrixClient, senderUserId: UserId, roomId: RoomId, eventId: EventId,
): Flow<Boolean?> = isReadSearch(client, senderUserId, roomId, eventId).map {
    // TODO: escape on first read == true
    when (it) {
        is IsReadSearchResult.Read -> true
        IsReadSearchResult.Unread -> false
    }
}.takeWhileInclusive { !it }

@OptIn(ExperimentalCoroutinesApi::class)
fun getMessageReadReceipts(
    client: MatrixClient, senderUserId: UserId, roomId: RoomId, eventId: EventId,
): Flow<List<RoomUser>> = flow {
    val cumulatedReads = mutableSetOf<UserId>()
    isReadSearch(client, senderUserId, roomId, eventId)
        .collect {
            when (it) {
                is IsReadSearchResult.Read -> {
                    cumulatedReads.addAll(it.readBy)
                    emit(cumulatedReads.toList())
                }

                IsReadSearchResult.Unread -> {
                    if (cumulatedReads.isEmpty()) emit(cumulatedReads.toList())
                }
            }
        }
}.flatMapLatest { userIds ->
    if (userIds.isEmpty()) flowOf(emptyList())
    else {
        val roomUserFlows = userIds
            .map { userId -> client.user.getById(roomId, userId) }
        combine(roomUserFlows) { roomUsers ->
            roomUsers.filterNotNull().toList()
        }
    }
}

/**
 * TODO This algorithm has a few issues (mostly edge cases):
 *   - Ressource consumption: Too many same re-computations are done for each element.
 *     For example when far away from the last event and only ourself wrote messages.
 *   - Wrong results: On membership change depending on history visibility we may getting wrong results.
 *     For example when A sends a message and B joins, B may not be able to read at all but is marked as reader.
 *   Possible solution: lazily calculate Map<EventId,Set<UserId>> (sorted) in TimelineViewModel, which can be iterated through.
 *   This List must also forget "old" events, when not needed anymore and consider membership changes depending on history visibility.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun isReadSearch(
    client: MatrixClient,
    senderUserId: UserId,
    roomId: RoomId,
    eventId: EventId,
): Flow<IsReadSearchResult> =
    getReceipts(client, senderUserId, roomId).flatMapLatest { receipts ->
        log.trace { "isReadSearch: senderUserId=$senderUserId roomId=$roomId eventId=$eventId" }
        client.room.getTimelineEvents(roomId, eventId, FORWARDS)
            .transform {
                val timelineEvent = it.first()
                val sender = timelineEvent.sender
                val currentEventId = timelineEvent.eventId
                val currentRoomId = timelineEvent.roomId
                val foundReaders = buildSet {
                    addAll(receipts[currentEventId].orEmpty())
                    add(sender)
                    remove(senderUserId)
                    remove(client.userId)
                }
                when {
                    foundReaders.isNotEmpty() -> emit(IsReadSearchResult.Read(foundReaders))
                    currentRoomId != roomId -> emitAll(
                        isReadSearch(
                            client,
                            senderUserId,
                            currentRoomId,
                            currentEventId,
                        ) // Recursive!
                    )

                    else -> emit(IsReadSearchResult.Unread)
                }
            }
    }

private val getReceiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()
private fun getReceipts(
    client: MatrixClient,
    userId: UserId, roomId: RoomId,
): Flow<Map<EventId, Set<UserId>>> = flow {
    emitAll(
        getReceiptsByEventCache.read { get(roomId) }
            ?: getReceiptsByEventCache.write {
                getOrPut(roomId) {
                    client.user.getAllReceipts(roomId)
                        .flattenNotNull()
                        .map { receipts ->
                            receipts
                                .mapNotNull { (key, value) ->
                                    if (key == userId) null
                                    else value.receipts[Read]
                                        ?.let { it.eventId to key }
                                }
                                .groupBy { it.first }
                                .mapValues { it.value.map { it.second }.toSet() }
                        }
                        .distinctUntilChanged()
                }
            }
    )
}

private sealed interface IsReadSearchResult {
    data object Unread : IsReadSearchResult
    data class Read(val readBy: Set<UserId>) : IsReadSearchResult
}
