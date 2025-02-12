package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReadReceiptsManager.ReadReceipts
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReadReceiptsManager.ReadReceipts.EmptyReceipts
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReadReceiptsManager.ReadReceiptsHandle
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReadReceiptsViewModel.ReadEvent
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.debounceAfterFirst
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.invokeOnCompletion
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.MatrixRegex.eventId
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import net.folivo.trixnity.utils.Concurrent
import net.folivo.trixnity.utils.concurrentMutableMap
import net.folivo.trixnity.utils.concurrentOf
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


// TODO: rename to ReadReceiptsManager.kt or ReadReceipts.kt


private val log = KotlinLogging.logger {}

interface ReadReceiptsManager {
    suspend fun getReadReceipts(
        client: MatrixClient,
        eventId: EventId,
        roomId: RoomId,
        filter: Set<UserId>,
        coroutineScope: CoroutineScope,
    ): ReadReceiptsHandle

    interface ReadReceiptsHandle {
        val eventId: EventId
        val roomId: RoomId
        val filtered: Set<UserId>
        val isRead: StateFlow<Boolean>
        val readReceiptsCumulative: StateFlow<Set<UserId>>
        val readReceiptsSingle: StateFlow<Set<UserId>>
    }

    interface ReadReceipts {
        operator fun get(eventId: EventId): Deferred<SortedReadEvent?>
        val fromLatestToEarliest: Deferred<Iterator<SortedReadEvent>>
        fun isCompleteFrom(eventId: EventId): Deferred<Boolean> // Useful to indicate further loading.
        val relevantEventsTracker: MutableSharedFlow<EventId> // For notifying how far the current receipts cache should fetch.

        class EmptyReceipts(private val scope: CoroutineScope) : ReadReceipts {
            override fun isCompleteFrom(eventId: EventId) = scope.async { false }
            override fun get(eventId: EventId) = scope.async { null }
            override val relevantEventsTracker get() = MutableSharedFlow<EventId>()
            override val fromLatestToEarliest: Deferred<Iterator<SortedReadEvent>>
                get() = scope.async {
                    object : Iterator<SortedReadEvent> {
                        override fun hasNext() = false
                        override fun next() = throw NoSuchElementException()
                    }
                }
        }
    }
}

class ReadReceiptsManagerImpl(
    private val initials: Initials,
) : ReadReceiptsManager {
    override suspend fun getReadReceipts(
        client: MatrixClient,
        eventId: EventId,
        roomId: RoomId,
        filter: Set<UserId>,
        coroutineScope: CoroutineScope
    ): ReadReceiptsHandle = coroutineScope {


        val a = getCachedReceipts(eventId, roomId, client, coroutineScope)
            .stateIn(coroutineScope, WhileSubscribed(), 0)


        TODO("Not yet implemented")
    }

//    val off = MutableStateFlow<EventId?>(null)

    private val receiptsByEventCache = concurrentMutableMap<RoomId, StateFlow<ReadReceiptsHandle>>()
    private fun getCachedReceipts(
        eventId: EventId,
        roomId: RoomId,
        client: MatrixClient,
        scope: CoroutineScope,
    ): Flow<ReadReceiptsHandle> =
        flow {
            emitAll(receiptsByEventCache.read { get(roomId) }
                ?: receiptsByEventCache.write {
                    getOrPut(roomId) {
                        receiptsFlow(eventId, roomId, client, scope)
                            // TODO: could be dangerous to give out state flows from potentially different and cancelled coroutines
                            // TODO: maybe use on completion to clear the cache from dead entries
                            .stateIn(
                                scope, WhileSubscribed(),
                                ReadReceiptsHandleImpl(eventId, roomId, setOf(), scope, flowOf(EmptyReceipts(scope))),
                            )
                            .also { it.onCompletion { log.debug { "--- elvis has left the building" } } }
                    }
                }
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun receiptsFlow(
        eventId: EventId,
        roomId: RoomId,
        client: MatrixClient,
        scope: CoroutineScope,
    ): Flow<ReadReceiptsHandle> =
        client.user.getAllReceipts(roomId)
            .debounceAfterFirst(10.milliseconds)
            .distinctUntilChanged()
            .flattenNotNull()
            // TODO: channelflow using "off"
            .mapLatest { userReceipts ->
                val updater =
                    MutableSharedFlow<ReadReceipts>(replay = 1)
                ReadReceiptsHandleImpl(eventId, roomId, setOf(), scope, updater)
            }
}

class ReadReceiptsHandleImpl(
    override val eventId: EventId,
    override val roomId: RoomId,
    override val filtered: Set<UserId>,
    scope: CoroutineScope,
    receipts: Flow<ReadReceipts>,
//    private val offsetter: MutableStateFlow<EventId?>,
) : ReadReceiptsHandle {

    // TODO: remove
    init {
        scope.launch {
            receipts.collectLatest {
                val a = it.fromLatestToEarliest.await()
                val b = it[eventId].await()
                log.debug { "=== handle for $eventId got receipts in collect" }
                // TODO: write these to mutable SFs
            }
        }
    }

    private val _receipts = receipts
        .map {
            val a = it.fromLatestToEarliest.await()
            val b = it[eventId].await()
            log.debug { "=== handle for $eventId got receipts" }
        }
        .stateIn(scope, WhileSubscribed(), EmptyReceipts(scope))

    override val isRead = _receipts
        .map { true }
        .stateIn(scope, WhileSubscribed(), false)

    override val readReceiptsCumulative = _receipts
        .map { setOf<UserId>() }
        .stateIn(scope, WhileSubscribed(), setOf())

    override val readReceiptsSingle = _receipts
        .map { setOf<UserId>() }
        .stateIn(scope, WhileSubscribed(), setOf())
}


interface ReadReceiptsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
//        key: String,
//        timelineEventFlow: Flow<TimelineEvent>,
    ): ReadReceiptsViewModel =
        ReadReceiptsViewModelImpl(viewModelContext = viewModelContext)

    companion object : ReadReceiptsViewModelFactory
}

// TODO: rename to ReadReceiptsManager because it should kept alive between different VMs
interface ReadReceiptsViewModel {
//    val isRead: StateFlow<Boolean?>
//    val reactions: StateFlow<Map<ReactionKey, Set<ReactionEvent>>>

    fun readReceipts(roomId: RoomId, eventId: EventId): StateFlow<Map<UserId, ReadEvent>>
    fun isRead(roomId: RoomId, eventId: EventId): StateFlow<Boolean>

    fun testGet(roomId: RoomId): StateFlow<Map<EventId, MutableSet<UserId>>>

    data class ReadEvent(
        val eventId: EventId,
        val senderFlow: StateFlow<UserInfoElement?>,
    )
}

class ReadReceiptsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : ReadReceiptsViewModel, MatrixClientViewModelContext by viewModelContext {


    override fun readReceipts(roomId: RoomId, eventId: EventId): StateFlow<Map<UserId, ReadEvent>> {
        TODO("Not yet implemented")
    }

    override fun isRead(roomId: RoomId, eventId: EventId): StateFlow<Boolean> {
        TODO("Not yet implemented")
    }

    private val receiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, ReadReceiptResult>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun testGet(roomId: RoomId) = getCachedReceipts(roomId)
        .mapLatest {
            it.mapValues { (eventId, receipt) ->
                receipt.usersByEvent
            }
        }.stateIn(coroutineScope, WhileSubscribed(), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getCachedReceipts(roomId: RoomId): Flow<Map<EventId, ReadReceiptResult>> =
        flow {
            emitAll(receiptsByEventCache.read { get(roomId) }
                ?: receiptsByEventCache.write {
                    getOrPut(roomId) {
                        getReceipts(roomId, setOf(/*TODO*/))
                            .mapLatest { receiptsByEvent ->

                                // Discard sorted list since receipts have changed.
                                val receiptsList = getReceiptsList(roomId, true)

                                // Load all the read events that are in the cache.
//                                fetchAllReadEventsFromCache(roomId, receiptsByEvent)

                                receiptsByEvent

                            }
                            .orderingCache(roomId)
                            .stateIn(coroutineScope, WhileSubscribed(), emptyMap())
                    }
                }
            )
        }

    /*
    private fun fetchAllReadEventsFromCache(roomId: RoomId, receiptsByEvent: Map<EventId, ReadReceiptResult>) {
        eventFetchJob.value?.cancel()
        eventFetchJob.value = coroutineScope.launch {
            val receiptsList = getReceiptsList(roomId, false)
            // TODO: filter by most early
            log.debug { "fetching up to ${receiptsByEvent.size} read events from cache" }
            combine(receiptsByEvent.keys.map { eventId ->
//                coroutineScope.async {
                matrixClient.room.getTimelineEvent(roomId, eventId) {
                    decryptionTimeout = ZERO
                    fetchTimeout = ZERO
                    fetchSize = 1
                    allowReplaceContent = false
                }.mapNotNull {
                    log.debug { "===== fetch ${it?.eventId}" }
                    it
                }
//                    .catch {
//                    log.debug { "===== error fetch" }
//                    emit(null)
//                }
//                        .first { it != null }
//                }
            }) {

//            }

//                .awaitAll().let {
                log.debug { "received ${it.size} read events from cache" }
                receiptsList.transaction {
                    it.forEach { event ->
                        val eventId = event.eventId
                        val timestamp = event.originTimestamp
                        val readers = receiptsByEvent[eventId]?.usersByEvent
                        if (!readers.isNullOrEmpty()) {
                            add(OrderedReadEvent(timestamp, eventId, readers))
                        }
                    }
                }
            }
        }
    }
     */

    private val eventFetchJob = MutableStateFlow<Job?>(null)

    private val _receiptsLists = MutableStateFlow<MutableMap<RoomId, ReadEventsOrdering>>(mutableMapOf())


    private fun getReceiptsList(roomId: RoomId, clear: Boolean): ReadEventsOrdering =
        clear.let {
            if (it) _receiptsLists.value.remove(roomId)
            _receiptsLists.value.getOrPut(roomId) { ReadEventsOrdering() }
        }


    /**
     * @return All users with read receipts for the selected room
     * grouped by read receipt event ids and containing the timestamp range
     * while excluding the sender and the current user.
     *
     * Can contain results with empty collections of users. These should be ignored.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getReceipts(
        roomId: RoomId,
        exclude: Set<UserId>,
    ): Flow<Map<EventId, ReadReceiptResult>> =
        matrixClient.user.getAllReceipts(roomId)
            .debounceAfterFirst(10.milliseconds)
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
                                    Read -> if (exclude.contains(userId).not()) {
                                        it.usersByEvent.add(userId)
                                    }
                                }
                            }
                        }
                    }
                }
            }


    init {
//        val order = ReadEventsOrdering()
//        order.transaction {
//            (1..12).forEach {
//                add(OrderedReadEvent(it * 1_000L, EventId("ev-$it"), setOf()))
//            }
//        }
//        order.print()
//        listOf(-1000, 500, 2500, 3000, 999, 1234, 500000).forEach { num ->
//            order.findEncompassing(num.toLong())
//                .let { log.debug { "=== FOUND $num between ${it.first?.timestamp} and ${it.second?.timestamp}" } }
//        }
    }


    private data class ReadReceiptResult(
        var timestampMostEarly: Long? = null, // Cutoff for fetching anything older than the selected event.
        var timestampMostRecent: Long? = null, // Prioritizing for this increases the chances to get the relevant events first.
        var usersByEvent: MutableSet<UserId> = mutableSetOf(),
    )

    private fun Flow<Map<EventId, ReadReceiptResult>>.orderingCache(roomId: RoomId) =
//        channelFlow<List<SortedReadEvent>> {
        channelFlow<ReadReceipts> {
            val scope = this
            val orderedEvents = concurrentOf { ReadEventsOrdering() }
            val updateWrapper = ConcurrentReadEventsOrderingReceipts(orderedEvents, this)
            val receiptsByEventId = MutableStateFlow<Map<EventId, ReadReceiptResult>>(mapOf())

            //            fun onReceiveTimestamp(eventId: EventId, timestamp: Long) {
            suspend fun onReceiveTimestamp(event: TimelineEvent) {
                val eventId = event.eventId
                val readers = receiptsByEventId.value[eventId]?.usersByEvent ?: return
                val timestamp = event.originTimestamp
                val orderable = OrderableReadEvent(timestamp, eventId, readers)
                orderedEvents.write {
                    transaction {
                        if (orderable.readers.isNotEmpty()) {
                            add(orderable)
                            log.debug { "--- added ordered event: ${orderable.eventId} at: ${orderable.timestamp} with ${orderable.readers.size} readers to container of size: ${orderedEvents.value.size}" }
                        }
                    }
                }
            }

            val relevantEvents = MutableSharedFlow<EventId>(replay = 0)
            scope.launch {
                relevantEvents.collect { eventId ->
                    fetchTimelineEvent(roomId, eventId, false)
//                    { receiptsByEventId.value[it]?.usersByEvent }
                        .map {
                            it?.also { event ->
                                log.debug { "============= FETCHED RELEVANT: $eventId -> ${event.originTimestamp}" }
                                onReceiveTimestamp(event)
                            } ?: let { log.debug { "============= FAILED RELEVANT: $eventId" } }
                        }
                        .first()
                }
            }


            scope.launch {
                // State machine loop until everyone unsubscribes.
                while (true) {
                    delay(10.milliseconds) // Skip frames to not lock up the routine.
                }
            }.invokeOnCompletion {
                log.debug { "====== completed state machine loop" }
            }


            val receiptsTimestampRetrievalJob = MutableStateFlow<Job?>(null)
            fun launchReceiptsTimestampRetrieval(receiptEventIds: Set<EventId>) = receiptsTimestampRetrievalJob.apply {
                value?.cancel()
                value = scope.launch {
                    log.debug { "============= launch fetch of up to ${receiptEventIds.size} read events" }
                    val fetchQueue = MutableStateFlow(ArrayDeque(receiptEventIds.map { it to true }))
                    // TODO: sort by most recent and re-fetch from server
                    while (fetchQueue.value.isNotEmpty()) {
                        fetchQueue.value.removeFirstOrNull()?.let { (eventId, fromCache) ->
                            fetchTimelineEvent(roomId, eventId, fromCache)
//                            { receiptsByEventId.value[it]?.usersByEvent }
                                .map {
                                    it?.also { event ->
                                        log.debug { "============= FETCHED RECEIPT: $eventId -> ${event.originTimestamp}" }
                                        // TODO: If timeline event's roomId leads to different room
                                        //  start collecting the according stateflow of this channel flow for
                                        //  that other room and combine the results.
                                        //  But exact case needs to be clarified!
                                        onReceiveTimestamp(event)
                                    } ?: let {
                                        log.debug { "============= FAILED RECEIPT: $eventId" }

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

            collectLatest { receipts ->
                receiptsByEventId.value = receipts

                // Discard sorted list since receipts have changed.
                orderedEvents.write { clear() }

                // Load or re-load all read events from the store cache.
                launchReceiptsTimestampRetrieval(receipts.keys)
            }
        }

    private suspend fun fetchTimelineEvent(
        roomId: RoomId,
        eventId: EventId,
        fromCache: Boolean,
//        readersForEvent: (EventId) -> Set<UserId>?,
    ): Flow<TimelineEvent?> =
        withTimeout(10.seconds) {
            matrixClient.room.getTimelineEvent(roomId, eventId) {
                decryptionTimeout = ZERO
                fetchTimeout = if (fromCache) ZERO else 10.seconds
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
//                        val timestamp = event.originTimestamp
//                        OrderableReadEvent(timestamp, eventId, readersForEvent(eventId) ?: setOf())
                    }
                    null
                }
        }

}

private class ConcurrentReadEventsOrderingReceipts(
    private val receipts: Concurrent<ReadEventsOrdering, ReadEventsOrdering>,
    private val scope: CoroutineScope,
    override val relevantEventsTracker: MutableSharedFlow<EventId>,
) : ReadReceipts {

    override fun get(eventId: EventId): Deferred<SortedReadEvent?> =
        scope.async {
            receipts.read {
                val list = toList()
                list.find { it.eventId == eventId }
            }
        }

    override val fromLatestToEarliest: Deferred<Iterator<SortedReadEvent>>
        get() = scope.async {
            receipts.read {
                val list = toList()
                var index = list.size - 1
                object : Iterator<SortedReadEvent> {
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
) : SortedReadEvent // Inherits from since this is what it is meant to become.
