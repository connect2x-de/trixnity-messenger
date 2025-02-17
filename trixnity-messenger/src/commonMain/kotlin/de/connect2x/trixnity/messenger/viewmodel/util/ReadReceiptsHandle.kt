package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.ReadReceiptsHandle.Reader
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction.FORWARDS
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface ReadReceiptsHandleFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        senderId: UserId,
        cache: ReadReceiptsCache,
    ): ReadReceiptsHandle =
        ReadReceiptsHandleImpl(
            eventId = eventId,
            roomId = roomId,
            senderId = senderId,
            initials = viewModelContext.get<Initials>(),
            client = viewModelContext.matrixClient,
            config = viewModelContext.get<MatrixMessengerConfiguration>(),
            cache = cache,
            scope = viewModelContext.coroutineScope,
        )

    companion object : ReadReceiptsHandleFactory
}

interface ReadReceiptsHandle {
    val isRead: Flow<Boolean>
    val isReadBy: Flow<Set<Reader>>

    data class Reader(
        val userId: UserId,
        val userInfo: StateFlow<UserInfoElement?>,
    ) { // For correct handling in Sets, only compare userIds
        override fun hashCode() = userId.hashCode()
        override fun equals(other: Any?) =
            other is Reader && other.userId == userId
    }
}

class ReadReceiptsHandleImpl(
    val eventId: EventId,
    val roomId: RoomId,
    val senderId: UserId,
    val initials: Initials,
    val client: MatrixClient,
    val config: MatrixMessengerConfiguration,
    val cache: ReadReceiptsCache,
    val scope: CoroutineScope,
) : ReadReceiptsHandle {

    override val isRead: Flow<Boolean> =
        isReadSearch(roomId, eventId)
            .map {
                when (it) {
                    is IsReadSearchResult.Read -> true
                    IsReadSearchResult.Unread -> false
                }
            }
            .distinctUntilChanged()
            .takeWhileInclusive { !it }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isReadBy: Flow<Set<Reader>> =
        flow {
            val cumulatedReads = mutableSetOf<UserId>()
            isReadSearch(roomId, eventId)
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
        }
            .distinctUntilChanged()
            .mapLatest {
                it.map { userId ->
                    userId.toReader()
                }.toSet()
            }

    private fun UserId.toReader() =
        Reader(
            userId = this,
            userInfo = this.toUserInfoFlow(),
        )

    private fun UserId.toUserInfoFlow() = client
        .user.getById(roomId, this)
        .map {
            it.toUserInfoElement(
                coroutineScope = scope,
                matrixClient = client,
                initials = initials,
                config.avatarMaxSize,
                this,
            )
        }
        .stateIn(scope, whileSubscribedWithTimeout, null)

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
    private fun isReadSearch(roomId: RoomId, eventId: EventId): Flow<IsReadSearchResult> =
        cache
            .getReceipts(roomId)
            .flatMapLatest { receipts ->
                log.trace { "isReadSearch: roomId=$roomId eventId=$eventId" }
                client.room.getTimelineEvents(roomId, eventId, FORWARDS)
                    .transform {
                        val timelineEvent = it.first()
                        val sender = timelineEvent.sender
                        val currentEventId = timelineEvent.eventId
                        val currentRoomId = timelineEvent.roomId
                        val foundReaders = buildSet {
                            addAll(receipts[currentEventId].orEmpty())
                            add(sender)
                            remove(senderId)
                            remove(client.userId)
                        }
                        when {
                            foundReaders.isNotEmpty() -> emit(IsReadSearchResult.Read(foundReaders))
                            currentRoomId != roomId -> emitAll(
                                isReadSearch(
                                    currentRoomId,
                                    currentEventId,
                                )
                            ) // recursive!
                            else -> emit(IsReadSearchResult.Unread)
                        }
                    }
            }
}

interface ReadReceiptsCacheFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): ReadReceiptsCache =
        ReadReceiptsCacheImpl(
            client = viewModelContext.matrixClient,
            scope = viewModelContext.coroutineScope,
        )

    companion object : ReadReceiptsCacheFactory
}

interface ReadReceiptsCache {
    fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>>
}

class ReadReceiptsCacheImpl(
    private val client: MatrixClient,
    private val scope: CoroutineScope,
) : ReadReceiptsCache {
    // Don't use ConcurrentMap as it saves its contents twice. Not ideal for caching flows.
    private val _receiptsCache = MutexMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()
    override fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>> =
        flow {
            emitAll(
                _receiptsCache.getOrSet(roomId) {
                    client.getReadReceipts(roomId)
                        .stateIn(scope, WhileSubscribed(), mapOf())
                })
        }

    private fun MatrixClient.getReadReceipts(
        roomId: RoomId,
    ): Flow<Map<EventId, Set<UserId>>> =
        user
            .getAllReceipts(roomId)
//            .debounceAfterFirst(500.milliseconds)
//            .distinctUntilChanged()
            .flattenNotNull()
            .map { receipts ->
                receipts
                    .mapNotNull { (userId, userReceipts) ->
                        if (userId == this.userId) null
                        else userReceipts.receipts[Read]
                            ?.let { it.eventId to userId }
                    }
                    .groupBy { (eventId, _) -> eventId }
                    .mapValues { (_, eventIdsToUserIds) ->
                        eventIdsToUserIds.map { (_, userId) -> userId }.toSet()
                    }
            }
}

private class MutexMap<K, V> {
    private val mutex = Mutex()
    private val map = mutableMapOf<K, V>()

    suspend fun getOrSet(key: K, constructor: suspend () -> V): V =
        mutex.withLock {
            map.getOrPut(key) { constructor() }
        }
}
