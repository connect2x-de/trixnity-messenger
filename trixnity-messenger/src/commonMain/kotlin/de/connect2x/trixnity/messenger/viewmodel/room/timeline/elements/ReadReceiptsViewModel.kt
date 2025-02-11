package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.debounceAfterFirst
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
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
import kotlin.math.sign
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds


private val log = KotlinLogging.logger {}

interface ReadReceiptsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
//        key: String,
//        timelineEventFlow: Flow<TimelineEvent>,
//        roomId: RoomId,
    ): ReadReceiptsViewModel =
        ReadReceiptsViewModelImpl(
            viewModelContext = viewModelContext,
//            roomId = roomId,
        )

    companion object : ReadReceiptsViewModelFactory
}

interface ReadReceiptsViewModel {
//    val roomId: RoomId
//    val eventId: EventId


//    val isRead: StateFlow<Boolean?>
//    val reactions: StateFlow<Map<ReactionKey, Set<ReactionEvent>>>

    fun readReceipts(roomId: RoomId, eventId: EventId): StateFlow<Map<UserId, Flow<UserInfoElement?>>>
    fun isRead(roomId: RoomId, eventId: EventId): StateFlow<Boolean>

//    data class ReactionEvent(
//        val eventId: EventId,
//        val senderFlow: StateFlow<UserInfoElement?>,
//        val isByMe: Boolean,
//    )
}

//@OptIn(ExperimentalCoroutinesApi::class)
class ReadReceiptsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
//    override val roomId: RoomId,
) : ReadReceiptsViewModel, MatrixClientViewModelContext by viewModelContext {


//    private val receipts = getReceipts()
//        .state


    override fun readReceipts(roomId: RoomId, eventId: EventId): StateFlow<Map<UserId, Flow<UserInfoElement?>>> {
        TODO("Not yet implemented")
    }

    override fun isRead(roomId: RoomId, eventId: EventId): StateFlow<Boolean> {
        TODO("Not yet implemented")
    }

    private val receiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, ReadReceiptResult>>>()

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
                                // TODO: filter by most early
                                receiptsByEvent.keys.map { eventId ->
                                    coroutineScope.async {
                                        matrixClient.room.getTimelineEvent(roomId, eventId) {
                                            decryptionTimeout = ZERO
                                            fetchTimeout = ZERO
                                            fetchSize = 1
                                            allowReplaceContent = false
                                        }.first { it != null }
                                    }
                                }.awaitAll().forEach {
                                    it?.let { event ->
                                        val eventId = event.eventId
                                        val timestamp = event.originTimestamp
                                        val readers = receiptsByEvent[eventId]?.usersByEvent
                                        if (!readers.isNullOrEmpty()) {
                                            receiptsList.value.add(ReadEvent(timestamp, eventId, readers))
                                        }
                                    }
                                }

                                receiptsByEvent
                            }
                            .stateIn(coroutineScope, WhileSubscribed(), emptyMap())
                    }
                }
            )
        }

    data class ReadEvent(val timestamp: Long, val eventId: EventId, val readers: Set<UserId>)

    //    private val _receiptsLists = concurrentMutableMap<RoomId, SortedLinkedList<ReadEvent>>()
    private val _receiptsLists =
        MutableStateFlow<MutableMap<RoomId, MutableStateFlow<SortedLinkedList<ReadEvent>>>>(mutableMapOf())

    //        concurrentMutableMap<RoomId, SortedLinkedList<ReadEvent>>()
    private fun getReceiptsList(roomId: RoomId, clear: Boolean): MutableStateFlow<SortedLinkedList<ReadEvent>> =
        clear.let {
            if (it) _receiptsLists.value.remove(roomId)
            _receiptsLists.value.getOrPut(roomId) {
                MutableStateFlow(SortedLinkedList { a, b ->
                    (a.timestamp - b.timestamp).sign
                })
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
                /*
                // TODO: do all in one mutable map
                val mostEarly = mutableMapOf<EventId, Long>()
                val mostRecent = mutableMapOf<EventId, Long>()
                val usersByEvent = buildMap<EventId, MutableSet<UserId>> {
                    userReceipts.entries.forEach { (userId, receipt) ->
                        receipt.receipts.entries.forEach { (type, receipt) ->
                            val time = receipt.receipt.timestamp
                            val eventId = receipt.eventId
                            mostEarly[eventId]
                                ?.let { mostEarly[eventId] = min(it, time) }
                                ?: let { mostEarly[eventId] = time }
                            mostRecent[eventId]
                                ?.let { mostRecent[eventId] = max(it, time) }
                                ?: let { mostRecent[eventId] = time }
                            when (type) {
                                Read -> if (exclude.contains(userId).not()) {
                                    getOrPut(eventId) { mutableSetOf() }
                                        .apply { add(userId) }
                                }
                            }
                        }
                    }
                }
                usersByEvent.mapValues { (eventId, users) ->
                    ReadReceiptResult(
                        timestampMostEarly = mostEarly[eventId] ?: 0L,
                        timestampMostRecent = mostRecent[eventId] ?: 0L,
                        usersByEvent = users,
                    )
                }
                 */
            }

    private data class ReadReceiptResult(
        var timestampMostEarly: Long? = null,
        var timestampMostRecent: Long? = null,
        var usersByEvent: MutableSet<UserId> = mutableSetOf(),
    )

    init {
        log.debug { "===== READ RECEIPTS" }
        val s = SortedLinkedList<Int> { a, b ->
            (a - b).sign
        }
        (1..12).shuffled()
//        listOf(2, 4, 3, 9, 8, 7, 5, 10, 6, 1)
            .also {
                log.debug { "===== TEST DATA: ${it}" }
            }
            .forEach {
                log.debug { "===== TEST ADD: $it CURR: ${s.toSortedList()}" }
                s.add(it)
            }
        log.debug { "===== TEST SLL: ${s.toSortedList()}" }
        s.first.let { while (it.hasNext()) log.debug { "--- FROM START: ${it.next()}" } }
        s.last.let { while (it.hasNext()) log.debug { "--- FROM END: ${it.next()}" } }
    }

    private class ReadEventsOrdering : MutableList<ReadEvent> by mutableListOf() {
        fun add(transaction: AddTransaction.() -> Unit) {
            transaction(object : AddTransaction {
                override fun add(event: ReadEvent) {
                    this.add(event)
                }
            })
            this.sortBy { it.timestamp }
        }

        interface AddTransaction {
            fun add(event: ReadEvent)
        }
    }
}

private class SortedLinkedList<T>(
    private val sortBy: Comparator<T>,
) {
    private var _size: Int = 0
    val size: Int get() = _size

    private var _nodes: MutableList<Node<T>> = mutableListOf()

    private var _first: Node<T>? = null
    val first: Iterator<T>
        get() = object : Iterator<T> {
            private var _next: Node<T>? = _first
            override fun hasNext(): Boolean = _next != null
            override fun next(): T {
                val current = _next ?: throw NoSuchElementException()
                return current.value.let {
                    _next = current.next
                    it
                }
            }
        }
    private var _last: Node<T>? = null
    val last: Iterator<T>
        get() = object : Iterator<T> {
            private var _past: Node<T>? = _last
            override fun hasNext(): Boolean = _past != null
            override fun next(): T {
                val current = _past ?: throw NoSuchElementException()
                return current.value.let {
                    _past = current.past
                    it
                }
            }
        }

    fun add(value: T) {
        _add(value)
//        _nodes = toSortedList()
    }

    private fun _add(value: T) {
        val node = Node(value)
        val first = _first
        _size++
        if (first == null) {
            _first = node
            _last = _first
            return
        }
        var iter: Node<T>? = first
        var last: Node<T> = first
        while (iter != null) {
            if (sortBy.compare(value, iter.value) <= 0) {
                val past = iter.past
                iter.past = node
                node.next = iter
                if (past != null) {
                    past.next = node
                    node.past = past
                } else {
                    _first = node
                }
                return
            }
            last = iter
            iter = iter.next
        }
        last.next = node
        node.past = last
        _last = node
    }

    // No remove function since it's currently not needed.

    fun toSortedList(): List<T> =
        ArrayList<T>(_size).also { result ->
            first.let { while (it.hasNext()) result.add(it.next()) }
        }

    private data class Node<T>(
        val value: T,
        var next: Node<T>? = null,
        var past: Node<T>? = null,
    )
}

