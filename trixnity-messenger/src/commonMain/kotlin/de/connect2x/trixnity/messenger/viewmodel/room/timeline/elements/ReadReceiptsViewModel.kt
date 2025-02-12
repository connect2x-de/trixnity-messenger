package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReadReceiptsManager.ReadReceipts
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReadReceiptsViewModel.ReadEvent
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.debounceAfterFirst
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import net.folivo.trixnity.utils.concurrentMutableMap
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

interface ReadReceiptsManager {
    suspend fun getReadReceipts(
        client: MatrixClient,
        eventId: EventId,
        roomId: RoomId,
        filter: Set<UserId>,
        coroutineScope: CoroutineScope,
    ): ReadReceipts

    interface ReadReceipts {
        val eventId: EventId
        val roomId: RoomId
        val filtered: Set<UserId>
        val isRead: StateFlow<Boolean>
        val readReceiptsCumulative: StateFlow<Set<UserId>>
        val readReceiptsSingle: StateFlow<Set<UserId>>
    }
}

class ReadReceiptsManagerImpl(
//    private val matrixClient: MatrixClient,
    private val initials: Initials,
) : ReadReceiptsManager {
    override suspend fun getReadReceipts(
        client: MatrixClient,
        eventId: EventId,
        roomId: RoomId,
        filter: Set<UserId>,
        coroutineScope: CoroutineScope
    ): ReadReceipts = coroutineScope {


        val a = getCachedReceipts(roomId, client, coroutineScope)
            .stateIn(coroutineScope, WhileSubscribed(), 0)


        TODO("Not yet implemented")
    }

    val off = MutableStateFlow<EventId?>(null)

    private val receiptsByEventCache = concurrentMutableMap<RoomId, StateFlow<ReadReceipts>>()
    private fun getCachedReceipts(
        eventId: EventId,
        roomId: RoomId,
        client: MatrixClient,
        scope: CoroutineScope,
    ): Flow<ReadReceipts> =
        flow {
            emitAll(receiptsByEventCache.read { get(roomId) }
                ?: receiptsByEventCache.write {
                    getOrPut(roomId) {
                        receiptsFlow(eventId, roomId, client, scope)
                            // TODO: could be dangerous to give out state flows from potentially different and cancelled coroutines
                            // TODO: maybe use on completion to clear the cache from dead entries
                            .stateIn(scope, WhileSubscribed(), ReadReceiptsImpl(eventId, roomId, setOf(), scope, off))
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
    ): Flow<ReadReceipts> =
        client.user.getAllReceipts(roomId)
            .debounceAfterFirst(10.milliseconds)
            .distinctUntilChanged()
            .flattenNotNull()
            // TODO: channelflow using "off"
            .mapLatest { userReceipts ->
                ReadReceiptsImpl(eventId, roomId, setOf(), scope, off)
            }
}

class ReadReceiptsImpl(
    override val eventId: EventId,
    override val roomId: RoomId,
    override val filtered: Set<UserId>,
    scope: CoroutineScope,
    private val offsetter: MutableStateFlow<EventId?>,
) : ReadReceipts {

    override val isRead: StateFlow<Boolean> =
        flowOf(true)
            .stateIn(scope, WhileSubscribed(), false)

    override val readReceiptsCumulative: StateFlow<Set<UserId>> =
        flowOf(setOf<UserId>())
            .stateIn(scope, WhileSubscribed(), setOf())

    override val readReceiptsSingle: StateFlow<Set<UserId>> =
        flowOf(setOf<UserId>())
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
        channelFlow<Map<EventId, ReadReceiptResult>> {
            val orderedEvents = MutableStateFlow(ReadEventsOrdering())
            this.launch {
                // State machine loop until everyone unsubscribes.
                while (true) {
                    delay(10.milliseconds) // Skip frames to not lock up the routine.
                }
            }.invokeOnCompletion {
                log.debug { "--- completed state machine loop" }
            }
            collectLatest { receiptsByEventId ->
                orderedEvents.value.clear()
                send(receiptsByEventId)
                this.launch {
                    log.debug { "--- launch fetch of up to ${receiptsByEventId.size} read events" }
                    receiptsByEventId.forEach { (eventId, receipt) ->
                        withTimeout(1.seconds) {
                            matrixClient.room.getTimelineEvent(roomId, eventId) {
                                decryptionTimeout = ZERO
                                fetchTimeout = ZERO
                                fetchSize = 1
                                allowReplaceContent = false
                            }
                        }.first()?.let { event ->
                            orderedEvents.value.transaction {
                                val timestamp = event.originTimestamp
                                val readers = receiptsByEventId[event.eventId]?.usersByEvent
                                if (!readers.isNullOrEmpty()) {
                                    add(OrderedReadEvent(timestamp, event.eventId, readers))
                                    log.debug { "--- added ordered event: ${event.eventId} at: $timestamp with ${readers.size} readers to container of size: ${orderedEvents.value.size}" }
                                }
                            }
//                            orderedEvents.value.print()
                        }
                    }
                }.invokeOnCompletion {
                    log.debug { "--- completed fetch loop" }
                }
            }
        }

    private class ReadEventsOrdering {
        //    private val _events = MutableStateFlow(mutableListOf<OrderedReadEvent>())
        private var _events = mutableListOf<OrderedReadEvent>()

        operator fun get(index: Int) = _events[index]

        fun transaction(transaction: AddTransaction.() -> Unit) {
            transaction(object : AddTransaction {
                override fun add(event: OrderedReadEvent) {
                    _events.add(event)
                }
            })
            _events = _events
                .distinctBy { it.eventId }
                .sortedBy { it.timestamp }
                .toMutableList()
        }

        interface AddTransaction {
            fun add(event: OrderedReadEvent)
        }

        val size get() = _events.size

        fun clear() {
            _events = mutableListOf()
        }

        fun print() {
            _events.forEachIndexed { i, v -> log.debug { "--- #$i \t ${v.timestamp}" } }
        }

        fun findEncompassing(timestamp: Long): Pair<OrderedReadEvent?, OrderedReadEvent?> {
            var past: OrderedReadEvent? = null
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

    private data class OrderedReadEvent(
        val timestamp: Long,
        val eventId: EventId,
        val readers: Set<UserId>,
        var gapsToNext: Boolean = false,
        var gapsToPast: Boolean = false,
    )
}
