package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.util.IsReadSearchResult.IsReadBy
import de.connect2x.trixnity.messenger.viewmodel.util.IsReadSearchResult.IsUnread
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
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
import net.folivo.trixnity.utils.ConcurrentMap
import net.folivo.trixnity.utils.concurrentMutableMap


private val log = KotlinLogging.logger {}

/**
 * This method will keep emitting `false` until it has finally determined that the message has been seen
 * by the selected user and then return a single `true` or run out of the underlying read marker search
 * and stop at `false`.
 */
fun getMessageIsRead(
    client: MatrixClient,
    senderId: UserId,
    roomId: RoomId,
    eventId: EventId,
): Flow<Boolean> =
    searchReaders(client, senderId, roomId, eventId)
        .map { it is IsReadBy }
        .takeWhileInclusive { !it }

/**
 * This method will keep emitting a collection of users who theoretically must've seen the selected event
 * by either the actual read receipts or through inference of what other future events have been seen
 * and thus probably also this one.
 * An updated list of readers is emitted on each evaluated event of the underlying read marker search.
 */
fun getMessageReadReceipts(
    client: MatrixClient,
    senderId: UserId,
    roomId: RoomId,
    eventId: EventId,
): Flow<Map<UserId, Flow<RoomUser?>>> =
    searchReaders(client, senderId, roomId, eventId)
        .map {
            when (it) {
                is IsReadBy -> it.readers.associateWith { userId ->
                    client.user.getById(roomId, userId)
                }

                else -> emptyMap()
            }
        }

// TODO This algorithm has a few issues (mostly edge cases):
//   - Ressource consumption: Too many same re-computations are done for each element.
//     For example when far away from the last event and only ourself wrote messages.
//   - Wrong results: On membership change depending on history visibility we may getting wrong results.
//     For example when A sends a message and B joins, B may not be able to read at all but is marked as reader.
//   Possible solution: lazily calculate Map<EventId,Set<UserId>> (sorted) in TimelineViewModel, which can be iterated through.
//   This List must also forget "old" events, when not needed anymore and consider membership changes depending on history visibility.
@OptIn(ExperimentalCoroutinesApi::class)
private fun searchReaders(
    client: MatrixClient,
    senderId: UserId,
    roomId: RoomId,
    eventId: EventId,
    receiptsPerRoomCache: ConcurrentMap<RoomId, Map<EventId, Set<UserId>>> = concurrentMutableMap(),
    foundReaders: MutableSet<UserId> = mutableSetOf(), // Used internally to store cumulative results.
): Flow<IsReadSearchResult> =
//    flow {
//        receiptsPerRoomCache.read { get(roomId) }
//            ?.let { emit(it) }
//            ?: emitAll(getReceipts(client, roomId, senderId).map {
//                receiptsPerRoomCache.write { getOrPut(roomId) { it } }
//            })
//    }
    getReceipts(client, roomId, senderId)
        .flatMapLatest { receipts ->
        log.trace { "isReadSearch: senderUserId=$senderId roomId=$roomId eventId=$eventId" }
        client.room.getTimelineEvents(roomId, eventId, FORWARDS)
            .flatMapMerge { it } // The flow processing order doesn't matter.
            .transform { timelineEvent ->
                val currentSenderId = timelineEvent.sender
                val currentEventId = timelineEvent.eventId
                val currentRoomId = timelineEvent.roomId
//                timelineEvent.event.apply {
//                    if (this is ClientEvent.RoomEvent.StateEvent<*>
//                        && content is MemberEventContent
//                    ) {
//                        // TODO: do some visibility filtering
//                    }
//                }
                receipts[currentEventId]?.let {
                    if (it.isNotEmpty()) foundReaders.addAll(it)
                }
                if (currentSenderId != senderId && currentSenderId != client.userId) {
                    foundReaders.add(currentSenderId)
                }
                when {
                    foundReaders.isNotEmpty() -> emit(IsReadBy(foundReaders))

                    currentRoomId != roomId -> emitAll(
                        // Search recursively in the other room.
                        // TODO: consider using a cache to optimize jumps between the same rooms
                        searchReaders(
                            client,
                            senderId,
                            currentRoomId,
                            currentEventId,
                        )
                    )

                    else -> emit(IsUnread)
                }
            }
    }

/**
 * This method returns all users with read receipts from the selected room
 * while excluding the sender and the current user.
 */
private fun getReceipts(
    client: MatrixClient,
    roomId: RoomId,
    senderId: UserId,
): Flow<Map<EventId, Set<UserId>>> =
    client.user.getAllReceipts(roomId)
        // TODO return previous results if getAllReceipts didn't yield a different result.
        .distinctUntilChanged() // This should buffer results until the local store has relevant updates.
        .flattenNotNull()
        .map {
            it.filterValues { receipt -> receipt.receipts[Read] != null } // Pass trims succeeding loops.
                .let { readEvents ->
                    buildMap<EventId, MutableSet<UserId>> {
                        readEvents.forEach { (userId, receipt) ->
                            val eventId = receipt.receipts[Read]!!.eventId
                            eventId to (getOrPut(eventId) { mutableSetOf() }.apply {
                                // The sender and current user should be ignored.
                                if (userId != senderId && userId != client.userId) add(userId)
                            })
                        }
                    }
                }
        }

private sealed interface IsReadSearchResult {
    data object IsUnread : IsReadSearchResult
    data class IsReadBy(val readers: Set<UserId>) : IsReadSearchResult
}
