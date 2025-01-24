package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomUserBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineMock
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.engine.runBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.seconds


class getMessageReadReceiptsTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val us = UserId("martin", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        coroutineTestScope = true

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
            val eventIds: List<EventId> = (0..2).map { EventId("event_${runId}_$it") },
            val us: UserId = this@getMessageReadReceiptsTest.us,
            val alice: UserId = UserId("alice_$runId", "localhost"),
            val bob: UserId = UserId("bob_$runId", "localhost"),
            val testScope: TestScope,
        )

        fun TestEnv.cutMessageIsRead(
            senderId: UserId,
            eventId: EventId,
        ): Flow<Boolean?> = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock)))
        }.koin.let { di ->
            getMessageIsRead(
                client = testScope.testMatrixClientViewModelContext(
                    di = di,
                    userId = us,
                ).matrixClient,
                senderId = senderId,
                roomId = roomId,
                eventId = eventId,
            )
        }

        fun TestEnv.cutMessageReadReceipts(
            senderId: UserId,
            eventId: EventId,
        ): Flow<Map<UserId, Flow<RoomUser?>>> = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock)))
        }.koin.let { di ->
            getMessageReadReceipts(
                client = testScope.testMatrixClientViewModelContext(
                    di = di,
                    userId = us,
                ).matrixClient,
                senderId = senderId,
                roomId = roomId,
                eventId = eventId,
            )
        }

        should("be false when no on read or sent a message other than the sender") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[0])
            }
            cut shouldBeRead false
            cancelNeverEndingCoroutines()
        }

        should("be false when not read by anyone but us and we sent a message after it") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.us, event[1])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[0])
                +roomUser("Martin", env.us, event[1])
            }
            cut shouldBeRead false
            cancelNeverEndingCoroutines()
        }

        should("be false when not read by anyone and only the same user sent a message after it") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.alice, event[1])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[1])
            }
            cut shouldBeRead false
            cancelNeverEndingCoroutines()
        }

        should("be false when only we read the event") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            // receipts.value = mapOf(eventIdByAlice to setOf(us))
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Martin", env.us, event[0])
            }
            cut shouldBeRead false
            cancelNeverEndingCoroutines()
        }

        should("be false when only the sender read the event") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            // receipts.value = mapOf(eventIdByAlice to setOf(alice))
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Alice", env.alice, event[0])
            }
            cut shouldBeRead false
            cancelNeverEndingCoroutines()
        }

        should("be true when read by third user") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, event[0])
            }
            cut shouldBeRead false
            // receipts.value = mapOf(eventIdByAlice to setOf(bob))
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            cut shouldBeRead true
            cancelNeverEndingCoroutines()
        }

        should("be true when message from third user after it") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageIsRead(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, event[0])
            }
            cut shouldBeRead false
            timeline.addEvents {
                // Should be ignored.
                +timelineEventOf(env.alice, event[1])
                +timelineEventOf(env.bob, event[2])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            cut shouldBeRead true
            cancelNeverEndingCoroutines()
        }

        should("be empty when not read") {
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
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            cut shouldBeUsers emptySet()
            cancelNeverEndingCoroutines()
        }

        should("contain users from read markers") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, null)
                +roomUser("Bob", env.bob, null)
                +roomUser("Alice", env.alice, null)
            }
            cut shouldBeUsers emptySet()
            // receipts.value = mapOf(eventIdByAlice to setOf(bob))
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            cut shouldBeUsers setOf(env.bob)
            cancelNeverEndingCoroutines()
        }

        should("contain sender of subsequent events") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            // cut shouldBeUsers emptySet()
            // cut shouldBeUsers setOf(bob)
            timeline.addEvents {
                +timelineEventOf(env.bob, event[1])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
            }
            cut shouldBeUsers setOf(env.bob)
            cancelNeverEndingCoroutines()
        }

        should("not contain us from read marker") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Us", env.us, event[0])
                +roomUser("Bob", env.bob, event[0])
            }
            // receipts.value = mapOf(eventIdByAlice to setOf(us, bob))
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            cut shouldBeUsers setOf(env.bob)
            cancelNeverEndingCoroutines()
        }

        should("not contain us from subsequent events") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.us, event[1])
            }
            // roomUsers(userServiceMock, roomId) {}
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            cut shouldBeUsers emptySet()
            cancelNeverEndingCoroutines()
        }

        should("not contain sender from subsequent events") {
            val env = TestEnv(testScope = this)
            val (_, timeline, _, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
                +timelineEventOf(env.alice, event[1])
            }
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            cut shouldBeUsers emptySet()
            cancelNeverEndingCoroutines()
        }

        should("not contain sender from read marker") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            timeline.addEvents {
                +timelineEventOf(env.alice, event[0])
            }
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
                +roomUser("Alice", env.alice, event[0])
            }
            val cut = env.cutMessageReadReceipts(env.alice, event[0])
            cut shouldBeUsers setOf(env.bob)
            cancelNeverEndingCoroutines()
        }

        should("not contain sender from read marker 2") {
            val env = TestEnv(testScope = this)
            val (_, timeline, roomUsers, event) = env
            val updateCount by launchAndObserveCut(
                env.cutMessageReadReceipts(env.alice, event[0]),
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
            advanceUntilIdle()
            roomUsers.addOrUpdateUsers {
                +roomUser("Bob", env.bob, event[0])
                +roomUser("Alice", env.alice, event[0])
            }
            advanceUntilIdle()
            eventually(1.seconds) { updateCount shouldBe 2 }
            cancelNeverEndingCoroutines()
        }

        // TODO: maybe add some more better tests for read receipts
    }

    private fun <T> CoroutineScope.launchAndObserveCut(
        cut: Flow<T>,
        onCollect: (result: T, updateCount: Int) -> Unit,
    ): MutableState<Int> {
        val updateCount = MutableState(0)
        launch {
            cut.collect { result ->
                updateCount.state.value++
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

    private suspend inline infix fun Flow<Map<UserId, Flow<RoomUser?>>>.shouldBeUsers(expectedUsers: Set<UserId>) {
        this.first() shouldBeUsers expectedUsers
    }

    private inline infix fun Map<UserId, Flow<RoomUser?>>.shouldBeUsers(expectedUsers: Set<UserId>) {
        withClue("unexpected result for reader receipts!") {
            this.keys shouldBe expectedUsers
            this.map { (key, flow) ->
                runBlocking {
                    val roomUser = flow.first()!!
                    roomUser.userId shouldBe key
                }
            }
        }
    }

    private suspend inline infix fun Flow<Boolean?>.shouldBeRead(isRead: Boolean) {
        withClue("unexpected result for read indicator!") {
            this.first()!! shouldBe isRead
        }
    }
}

// TODO: move to utils?
class MutableState<T>(value: T) {
    val state = MutableStateFlow(value)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return state.value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
    }
}
