package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.ReadReceiptsHandle.Reader
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.debounceAfterFirst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import kotlin.time.Duration.Companion.milliseconds


/*
interface ReadReceiptsHandle {
    val eventId: EventId
    val roomId: RoomId
    val filtered: Set<UserId>
    val isRead: StateFlow<Boolean>
    val readReceiptsCumulative: StateFlow<Set<Reader>>
    val readReceiptsSingle: StateFlow<Set<Reader>>

    data class Reader(
        val userId: UserId,
        val userInfo: StateFlow<UserInfoElement?>,
    ) { // For correct handling in Sets, only compare userIds
        override fun hashCode() = userId.hashCode()
        override fun equals(other: Any?) =
            other is Reader && other.userId == userId
    }

    object Empty : ReadReceiptsHandle {
        override val eventId = EventId("")
        override val roomId = RoomId("")
        override val filtered = setOf<UserId>()
        override val isRead = MutableStateFlow(false)
        override val readReceiptsCumulative = MutableStateFlow(setOf<Reader>())
        override val readReceiptsSingle = MutableStateFlow(setOf<Reader>())
    }
}
*/


interface ReadReceiptsHandleFactory {
    fun create(
        eventId: EventId,
        senderId: UserId,
        cache: ReadReceiptsCache,
        scope: CoroutineScope,
    ): ReadReceiptsHandle =
        Handle(
            eventId = eventId,
            senderId = senderId,
            cache = cache,
            scope = scope,
        )

    companion object : ReadReceiptsHandleFactory
}

interface ReadReceiptsHandle {
    val isRead: Flow<Boolean>
    val readReceiptsCumulative: Flow<Set<Reader>>
    val readReceiptsSingle: Flow<Set<Reader>>

    data class Reader(
        val userId: UserId,
        val userInfo: StateFlow<UserInfoElement?>,
    ) { // For correct handling in Sets, only compare userIds
        override fun hashCode() = userId.hashCode()
        override fun equals(other: Any?) =
            other is Reader && other.userId == userId
    }
}

class Handle(
    val eventId: EventId,
    val senderId: UserId,
    val cache: ReadReceiptsCache,
    val scope: CoroutineScope,
) : ReadReceiptsHandle {
    override val isRead: Flow<Boolean> =
        cache
            .getReceipts()
            .map { true } // TODO

    override val readReceiptsCumulative =
        cache
            .getReceipts()
            .map { setOf<Reader>() } // TODO

    override val readReceiptsSingle =
        cache
            .getReceipts()
            .map { setOf<Reader>() } // TODO
}

interface ReadReceiptsCacheFactory {
    fun create(
        roomId: RoomId,
        client: MatrixClient,
        scope: CoroutineScope,
    ): ReadReceiptsCache =
        ReadReceiptsCacheImpl(
            roomId = roomId,
            client = client,
            scope = scope,
        )

    companion object : ReadReceiptsCacheFactory
}

interface ReadReceiptsCache {
    fun getReceipts(): Flow<Map<EventId, Set<UserId>>>
}

class ReadReceiptsCacheImpl(
    private val roomId: RoomId,
    private val client: MatrixClient,
    private val scope: CoroutineScope,
) : ReadReceiptsCache {
    // Don't use ConcurrentMap as it saves its contents twice. Not ideal for caching flows.
    private val _receiptsCache = MutexMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()
    override fun getReceipts(): Flow<Map<EventId, Set<UserId>>> =
        flow {
            emitAll(
                _receiptsCache
                    .getOrSet(roomId) {
                        client
                            .getReadReceipts(roomId)
//                .orderingCache(roomId, client)
//                .onCompletion {
//                    // TODO: The timeline purges all the element holders before it creates new ones.
//                    //  This means that onComplete is called even though the channel flow should be kept alive.
//                    //  Solution 1: have the timeline VM subscribe to this.
//                    //  Solution 2: build and save a timer flow which subscribes to this and deletes it after
//                    //      some time and there's been no new cache retrievals for this flow.
//                    receiptsFlowCache.remove(roomId)
//                    receiptsHandleCache.removeIf { key, _ -> key.roomId == roomId }
//                }
//                // Persist the current receipts data and update channel for all by room.
//                // Note: the coroutine scopes are set to the one when this shared item is created but not when it's retrieved.
                            .stateIn(scope, WhileSubscribed(), mapOf())
                    })
        }
}

private fun MatrixClient.getReadReceipts(
    roomId: RoomId,
//    ignore: Set<UserId>,
): Flow<Map<EventId, Set<UserId>>> =
    user
        .getAllReceipts(roomId)
        .debounceAfterFirst(500.milliseconds)
        .distinctUntilChanged()
        .flattenNotNull()
        .map { receipts ->
            receipts
                .mapNotNull { (userId, userReceipts) ->
                    if (userId == this.userId) null
//                    if (ignore.contains(userId)) null
                    else userReceipts.receipts[Read]
                        ?.let { it.eventId to userId }
                }
                .groupBy { (eventId, _) -> eventId }
                .mapValues { (_, eventIdsToUserIds) ->
                    eventIdsToUserIds.map { (_, userId) -> userId }.toSet()
                }
        }

/*
@OptIn(ExperimentalCoroutinesApi::class)
private fun MatrixClient.getReceipts1(
    roomId: RoomId,
    ignore: Set<UserId>,
) =
    user
        .getAllReceipts(roomId)
        .debounceAfterFirst(500.milliseconds)
        .distinctUntilChanged()
        .flattenNotNull()
        .mapLatest { userReceipts ->
            buildMap<EventId, MutableSet<UserId>> {
                userReceipts.entries.forEach { (userId, receipt) ->
                    receipt.receipts[Read]?.let { (eventId, receipt) ->
                        getOrPut(eventId) { mutableSetOf() }.also {
                            if (!ignore.contains(userId)) {
                                it.add(userId)
                            }
                        }
                    }
                }
            }
        }

@OptIn(ExperimentalCoroutinesApi::class)
private fun MatrixClient.getReceipts(
    roomId: RoomId,
    ignore: Set<UserId>,
): Flow<Map<EventId, Set<UserId>>> =
    user.getAllReceipts(roomId)
        .flattenNotNull()
        .mapLatest { userReceipts ->
            buildMap<EventId, MutableSet<UserId>> {
                userReceipts.entries.forEach { (userId, receipt) ->
                    receipt.receipts[Read]?.eventId.let {
                        // The sender and current user should be ignored.
//                        if (userId == client.userId || userId == senderId) null else it
                        if (ignore.contains(userId)) null else it
                    }?.let { eventId ->
                        getOrPut(eventId) { mutableSetOf() }.apply { add(userId) }
                    }
                }
            }
        }
        .distinctUntilChanged()
*/


private class MutexMap<K, V> {
    private val mutex = Mutex()
    private val map = mutableMapOf<K, V>()

    suspend fun getOrSet(key: K, constructor: suspend () -> V): V =
        mutex.withLock {
            val has = map.contains(key)
            map.getOrPut(key) { constructor() }
//                .also { log.debug { "=== ${if (has) "getting" else "added"}: $key - ${map.size} items now cached" } }
        }

    suspend fun remove(key: K): Unit =
        mutex.withLock {
            map.remove(key)
//            log.debug { "=== removed: $key - ${map.size} cached items remaining" }
        }

    suspend fun removeIf(predicate: (K, V) -> Boolean): Unit =
        mutex.withLock {
            val oldSize = map.size
            map.filter { predicate(it.key, it.value).not() }.let {
                map.clear()
                map.putAll(it)
            }
//            log.debug { "=== removed many: $oldSize -> ${map.size} cached items remaining" }
        }
}
