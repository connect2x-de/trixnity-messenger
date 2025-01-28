package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.util.IsReadSearchResult.IsReadBy
import de.connect2x.trixnity.messenger.viewmodel.util.IsReadSearchResult.IsUnread
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import kotlin.time.Duration


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
        .distinctUntilChanged()

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
        .distinctUntilChanged()

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
    foundReaders: MutableSet<UserId> = mutableSetOf(), // Used internally to store cumulative results.
): Flow<IsReadSearchResult> = flow {
    log.trace { "searching read events from: $eventId in $roomId for $senderId" }
    var firstResult = true
    getReceipts(client, roomId, senderId)
        .flatMapLatest { receipts ->
            client.room.getTimelineEvents(
                roomId = roomId,
                startFrom = eventId,
                direction = GetEvents.Direction.FORWARDS,
                config = {
                    decryptionTimeout = Duration.ZERO
                    fetchSize = Long.MAX_VALUE
                },
            )
                .map { flow -> flow.filterNotNull().first() }
                .map { event -> Pair(receipts, event) }
        }
        .collectIndexed { index, (readReceipts, timelineEvent) ->
            val currentSenderId = timelineEvent.sender
            val currentEventId = timelineEvent.eventId
            val currentRoomId = timelineEvent.roomId
            log.trace { "found read event(#$index) $currentEventId in $currentRoomId for $currentSenderId" }
            var needsToEmit = firstResult
            firstResult = false
            readReceipts[currentEventId]?.let {
                if (it.isNotEmpty()) {
                    needsToEmit = true
                    foundReaders.addAll(it)
                }
            }
            if (currentSenderId != senderId && currentSenderId != client.userId) {
                needsToEmit = true
                foundReaders.add(currentSenderId)
            }
            when {
                currentRoomId != roomId -> emitAll(
                    // Search recursively in the other room.
                    // TODO: consider using a receipts cache to optimize jumps between the same rooms
                    searchReaders(
                        client = client,
                        senderId = senderId,
                        roomId = currentRoomId,
                        eventId = currentEventId,
                        foundReaders = foundReaders,
                    )
                )

                needsToEmit -> emit(
                    when {
                        foundReaders.isNotEmpty() -> IsReadBy(foundReaders)
                        else -> IsUnread
                    }
                )
            }
        }
}

/**
 * This method returns all users with read receipts from the selected room
 * while excluding the sender and the current user.
 */
// TODO: cache by room + current user
@OptIn(ExperimentalCoroutinesApi::class)
private fun getReceipts(
    client: MatrixClient,
    roomId: RoomId,
    senderId: UserId,
): Flow<Map<EventId, Set<UserId>>> =
    client.user.getAllReceipts(roomId)
        .flattenNotNull()
        .mapLatest { userReceipts ->
            buildMap<EventId, MutableSet<UserId>> {
                userReceipts.entries.forEach { (userId, receipt) ->
                    receipt.receipts[Read]?.eventId.let {
                        // The sender and current user should be ignored.
                        if (userId == client.userId || userId == senderId) null else it
                    }?.let { eventId ->
                        getOrPut(eventId) { mutableSetOf() }.apply { add(userId) }
                    }
                }
            }
        }
        .distinctUntilChanged()

private sealed interface IsReadSearchResult {
    data object IsUnread : IsReadSearchResult
    data class IsReadBy(val readers: Set<UserId>) : IsReadSearchResult
}
