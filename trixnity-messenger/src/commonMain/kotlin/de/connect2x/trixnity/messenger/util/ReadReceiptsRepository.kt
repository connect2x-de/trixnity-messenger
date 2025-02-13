package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.ReadReceiptsRepository.ReadReceipts
import de.connect2x.trixnity.messenger.util.ReadReceiptsRepository.ReadReceiptsHandle
import de.connect2x.trixnity.messenger.util.ReadReceiptsRepository.ReadReceiptsHandle.Reader
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.debounceAfterFirst
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import net.folivo.trixnity.utils.Concurrent
import net.folivo.trixnity.utils.concurrentOf
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

interface ReadReceiptsRepository {
    fun getReadReceipts(
        client: MatrixClient,
        eventId: EventId,
        roomId: RoomId,
        filter: Set<UserId>,
        scope: CoroutineScope,
    ): Flow<ReadReceiptsHandle>

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

    interface ReadReceipts {
        operator fun get(eventId: EventId): Deferred<SortedReadEvent?>
        val fromLatestToEarliest: Deferred<Iterator<SortedReadEvent>>
        fun isCompleteFrom(eventId: EventId): Deferred<Boolean> // Useful to indicate further loading.
        fun registerTimelineFetchPosition(timestamp: Long)

        object Empty : ReadReceipts {
            override fun get(eventId: EventId) = CompletableDeferred<SortedReadEvent?>(value = null)
            override fun isCompleteFrom(eventId: EventId) = CompletableDeferred(value = false)
            override fun registerTimelineFetchPosition(timestamp: Long) {}
            override val fromLatestToEarliest = CompletableDeferred<Iterator<SortedReadEvent>>(value =
                object : Iterator<SortedReadEvent> {
                    override fun hasNext() = false
                    override fun next() = throw NoSuchElementException()
                })
        }
    }
}

class ReadReceiptsRepositoryImpl(
    private val config: MatrixMessengerConfiguration,
    private val initials: Initials,
) : ReadReceiptsRepository {

    private data class ReceiptsHandleCacheKey
        (val userId: UserId, val eventId: EventId, val roomId: RoomId, val filter: Set<UserId>)

    // Don't use ConcurrentMap as it saves its contents twice. Not ideal for caching flows.
    private val receiptsHandleCache = MutexCache<ReceiptsHandleCacheKey, ReadReceiptsHandle>()
    override fun getReadReceipts(
        client: MatrixClient,
        eventId: EventId,
        roomId: RoomId,
        filter: Set<UserId>,
        scope: CoroutineScope,
    ): Flow<ReadReceiptsHandle> =
        flow {
            val key = ReceiptsHandleCacheKey(client.userId, eventId, roomId, filter)
            emit(
                receiptsHandleCache.getOrSet(key) {
                    ReadReceiptsHandleImpl(
                        eventId = eventId,
                        roomId = roomId,
                        filtered = filter,
                        scope = scope,
                        client = client,
                        config = config,
                        initials = initials,
                        receipts = getReceiptsFlow(roomId, filter, client, scope)
                            .onCompletion { receiptsHandleCache.remove(key) },
                    )
                })
        }

    // Don't use ConcurrentMap as it saves its contents twice. Not ideal for caching flows.
    private val receiptsFlowCache = MutexCache<RoomId, Flow<ReadReceipts>>()
    private suspend fun getReceiptsFlow(
        roomId: RoomId,
        filter: Set<UserId>,
        client: MatrixClient,
        scope: CoroutineScope,
    ): Flow<ReadReceipts> =
        receiptsFlowCache.getOrSet(roomId) {
            client
                .getReceipts(roomId, filter)
                .orderingCache(roomId, client)
                .onCompletion {
                    // TODO: The timeline purges all the element holders before it creates new ones.
                    //  This means that onComplete is called even though the channel flow should be kept alive.
                    //  Solution 1: have the timeline VM subscribe to this.
                    //  Solution 2: build and save a timer flow which subscribes to this and deletes it after
                    //      some time and there's been no new cache retrievals for this flow.
                    receiptsFlowCache.remove(roomId)
                    receiptsHandleCache.removeIf { key, _ -> key.roomId == roomId }
                }
                // Persist the current receipts data and update channel for all by room.
                // Note: the coroutine scopes are set to the one when this shared item is created but not when it's retrieved.
                .stateIn(scope, WhileSubscribed(), ReadReceipts.Empty)
        }

    private fun Flow<Map<EventId, ReadReceiptResult>>.orderingCache(roomId: RoomId, client: MatrixClient) =
        channelFlow<ReadReceipts> {
            val scope = this
            val orderedEvents = concurrentOf { ReadEventsOrdering() } // TODO: mutex?
            val fetchCutoffTracker = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 32)
            val updateWrapper = ConcurrentReadEventsOrderingReceipts(orderedEvents, fetchCutoffTracker, scope)
            val receiptsByEventId = MutableStateFlow<Map<EventId, ReadReceiptResult>>(mapOf()) // Atomic changes.
            val fetchLimit = MutableStateFlow(Long.MAX_VALUE)

            log.debug { "======= SETUP RECEIPTS CHANNEL: $roomId" }

            suspend fun onReceiveTimestamp(event: TimelineEvent) {
                val eventId = event.eventId
                val readers = receiptsByEventId.value[eventId]?.usersByEvent ?: return
                val timestamp = event.originTimestamp
                val orderable = OrderableReadEvent(timestamp, eventId, readers)
//                val collectedReads = orderedEvents.read { size } // TODO: remove
                orderedEvents.write {
                    transaction {
                        if (orderable.readers.isNotEmpty()) {
                            add(orderable)
//                            log.debug { "--- added ordered event: ${orderable.eventId} at: ${orderable.timestamp} with ${orderable.readers.size} readers to container of size: $collectedReads" }
                        }
                    }
                }
            }

            // What we care about is to get all the timestamps of events with read receipts.
            // Because without we can't tell if other event's readers already got past it or not.
            // And with, checking the read state is trivial.

            val receiptsTimestampRetrievalJob = MutableStateFlow<Job?>(null)
            fun launchReceiptsTimestampRetrieval(): Unit = receiptsByEventId.value.let { receipts ->
                receiptsTimestampRetrievalJob.apply {
                    data class FetchRequest(
                        val eventId: EventId,
                        val fetchTimeout: Duration,
                        val timestampMostEarly: Long,
                        val timestampMostRecent: Long,
                    )
                    value?.cancel()
                    value = scope.launch {
                        log.debug { "============= launch fetch of up to ${receipts.size} read events" }
                        val prioritized = fetchLimit.value.let { limit ->
                            receipts.map { (eventId, receipt) ->
                                FetchRequest(
                                    eventId, fetchTimeout = ZERO,
                                    receipt.timestampMostEarly ?: 0L,
                                    receipt.timestampMostRecent ?: 0L,
                                )
                            }
                                .filter { it.timestampMostEarly >= limit }
                                .sortedByDescending { it.timestampMostRecent }
                        }
                        val fetchQueue = ArrayDeque(prioritized) // TODO concurrency safe?
                        while (fetchQueue.isNotEmpty()) {
                            fetchQueue.removeFirstOrNull()?.let { request ->
                                client
                                    .fetchTimelineEvent(roomId, request.eventId, request.fetchTimeout)
                                    .catch {
                                        fetchQueue.addLast(request.copy(fetchTimeout = 5.milliseconds))
                                        log.debug { "============= FAILED RECEIPT 1: ${request.eventId} - (lim: ${fetchLimit.value})" }
                                    }
                                    .map {
                                        it?.also { event ->
                                            log.debug { "============= FETCHED RECEIPT: ${request.eventId} -> ${event.originTimestamp} - (lim: ${fetchLimit.value})" }
                                            // TODO: If timeline event's roomId leads to different room
                                            //  start collecting the according stateflow of this channel flow for
                                            //  that other room and combine the results.
                                            //  But exact case needs to be clarified!
                                            onReceiveTimestamp(event)
                                        }
                                            ?: also {
                                                fetchQueue.addLast(request.copy(fetchTimeout = 5.milliseconds))
                                                log.debug { "============= FAILED RECEIPT 2: ${request.eventId} - (lim: ${fetchLimit.value})" }
                                            }
                                    }
                                    .first()
                            }
                            delay(1.milliseconds) // Skip frames to not lock up the routine.
                        }
                        send(updateWrapper)
                    }.apply {
                        invokeOnCompletion { log.debug { "====== completed fetch loop" } }
                    }
                }
            }

            // Listen for eventIds to set the fetch range cutoff.
            scope.launch {
                fetchCutoffTracker.collect { timestamp ->
                    val currentLimit = fetchLimit.value
                    when {
                        currentLimit <= 0L || timestamp <= 0L -> {}
                        currentLimit <= 0L -> fetchLimit.value = timestamp
                        else -> fetchLimit.value = min(currentLimit, timestamp)
                    }
                    launchReceiptsTimestampRetrieval()
                    if (currentLimit != fetchLimit.value) log
                        .debug { "=== changed fetch timestamp cutoff: $currentLimit -> ${fetchLimit.value} for: $roomId" }
                }
            }

            collectLatest { receipts ->
                receiptsByEventId.value = receipts

                // Discard sorted list since receipts have changed.
                orderedEvents.write { clear() }

                // Load or re-load all read events from the store cache.
                launchReceiptsTimestampRetrieval()
            }
        }

    /**
     * @return All users with read receipts for the selected room
     * grouped by read receipt event ids and containing the timestamp range
     * while excluding the sender and the current user.
     *
     * Can contain results with empty collections of users. These should be ignored.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun MatrixClient.getReceipts(
        roomId: RoomId,
        exclude: Set<UserId>,
    ): Flow<Map<EventId, ReadReceiptResult>> =
        user.getAllReceipts(roomId)
            .debounceAfterFirst(500.milliseconds)
            .distinctUntilChanged()
            .flattenNotNull()
            .mapLatest { userReceipts ->
                buildMap {
                    userReceipts.entries.forEach { (userId, receipt) ->
                        receipt.receipts.entries.forEach { (type, receipt) ->
                            val time = receipt.receipt.timestamp
                            val eventId = receipt.eventId
                            getOrPut(eventId) { ReadReceiptResult() }.also {
                                it.timestampMostEarly = min(it.timestampMostEarly ?: time, time)
                                it.timestampMostRecent = max(it.timestampMostRecent ?: time, time)
                                when (type) {
                                    Read -> if (!exclude.contains(userId)) {
                                        it.usersByEvent.add(userId)
                                    }
                                }
                            }
                        }
                    }
                }
            }

    private data class ReadReceiptResult(
        var timestampMostEarly: Long? = null, // Cutoff for fetching anything older than the selected event.
        var timestampMostRecent: Long? = null, // Prioritizing for this increases the chances to get the relevant events first.
        var usersByEvent: MutableSet<UserId> = mutableSetOf(),
    )
}

private suspend fun MatrixClient.fetchTimelineEvent(
    roomId: RoomId,
    eventId: EventId,
    timeout: Duration,
): Flow<TimelineEvent?> =
    withTimeout(10.seconds) {
        room.getTimelineEvent(roomId, eventId) {
            decryptionTimeout = ZERO
            fetchTimeout = timeout
            fetchSize = 1
            allowReplaceContent = false
        }
            // mapNotNull might make the thread hold
            // while map might skip results
            .map { event ->
                if (event != null) {
                    return@map if (event.eventId != eventId) {
                        log.warn { "event id mismatch $eventId -> ${event.eventId}; skipping timeline event" }
                        null
                    } else event
                }
                null
            }
    }

class ReadReceiptsHandleImpl(
    override val eventId: EventId,
    override val roomId: RoomId,
    override val filtered: Set<UserId>,
    private val client: MatrixClient,
    private val scope: CoroutineScope,
    private val config: MatrixMessengerConfiguration,
    private val initials: Initials,
    receipts: Flow<ReadReceipts>,
) : ReadReceiptsHandle {
    private val timestamp: StateFlow<Long> =
        combine(
            receipts,
            client.room.getTimelineEvent(roomId, eventId),
        ) { receipts, event ->
            event?.originTimestamp
                ?.also { receipts.registerTimelineFetchPosition(it) }
        }
            .filterNotNull()
            .stateIn(scope, WhileSubscribed(), Long.MAX_VALUE)

    @OptIn(FlowPreview::class)
    override val isRead =
        combine(receipts, timestamp)
        { receipts, timestamp ->
            receipts.fromLatestToEarliest.await().let { iter ->
                iter.hasNext() && iter.next().timestamp >= timestamp
            }
        }
            .debounce(16.milliseconds) // Avoid initial empty events to be propagated.
            .stateIn(scope, WhileSubscribed(), false)

    @OptIn(FlowPreview::class)
    override val readReceiptsCumulative =
        combine(receipts, timestamp)
        { receipts, timestamp ->
            buildSet {

                log.debug { "====== accumulating readers for: $eventId" }

                receipts.fromLatestToEarliest.await().let { iter ->
                    while (iter.hasNext()) {
                        // TODO: check join events and visibility setting and
                        //  filter out corresponding users accordingly since
                        //  they could not have read a message not accessible to them
                        //  AFTER they joined the room.
                        val event = iter.next()
                        if (event.timestamp < timestamp) break
                        // TODO: filter users by sender and current user
                        // TODO: check if the channel flow is triggering any updates
                        addAll(event.readers.map { it.toReader() })

                        log.debug { "=== added readers: ${event.readers} for: $eventId" }
                    }
                }
            }
        }
            .debounce(16.milliseconds) // Avoid initial empty events to be propagated.
            .stateIn(scope, WhileSubscribed(), setOf())

    @OptIn(FlowPreview::class)
    override val readReceiptsSingle =
        receipts
            .map { receipts ->
                receipts[eventId].await()?.readers
                    ?.map { userId -> userId.toReader() }?.toSet()
                    ?: setOf()
            }
            .debounce(16.milliseconds) // Avoid initial empty events to be propagated.
            .stateIn(scope, WhileSubscribed(), setOf())

    private fun UserId.toReader() =
        Reader(
            userId = this,
            userInfo = this.toUserInfoFlow()
                .stateIn(scope, whileSubscribedWithTimeout, null),
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
}

private class ConcurrentReadEventsOrderingReceipts(
    private val receipts: Concurrent<ReadEventsOrdering, ReadEventsOrdering>,
    private val relevantEventsTracker: MutableSharedFlow<Long>,
    private val scope: CoroutineScope,
) : ReadReceipts {

    override fun registerTimelineFetchPosition(timestamp: Long) {
        relevantEventsTracker.tryEmit(timestamp)
    }

    override fun get(eventId: EventId): Deferred<SortedReadEvent?> =
        scope.async {
            receipts.read {
                toList().find { it.eventId == eventId }
            }
        }

    override val fromLatestToEarliest: Deferred<Iterator<SortedReadEvent>>
        get() = scope.async {
            receipts.read {
                object : Iterator<SortedReadEvent> {
                    private val list = toList()
                    private var index = list.size - 1
                    override fun hasNext() = index >= 0
                    override fun next() =
                        if (hasNext().not()) throw NoSuchElementException()
                        else list[index]
                            .also { index-- }
                }
            }
        }

    override fun isCompleteFrom(eventId: EventId): Deferred<Boolean> =
        scope.async {
            receipts.read {
                // TODO: iterate from the most recent to eventId to check if there's still gaps of relevant read receipts
                TODO("Not yet implemented")
            }
        }
}

private class ReadEventsOrdering {
    private var _eventIds = mutableSetOf<EventId>()
    private var _events = mutableListOf<OrderableReadEvent>()

    operator fun get(index: Int) = _events[index]

    fun transaction(transaction: AddTransaction.() -> Unit) {
        transaction(object : AddTransaction {
            override fun add(event: OrderableReadEvent) {
                if (_eventIds.contains(event.eventId).not()) {
                    _events.add(event)
                    _eventIds.add(event.eventId)
                }
            }
        })
        _events = _events
            .distinctBy { it.eventId }
            .sortedBy { it.timestamp }
            .toMutableList()
    }

    interface AddTransaction {
        fun add(event: OrderableReadEvent)
    }

    val size get() = _events.size

    fun clear() {
        _events = mutableListOf()
        _eventIds = mutableSetOf()
    }

    fun print() {
        _events.forEachIndexed { i, v -> log.debug { "--- #$i \t ${v.timestamp}" } }
    }

    fun toList(): List<SortedReadEvent> = _events.toList()

    fun findEncompassing(timestamp: Long): Pair<OrderableReadEvent?, OrderableReadEvent?> {
        var past: OrderableReadEvent? = null
        for (i in (_events.size - 1) downTo 0) {
            past = _events[i]
            if (timestamp >= past.timestamp) {
                val next = if (i + 1 < _events.size) _events[i + 1] else null
                return Pair(next, past)
            }
        }
        return Pair(past, null)
    }
}

interface SortedReadEvent {
    val timestamp: Long
    val eventId: EventId
    val readers: Set<UserId>
    // TODO: joined: Set<UserId>? // To filter by visibility while traversing in reverse chronology.
}

private data class OrderableReadEvent(
    override val timestamp: Long,
    override val eventId: EventId,
    override val readers: Set<UserId>,
    var gapsToNext: Boolean = false,
    var gapsToPast: Boolean = false,
) : SortedReadEvent

class MutexCache<K, V> {
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
