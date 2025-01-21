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
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.engine.runBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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


private val log = KotlinLogging.logger {}

class getMessageReadReceiptsTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val us = UserId("martin", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        // Ideally is should run with
        // coroutineTestScope = true
        // but currently that makes
        // it fail in the test suit run.

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


//            every { userServiceMock.canSendEvent(roomId, any()) } returns flowOf(true)
//            every { userServiceMock.canSendEvent(roomId, any()) } ru
//            every { userServiceMock.getAllReceipts(roomId) } calls {
//                flowOf(mapOf())
//            }
//            every { userServiceMock.getAll(roomId) } calls {
//                flowOf(mapOf())
//            }

//            every { userServiceMock.getAllReceipts(roomId) } calls {
//                val receipts = readReceipts.value
//                flowOf(receipts)
//            }

//            returns users.map {
//                it.associate { (_, receipts) ->
//                    receipts.userId to flowOf(
//                        receipts
//                    )
//                }
//            }
//            every { userServiceMock.getById(roomId, any()) } calls { params ->
//                val userId = params.args[1] as UserId
//                flowOf(
//                    RoomUser(
//                        roomId, userId, userId.full, ClientEvent.RoomEvent.StateEvent(
//                            MemberEventContent(membership = Membership.JOIN),
//                            id = eventId,
//                            sender = userId,
//                            roomId = roomId,
//                            originTimestamp = 0L,
//                            stateKey = userId.full,
//                        )
//                    )
//                )
//            }
//            every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
//            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns emptyFlow()

            // TODO: filter by startAt?
//            every { roomServiceMock.getTimelineEvents(eq(roomId), any(), any()) } returns flow {
//                emitAll(flowOf( timelineEvents.value.map{ flowOf(it) }))
//                timelineEvents.value.forEach { emit(flowOf(it)) }
//                emit()
//            }
        }

        // Because Mokkery is a bitch and doesn't reinitialize mocks correctly
        // we need to differentiate the parameters by each test case.
        var runId = 0

        data class TestEnv(
            // Increment runId during the first val field assignment!
            val roomId: RoomId = RoomId("room_${runId++}", "localhost"),
            val timeline: TimelineMock = timeline(roomServiceMock, roomId) {},
            val roomUsers: RoomUserBuilder = roomUsers(userServiceMock, roomId) {},
            val eventIds: List<EventId> = (0..2).map { EventId("event_${runId}_$it") },
            val us: UserId = this@getMessageReadReceiptsTest.us, // Stays constant.
            val alice: UserId = UserId("alice_$runId", "localhost"),
            val bob: UserId = UserId("bob_$runId", "localhost"),
            val testScope: TestScope, // Place this last so it so param deconstruct is cleaner.
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
                senderUserId = senderId,
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
                senderUserId = senderId,
                roomId = roomId,
                eventId = eventId,
            )
        }

        context("message is read") {

//            if (false) should("isRead: be false when no on read or sent a message") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                cancelNeverEndingCoroutines()
//            }
            should("be false when no on read or sent a message") {
                val roomId = RoomId("room14", "localhost")
                val alice = UserId("alice14", "localhost")
                val eventId = EventId("ref1")
                val cut = cutMessageIsRead(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                roomUsers(userServiceMock, roomId) {}
                cut shouldBeRead false
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isRead: be false when not read by anyone and only us send message after it") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                    +messageEvent(sender = us) {
//                        text("Hi!")
//                    }
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                cancelNeverEndingCoroutines()
//            }
            should("be false when not read by anyone and only us send message after it") {
                val roomId = RoomId("room13", "localhost")
                val alice = UserId("alice13", "localhost")
                val eventId1 = EventId("ref1")
                val eventId2 = EventId("ref2")
                val cut = cutMessageIsRead(alice, eventId1, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId1)
                    +timelineEventOf(us, eventId2)
                }
                roomUsers(userServiceMock, roomId) {}
                cut shouldBeRead false
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isRead: be false when not read by anyone and only the same user send message after it") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                    +messageEvent(sender = alice) {
//                        text("Hi!")
//                    }
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                cancelNeverEndingCoroutines()
//            }
            should("be false when not read by anyone and only the same user send message after it") {
                val roomId = RoomId("room12", "localhost")
                val alice = UserId("alice12", "localhost")
                val eventId = EventId("ref1")
                val cut = cutMessageIsRead(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                    +timelineEventOf(alice)
                }
                roomUsers(userServiceMock, roomId) {}
                cut shouldBeRead false
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isRead: be false when only we read the event") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                receipts.value = mapOf(eventIdByAlice to setOf(us))
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                cancelNeverEndingCoroutines()
//            }
            should("be false when only we read the event") {
                val roomId = RoomId("room11", "localhost")
                val alice = UserId("alice11", "localhost")
                val eventId = EventId("ref1")
                val cut = cutMessageIsRead(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                roomUsers(userServiceMock, roomId) {
                    +roomUser("Martin", us, eventId)
                }
                cut shouldBeRead false
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isRead: be false when only the sender read the event") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                receipts.value = mapOf(eventIdByAlice to setOf(alice))
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                cancelNeverEndingCoroutines()
//            }
            should("be false when only the sender read the event") {
                val eventId = EventId("ref1")
                val roomId = RoomId("room10", "localhost")
                val alice = UserId("alice10", "localhost")
                val bob = UserId("bob10", "localhost")
                val cut = cutMessageIsRead(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                roomUsers(userServiceMock, roomId) {
                    +roomUser("Alice", alice, eventId)
                }
                cut shouldBeRead false
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isRead: be true when read by third user") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                receipts.value = mapOf(eventIdByAlice to setOf(bob))
//                advanceUntilIdle()
//                cut.isRead.value shouldBe true
//
//                cancelNeverEndingCoroutines()
//            }
            should("be true when read by third user") {
                val roomId = RoomId("room9", "localhost")
                val alice = UserId("alice9", "localhost")
                val bob = UserId("bob9", "localhost")
                val eventId = EventId("ref1")
                val cut = cutMessageIsRead(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                val roomUsers = roomUsers(userServiceMock, roomId) {
                    +roomUser("Us", us, null)
                    +roomUser("Bob", bob, null)
                    +roomUser("Alice", alice, eventId)
                }
                cut shouldBeRead false
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, eventId)
                }
                cut shouldBeRead true
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isRead: be true when message from third user after it") {
//                val timeline = timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isRead.collect() }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe false
//
//                timeline.addEvents {
//                    // should be ignored
//                    +messageEvent(sender = alice) {
//                        text("Hi!")
//                    }
//                    +messageEvent(sender = bob) {
//                        text("Hi!")
//                    }
//                }
//                advanceUntilIdle()
//                cut.isRead.value shouldBe true
//
//                cancelNeverEndingCoroutines()
//            }
            should("be true when message from third user after it") {
                val roomId = RoomId("room8", "localhost")
                val alice = UserId("alice8", "localhost")
                val bob = UserId("bob8", "localhost")
                val eventId = EventId("ref1")
                val cut = cutMessageIsRead(alice, eventId, roomId)
                val timeline = timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                val roomUsers = roomUsers(userServiceMock, roomId) {
                    +roomUser("Us", us, null)
                    +roomUser("Bob", bob, null)
                    +roomUser("Alice", alice, eventId)
                }
                cut shouldBeRead false
                timeline.addEvents {
                    // Should be ignored.
                    +timelineEventOf(alice)
                    +timelineEventOf(bob)
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, eventId)
                }
                cut shouldBeRead true
                cancelNeverEndingCoroutines()
            }
        }

        context("message read receipts:") {

//            if (false) should("isReadBy: be empty when not read") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                advanceUntilIdle()
//                cut.isReadBy.value shouldBe emptyList()
//
//                cancelNeverEndingCoroutines()
//            }
            should("be empty when not read") {
                val roomId = RoomId("room2", "localhost")
                val alice = UserId("alice2", "localhost")
                val bob = UserId("bob2", "localhost")
                val eventId = EventId("ref10")
                val cut = cutMessageReadReceipts(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                roomUsers(userServiceMock, roomId) {
                    +roomUser("Us", us, null, "TEST-2-1")
                    +roomUser("Bob", bob, null, "TEST-2-2")
                    +roomUser("Alice", alice, null, "TEST-2-3")
                }
                cut shouldBeUsers emptySet()
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isReadBy: contain users from read markers") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                advanceUntilIdle()
//                cut.isReadBy.value shouldBe emptyList()
//
//                receipts.value = mapOf(eventIdByAlice to setOf(bob))
//                advanceUntilIdle()
//                cut.isReadBy.value?.map { it.userId } shouldBe listOf(bob)
//
//                cancelNeverEndingCoroutines()
//            }
            should("contain users from read markers") {
                val roomId = RoomId("room1", "localhost")
                val alice = UserId("alice1", "localhost")
                val bob = UserId("bob1", "localhost")
                val eventId = EventId("ref12")
                val cut = cutMessageReadReceipts(alice, eventId, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId)
                }
                val roomUsers = roomUsers(userServiceMock, roomId) {
                    +roomUser("Us", us, null, "TEST-1-1")
                    +roomUser("Bob", bob, null, "TEST-1-2")
                    +roomUser("Alice", alice, null, "TEST-1-3")
                }
                cut shouldBeUsers emptySet()
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, eventId, "TEST-1-4")
                }
                cut shouldBeUsers setOf(bob)
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isReadBy: contain sender of subsequent events") {
//                val timeline = timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                advanceUntilIdle()
//                cut.isReadBy.value shouldBe emptyList()
//
//                timeline.addEvents {
//                    +messageEvent(sender = bob) {
//                        text("Hi!")
//                    }
//                }
//                advanceUntilIdle()
//                cut.isReadBy.value?.map { it.userId } shouldBe listOf(bob)
//
//                cancelNeverEndingCoroutines()
//            }
            should("contain sender of subsequent events") {
                val roomId = RoomId("room3", "localhost")
                val alice = UserId("alice3", "localhost")
                val bob = UserId("bob3", "localhost")
                val eventId1 = EventId("1")
                val eventId2 = EventId("2")
                val cut = cutMessageReadReceipts(alice, eventId1, roomId)
                val timeline = timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId1)
                }
                val roomUsers = roomUsers(userServiceMock, roomId) {
//                    +roomUser("Bob", bob, eventId1)
                }
//                cut shouldBeUsers emptySet()
//                cut shouldBeUsers setOf(bob)
                timeline.addEvents {
                    +timelineEventOf(bob, eventId2)
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, eventId1)
                }
                cut shouldBeUsers setOf(bob)
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isReadBy: not contain us from read marker") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                receipts.value = mapOf(eventIdByAlice to setOf(us, bob))
//                advanceUntilIdle()
//                cut.isReadBy.value?.map { it.userId } shouldBe listOf(bob)
//
//                cancelNeverEndingCoroutines()
//            }
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
                val cut = env.cutMessageReadReceipts(env.alice, event[0])
                cut shouldBeUsers setOf(env.bob)
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isReadBy: not contain us from subsequent events") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                    +messageEvent(sender = us) {
//                        text("Hi!")
//                    }
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                advanceUntilIdle()
//                cut.isReadBy.value shouldBe emptyList()
//
//                cancelNeverEndingCoroutines()
//            }
            should("not contain us from subsequent events") {
                val roomId = RoomId("room5", "localhost")
                val alice = UserId("alice5", "localhost")
                val eventId1 = EventId("1")
                val eventId2 = EventId("2")
                val cut = cutMessageReadReceipts(alice, eventId1, roomId)
                timeline(roomServiceMock, roomId) {
                    +timelineEventOf(alice, eventId1)
                    +timelineEventOf(us, eventId2)
                }
                roomUsers(userServiceMock, roomId) {}
                cut shouldBeUsers emptySet()
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isReadBy: not contain sender from subsequent events") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                    +messageEvent(sender = alice) {
//                        text("Hi!")
//                    }
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                advanceUntilIdle()
//                cut.isReadBy.value shouldBe emptyList()
//
//                cancelNeverEndingCoroutines()
//            }
            should("not contain sender from subsequent events") {
                val env = TestEnv(testScope = this)
                val (_, timeline, _, event) = env
                val cut = env.cutMessageReadReceipts(env.alice, event[0])
                timeline.addEvents {
                    +timelineEventOf(env.alice, event[0])
                    +timelineEventOf(env.alice, event[1])
                }
                cut shouldBeUsers emptySet()
                cancelNeverEndingCoroutines()
            }

//            if (false) should("isReadBy: not contain sender from read marker") {
//                timeline(roomServiceMock, roomId) {
//                    +timelineEventByAlice
//                }
//                val cut = cutOutDefaultSenderIsAlice()
//
//                launch { cut.isReadBy.collect() }
//                receipts.value = mapOf(eventIdByAlice to setOf(alice, bob))
//                advanceUntilIdle()
//                cut.isReadBy.value?.map { it.userId } shouldBe listOf(bob)
//
//                cancelNeverEndingCoroutines()
//            }
            should("not contain sender from read marker") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                val cut = env.cutMessageReadReceipts(env.alice, event[0])
                timeline.addEvents {
                    +timelineEventOf(env.alice, event[0])
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob, event[0])
                    +roomUser("Alice", env.alice, event[0])
                }
                cut shouldBeUsers setOf(env.bob)
                cancelNeverEndingCoroutines()
            }
        }
    }

    // TODO: maybe add some more better tests for read receipts

    private suspend inline infix fun Flow<Map<UserId, Flow<RoomUser?>>>.shouldBeUsers(requiredUsers: Set<UserId>) {
        withClue("unexpected result for reader receipts!") {
            val result = this.first()!!
            result.keys shouldBe requiredUsers
            result.map { (key, flow) ->
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

    // TODO remove
//    private val receipts = MutableStateFlow<Map<EventId, Set<UserId>>>(mapOf())

    // TODO remove
//    private val eventIdByAlice = EventId("event")

    // TODO remove
//    private val timelineEventByAlice = TimelineEvent(
//        event = MessageEvent(
//            TextBased.Text("Hi!"),
//            id = eventIdByAlice,
//            sender = alice,
//            roomId = roomId,
//            originTimestamp = 123456789L,
//        ),
//        content = null,
//        previousEventId = null,
//        nextEventId = null,
//        gap = null,
//    )

//    private fun TimelineBuilder.timelineEventOf(
//        senderId: UserId,
//        eventId: EventId?,
//    ):TimelineEvent = if(eventId!=null)
//    TimelineEvent(
//        event = MessageEvent(
//            TextBased.Text("Hi!"),
//            id = eventId,
//            sender = senderId,
//            roomId = roomId,
//            originTimestamp = 123456789L,
//        ),
//        content = null,
//        previousEventId = null,
//        nextEventId = null,
//        gap = null,
//    )else messageEvent(sender = senderId) {
//        text("Hi!")
//    }

    // TODO: remove
//    private fun TestScope.cutOutDefaultSenderIsAlice(
//        timelineEvent: TimelineEvent = this@getMessageReadReceiptsTest.timelineEventByAlice,
//        eventId: EventId = timelineEvent.eventId,
//    ): TimelineElementHolderViewModel {
//        val di = koinApplication {
//            modules(
//                createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))
//            )
//        }.koin
////        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
//        return TimelineElementHolderViewModelImpl(
//            viewModelContext = testMatrixClientViewModelContext(
//                di = di,
//                userId = us,
//            ),
//            key = "$roomId-$eventId",
//            roomId = roomId,
//            eventId = eventId,
//            senderUserId = timelineEvent.sender,
//            formattedDate = "01.01.2000",
//            formattedTime = "07:24",
//            showLoadingIndicatorBefore = flowOf(false),
//            showLoadingIndicatorAfter = flowOf(false),
//            showReplacedEvents = flowOf(false),
//            timelineEventFlow = flowOf(timelineEvent),
//            onMessageReplace = mock(),
//            onMessageReply = mock(),
//            onMessageReport = mock(),
//            onOpenMention = mock(),
//            onOpenMetadata = mock(),
//        )
//    }

    // TODO replace with variant above
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

    // TODO remove
    private fun TestScope.cutMessageIsRead(
        senderId: UserId,
        eventId: EventId,
        roomId: RoomId,
    ): Flow<Boolean?> = koinApplication {
        modules(
            createTestDefaultTrixnityMessengerModules(
                mapOf(us to matrixClientMock)
            )
        )
    }.koin.let { di ->
        getMessageIsRead(
            client = testMatrixClientViewModelContext(
                di = di,
                userId = us,
            ).matrixClient,
            senderUserId = senderId,
            roomId = roomId,
            eventId = eventId,
        )
    }

    // TODO remove
    private fun TestScope.cutMessageReadReceipts(
        senderId: UserId,
        eventId: EventId,
        roomId: RoomId,
    ): Flow<Map<UserId, Flow<RoomUser?>>> = koinApplication {
        modules(
            createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))
        )
    }.koin.let { di ->
        getMessageReadReceipts(
            client = testMatrixClientViewModelContext(
                di = di,
                userId = us,
            ).matrixClient,
            senderUserId = senderId,
            roomId = roomId,
            eventId = eventId,
        )
    }
}
