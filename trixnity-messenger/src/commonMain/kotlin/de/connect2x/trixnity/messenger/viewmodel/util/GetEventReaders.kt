package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId


private val log = KotlinLogging.logger {}

interface GetEventReaders {
    fun isRead(
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        sender: UserId,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
    ): Flow<Boolean>

    fun isReadBy(
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        sender: UserId,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
        initials: Initials,
        avatarMaxSize: Long,
    ): Flow<List<UserInfoElement>?>
}

class GetEventReadersImpl : GetEventReaders {
    private sealed interface IsReadSearchResult {
        data object Unread : IsReadSearchResult
        data class Read(val readBy: Set<UserId>) : IsReadSearchResult
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
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        sender: UserId,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>
    ): Flow<IsReadSearchResult> =
        getReceipts(roomId).flatMapLatest { receipts ->
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.FORWARDS)
                .transform {
                    val timelineEvent = it.first()
                    val currentSender = timelineEvent.sender
                    val currentEventId = timelineEvent.eventId
                    val currentRoomId = timelineEvent.roomId
                    val foundReaders = buildSet {
                        addAll(receipts[currentEventId].orEmpty())
                        add(currentSender)
                        remove(sender)
                        remove(matrixClient.userId)
                    }
                    when {
                        foundReaders.isNotEmpty() -> emit(IsReadSearchResult.Read(foundReaders))
                        currentRoomId != roomId -> emitAll(
                            // recursive!
                            isReadSearch(matrixClient, currentRoomId, currentEventId, sender, getReceipts)
                        )

                        else -> emit(IsReadSearchResult.Unread)
                    }.also {
                        log.trace { "isReadSearch: currentRoomId=$currentRoomId currentEventId=$currentEventId foundReaders=$foundReaders" }
                    }
                }
        }

    override fun isRead(
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        sender: UserId,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
    ): Flow<Boolean> =
        isReadSearch(matrixClient, roomId, eventId, sender, getReceipts).map {
            when (it) {
                is IsReadSearchResult.Read -> true
                IsReadSearchResult.Unread -> false
            }
        }.takeWhileInclusive { !it }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun isReadBy(
        matrixClient: MatrixClient,
        roomId: RoomId,
        eventId: EventId,
        sender: UserId,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
        initials: Initials,
        avatarMaxSize: Long,
    ): Flow<List<UserInfoElement>?> =
        flow {
            val cumulatedReads = mutableSetOf<UserId>()
            isReadSearch(matrixClient, roomId, eventId, sender, getReceipts)
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
            else combine(userIds.map { userId ->
                matrixClient.user.getById(
                    roomId,
                    userId
                )
            }) { it }
                .scopedMapLatest { roomUsers ->
                    roomUsers.mapNotNull { user ->
                        if (user == null) return@mapNotNull null
                        user.toUserInfoElement(this, matrixClient, initials, avatarMaxSize, user.userId)
                    }
                }
        }

}
