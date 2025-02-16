package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomUserBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineMock
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.withCleanup
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.failure
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.milliseconds


private val log = KotlinLogging.logger {}

class ReadReceiptsHandleTest : ShouldSpec() {

    private val us = UserId("martin", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        coroutineTestScope = true
        timeout = 5_000

        beforeEach {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns us
        }

        var runId = 0 // Mokkery fails to properly update existing mocks.

        data class TestEnv(
            val roomId: RoomId = RoomId("room_${runId++}", "localhost"),
            val timeline: TimelineMock = timeline(roomServiceMock, roomId) {},
            val roomUsers: RoomUserBuilder = roomUsers(userServiceMock, roomId) {},
            val eventIds: List<EventId> = (0..11).map { EventId("event_${runId}_$it") },
            val us: UserId = this@ReadReceiptsHandleTest.us,
            val alice: UserId = UserId("alice_$runId", "localhost"),
            val bob: UserId = UserId("bob_$runId", "localhost"),
            val reader: List<UserId> = (0..4).map { UserId("reader_${runId}_$it", "localhost") },
            val testScope: TestScope,
        )

//        fun TestEnv.getTestEnv

        fun TestEnv.cutReadReceiptsHandle(
            senderId: UserId,
            eventId: EventId,
        ): ReadReceiptsHandle {
            val di = koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))
                )
            }.koin
            val viewModelContext = testScope.testMatrixClientViewModelContext(
                di = di,
                userId = us,
            )
            return ReadReceiptsHandleImpl(
                eventId = eventId,
                roomId = roomId,
                senderId = senderId,
                initials = Initials,
                client = viewModelContext.matrixClient,
                config = MatrixMessengerConfiguration(),
                cache = ReadReceiptsCacheImpl(
                    client = viewModelContext.matrixClient,
                    scope = viewModelContext.coroutineScope,
                ),
                scope = viewModelContext.coroutineScope,
            )
        }

        should("be false when no on read or sent a message other than the sender").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[0])
            }
            cut shouldBeRead false
        }

        should("be false when not read by anyone but us and we sent a message after it").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.us, event[1])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[0])
                +roomUser("Martin", env.us, event[1])
            }
            cut shouldBeRead false
        }

        should("be false when not read by anyone and only the same user sent a message after it").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.alice, event[1])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[1])
            }
            cut shouldBeRead false
        }

        should("be false when only we read the event").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Martin", env.us, event[0])
            }
            cut shouldBeRead false
        }

        should("be false when only the sender read the event").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[0])
            }
            cut shouldBeRead false
        }

        should("be true when read by third user").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            val updateCount by launchAndCollectCut(
                cut.isRead,
                2,
            ) { result, updateCount ->
                when (updateCount) {
                    1 -> result shouldBe false
                    2 -> result shouldBe true
                }
            }
            wait()
            updateCount shouldBe 2
        }

        should("be true when message from third user after it").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, event[0])
            }
            timeline.addEvents {
                // Should be ignored.
                +timelineEventOf(env.alice, event[1])
                +timelineEventOf(env.bob, event[2])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            val updateCount by launchAndCollectCut(
                cut.isRead,
                2,
            ) { result, updateCount ->
                when (updateCount) {
                    1 -> result shouldBe false
                    2 -> result shouldBe true
                }
            }
            wait()
            updateCount shouldBe 2
        }

        should("be empty when not read").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, null)
            }
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            cut shouldBeUsers emptySet()
        }

        should("contain users from read markers").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, null)
            }
            cut shouldBeUsers emptySet()
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            cut shouldBeUsers setOf(env.bob)
        }

        should("contain sender of subsequent events").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            timeline.addEvents {
                +timelineEventOf(env.bob, event[1])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            cut shouldBeUsers setOf(env.bob)
        }

        should("not contain us from read marker").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, event[0])
                +roomUser("Bob", env.bob, event[0])
            }
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            cut shouldBeUsers setOf(env.bob)
        }

        should("not contain us from subsequent events").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.us, event[1])
            }
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            cut shouldBeUsers emptySet()
        }

        should("not contain sender from subsequent events").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, _, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.alice, event[1])
            }
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            cut shouldBeUsers emptySet()
        }

        should("not contain sender from read marker").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
                +roomUser("Alice", env.alice, event[0])
            }
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            cut shouldBeUsers setOf(env.bob)
        }

        should("not contain sender from read marker after update").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val updateCount by launchAndCollectCut(
                env.cutReadReceiptsHandle(env.alice, event[0]).isReadBy,
            ) { result, updateCount ->
                when (updateCount) {
                    1 -> result shouldBeUsers emptySet()
                    2 -> result shouldBeUsers setOf(env.bob)
                }
            }
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, null)
            }
            wait()
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
                +roomUser("Alice", env.alice, event[0])
            }
            wait()
            updateCount shouldBeGreaterThanOrEqual 2
        }

        should("update read marker").withCleanup {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutReadReceiptsHandle(env.alice, event[0])
            timeline.addEvents {
                (0..11).forEach { i ->
                    +timelineEventOf(env.alice, event[i])
                }
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Reader", env.reader[0], event[1])
                +roomUser("Reader", env.reader[1], event[4])
                +roomUser("Reader", env.reader[2], event[8])
                +roomUser("Reader", env.reader[3], event[11])
            }
            val updateCount by launchAndCollectCut(
                cut.isReadBy,
                5,
            ) { result, updateCount ->
                when (updateCount) {
                    1 -> result shouldBeUsers emptySet()

                    2 -> result shouldBeUsers setOf(
                        env.reader[0],
                    )

                    3 -> result shouldBeUsers setOf(
                        env.reader[0],
                        env.reader[1],
                    )

                    4 -> result shouldBeUsers setOf(
                        env.reader[0],
                        env.reader[1],
                        env.reader[2],
                    )

                    5 -> result shouldBeUsers setOf(
                        env.reader[0],
                        env.reader[1],
                        env.reader[2],
                        env.reader[3],
                    )
                }
            }
            wait()
            updateCount shouldBe 5
        }
    }

    // TODO move to test utils?
    private fun <T> CoroutineScope.launchAndCollectCut(
        cut: Flow<T>,
        maxUpdates: Int? = null,
        onCollect: suspend (result: T, updateCount: Int) -> Unit,
    ): MutableState1<Int> {
        val updateCount = MutableState1(0)
        launch {
            cut.collect { result ->
                updateCount.state.value++
                if (maxUpdates != null && maxUpdates < updateCount.state.value) {
                    throw failure("should not have updated >=$updateCount times")
                }
                log.debug { "update #${updateCount.state.value} -> $result" }
                onCollect(result, updateCount.state.value)
            }
        }
        return updateCount
    }

    private fun TimelineBuilder.timelineEventOf(
        senderId: UserId,
        eventId: EventId? = null,
    ): TimelineEvent = TimelineEvent(
        event = messageEvent(
            sender = senderId,
            eventId = eventId,
        ) { text("Hi!") },
        content = null,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private suspend inline infix fun ReadReceiptsHandle.shouldBeUsers(expectedUsers: Set<UserId>) {
        this.isReadBy shouldBeUsers expectedUsers
    }

    private suspend inline infix fun Flow<Set<ReadReceiptsHandle.Reader>>.shouldBeUsers(expectedUsers: Set<UserId>) {
        eventually(eventuallyConfig {
            retries = 10
            this.listener = { _, _ -> delay(100.milliseconds) }
        }) {
            this.first() shouldBeUsers expectedUsers
        }
    }

    private suspend inline infix fun Set<ReadReceiptsHandle.Reader>.shouldBeUsers(expectedUsers: Set<UserId>) {
        withClue("unexpected result for reader receipts!") {
            this.map { it.userId }.toSet() shouldBe expectedUsers
//            this.forEach {
//                eventually(eventuallyConfig {
//                    retries = 10
//                    this.listener = { _, _ -> delay(100.milliseconds) }
//                }) {
//                    it.userInfo.first() shouldBe it.userId
//                }
//            }
//            this.map { (key, flow) ->
//                runBlocking {
//                    val roomUser = flow.first()!!
//                    roomUser.userId shouldBe key
//                }
//            }
        }
    }

    private suspend inline infix fun ReadReceiptsHandle.shouldBeRead(isRead: Boolean) {
        withClue("unexpected result for read indicator!") {
            eventually(eventuallyConfig {
                retries = 10
                this.listener = { _, _ -> delay(100.milliseconds) }
            }) {
                this.isRead.first() shouldBe isRead
            }
        }
    }
}

// TODO: move to utils?
class MutableState1<T>(value: T) {
    val state = MutableStateFlow(value)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return state.value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
    }
}

private suspend fun wait() = delay(500.milliseconds)
