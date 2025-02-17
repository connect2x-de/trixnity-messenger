package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.launchAndCollectCut
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.shouldGroup
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomUserBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineMock
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.ReadReceiptsCacheImpl
import de.connect2x.trixnity.messenger.viewmodel.util.ReadReceiptsHandleImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.withCleanup
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.failure
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


class TimelineElementHolderViewModelTest : ShouldSpec() {
    private val roomId = RoomId("room1", "localhost")
    private val eventId = EventId("event")

    private val us = UserId("mimi", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()

    private val timelineEvent = cutTimelineEvent(eventId, alice, roomId)

    private fun setupMocks(
        roomId: RoomId = this@TimelineElementHolderViewModelTest.roomId,
    ) {
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
        every { userServiceMock.canSendEvent(roomId, any()) } returns flowOf(true)
        every { userServiceMock.getById(roomId, any()) } calls { params ->
            val userId = params.args[1] as UserId
            flowOf(
                RoomUser(
                    roomId, userId, userId.full, ClientEvent.RoomEvent.StateEvent(
                        MemberEventContent(membership = Membership.JOIN),
                        id = eventId,
                        sender = userId,
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = userId.full,
                    )
                )
            )
        }
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        every { userServiceMock.getAllReceipts(any()) } returns MutableStateFlow(mapOf())
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns emptyFlow()
    }

    init {
        timeout = 5_000
        coroutineTestScope = true

        beforeEach {
            setupMocks()
        }

        shouldGroup("isFirstInUserSequence") {

            should("be true when first in a user sequence").withCleanup {
                timeline(roomServiceMock, roomId) {
                    (1..4).forEach {
                        +messageEvent(sender = bob) { text("World-$it") }
                    }
                    // should be ignored
                    +MessageEvent(
                        content = UnknownEventContent(buildJsonObject { put("dino", JsonPrimitive("yes")) }, "m.dino"),
                        id = EventId("dino"),
                        sender = alice,
                        roomId = roomId,
                        originTimestamp = 1234,
                    )
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.isFirstInUserSequence.collect() }
                advanceUntilIdle()
                cut.isFirstInUserSequence.value shouldBe true
            }

            should("false when not first in a user sequence").withCleanup {
                timeline(roomServiceMock, roomId) {
                    (1..4).forEach {
                        +messageEvent(sender = alice) { text("World-$it") }
                    }
                    // should be ignored
                    +MessageEvent(
                        content = UnknownEventContent(buildJsonObject { put("dino", JsonPrimitive("yes")) }, "m.dino"),
                        id = EventId("dino"),
                        sender = bob,
                        roomId = roomId,
                        originTimestamp = 1234,
                    )
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.isFirstInUserSequence.collect() }
                advanceUntilIdle()
                cut.isFirstInUserSequence.value shouldBe false
            }
        }

        shouldGroup("showSender") {

            should("be true when first in a user sequence").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe true
            }

            should("be true when state event before").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                    +stateEvent(sender = alice) {
                        content = MemberEventContent(membership = Membership.JOIN)
                    }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe true
            }

            should("false when not first in a user sequence").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe false
            }

            should("be false when we are the sender").withCleanup {
                val ourTimelineEvent =
                    timelineEvent.copy(event = (timelineEvent.event as MessageEvent).copy(sender = us))
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob) { text("Hi!") }
                    +ourTimelineEvent
                }
                val cut = cutTimelineElementHolderViewModel(timelineEvent = ourTimelineEvent)
                launch { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe false
            }

            should("be false when room is direct").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe true
                timeline.room.update { it.copy(isDirect = true) }
                advanceUntilIdle()
                cut.showSender.value shouldBe false
            }
        }

        shouldGroup("showBigGapBefore") {

            should("be true when first in a user sequence").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(
                        sender = bob,
                        sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                    ) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showBigGapBefore.collect() }
                advanceUntilIdle()
                cut.showBigGapBefore.value shouldBe true
            }

            should("false when not first in a user sequence").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(
                        sender = alice,
                        sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                    ) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showBigGapBefore.collect() }
                advanceUntilIdle()
                cut.showBigGapBefore.value shouldBe false
            }

            should("be true when time gap is large enough").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(
                        sender = alice,
                        sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp) - 1.hours - 1.seconds
                    ) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showBigGapBefore.collect() }
                advanceUntilIdle()
                cut.showBigGapBefore.value shouldBe true
            }

            should("be false when time gap is not large enough").withCleanup {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(
                        sender = alice,
                        sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp) - 1.hours
                    ) { text("Hi!") }
                    +timelineEvent
                }
                val cut = cutTimelineElementHolderViewModel()
                launch { cut.showBigGapBefore.collect() }
                advanceUntilIdle()
                cut.showBigGapBefore.value shouldBe false
            }
        }

        shouldGroup("showUnreadMarker") {

            should("show the unread marker when event is set").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                    +messageEvent(sender = bob) { text("Hi!") }
                }
                val cut = cutTimelineElementHolderViewModel(eventId = EventId("0"))

                launch { cut.showUnreadMarker.collect() }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false

                timeline.fullyReadEventIndex.value = 0
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe true
            }

            should("show the unread marker when a subsequent event is added").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                }
                val cut = cutTimelineElementHolderViewModel(eventId = EventId("0"))

                timeline.fullyReadEventIndex.value = 0
                launch { cut.showUnreadMarker.collect() }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false

                timeline.addEvents {
                    +messageEvent(sender = bob) { text("Hi!") }
                }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe true
            }

            should("not show the unread marker when subsequent event is added but by us").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                }
                val cut = cutTimelineElementHolderViewModel(eventId = EventId("0"))

                timeline.fullyReadEventIndex.value = 0
                launch { cut.showUnreadMarker.collect() }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false

                timeline.addEvents {
                    +messageEvent(sender = us) { text("Hi!") }
                }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false
            }

            should("remove the unread marker when marker removed").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                    +messageEvent(sender = bob) { text("Hi!") }
                }
                val cut = cutTimelineElementHolderViewModel(eventId = EventId("0"))

                timeline.fullyReadEventIndex.value = 0
                launch { cut.showUnreadMarker.collect() }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe true

                timeline.fullyReadEventIndex.value = 1
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false
            }

            should("not show the unread marker, when no subsequent event").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                }
                val cut = cutTimelineElementHolderViewModel(eventId = EventId("0"))

                timeline.fullyReadEventIndex.value = 0
                launch { cut.showUnreadMarker.collect() }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false
            }

            should("not show the unread marker, when none of the next events is supported").withCleanup {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hi!") }
                }
                val cut = cutTimelineElementHolderViewModel(eventId = EventId("0"))

                timeline.fullyReadEventIndex.value = 0
                launch { cut.showUnreadMarker.collect() }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false

                timeline.addEvents {
                    // should be ignored
                    +MessageEvent(
                        content = UnknownEventContent(buildJsonObject { put("dino", JsonPrimitive("yes")) }, "m.dino"),
                        id = EventId("dino"),
                        sender = bob,
                        roomId = roomId,
                        originTimestamp = 1234,
                    )
                }
                advanceUntilIdle()
                cut.showUnreadMarker.value shouldBe false
            }
        }

        shouldGroup("is read and reactions") {

            var runId = 0 // Mokkery fails to properly update existing mocks.

            data class TestEnv(
                val roomId: RoomId = RoomId("room_${runId++}", "localhost")
                    .also { setupMocks(it) },
                val timeline: TimelineMock = timeline(roomServiceMock, roomId) {},
                val roomUsers: RoomUserBuilder = roomUsers(userServiceMock, roomId) {},
                val eventIds: List<EventId> = (0..4).map { EventId("event_${runId}_$it") },
                val us: UserId = this@TimelineElementHolderViewModelTest.us,
                val alice: UserId = UserId("alice_$runId", "localhost"),
                val bob: UserId = UserId("bob_$runId", "localhost"),
            )

            should("see if the message has been read").withCleanup {
                val env = TestEnv()
                val (roomId, timeline, roomUsers, event, _, alice, bob) = env
                val readMessage = cutTimelineEvent(event[1], alice, roomId)
                timeline.addEvents {
                    +cutTimelineEvent(event[0], alice, roomId)
                    +readMessage
                }
                val cut = cutTimelineElementHolderViewModel(readMessage, roomId = env.roomId)
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, lastReadMessage = null)
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, lastReadMessage = event[0])
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, lastReadMessage = readMessage.eventId)
                }
                launchAndCollectCut(
                    cut.isRead,
                    2,
                    5.seconds,
                ) { result, updateCount ->
                    when (updateCount) {
                        1 -> result shouldBe false
                        2 -> result shouldBe true
                    }
                }
            }

            should("see if the message has not been read").withCleanup {
                val env = TestEnv()
                val (roomId, timeline, roomUsers, event, us, alice, bob) = env
                val message = listOf(
                    cutTimelineEvent(event[0], alice, roomId),
                    cutTimelineEvent(event[1], alice, roomId),
                )
                timeline.addEvents { message.forEach { +it } }
                val cut = cutTimelineElementHolderViewModel(message[1], roomId = env.roomId)
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, lastReadMessage = null)
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob, lastReadMessage = event[0])
                }
                launchAndCollectCut(
                    cut.isRead,
                    1,
                    2.seconds,
                ) { result, updateCount ->
                    when (updateCount) {
                        1 -> result shouldBe false
                    }
                }
            }

            should("see if the message has any reactions").withCleanup {
                val env = TestEnv()
                val (roomId, timeline, roomUsers, event, us, alice, bob) = env
                val message = cutTimelineEvent(event[0], alice, roomId)
                timeline.addEvents {
                    +message
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob)
                    +roomUser("Martin", us)
                }
                val cut = cutTimelineElementHolderViewModel(message, roomId = env.roomId)
                timeline.addEvents {
                    +messageEvent(sender = bob, eventId = event[1])
                    { reaction(event[0], "😄") }
                }
                timeline.addEvents {
                    +messageEvent(sender = us, eventId = event[2])
                    { reaction(event[0], "🥳") }
                }
                timeline.addEvents {
                    +messageEvent(sender = us, eventId = event[3])
                    { reaction(event[0], "😄") }
                }
                timeline.addEvents {
                    +messageEvent(sender = alice, eventId = event[4])
                    { reaction(event[0], "🙂") }
                }
                launchAndCollectCut(
                    cut.reactions,
                    2,
                    2.seconds,
                ) { result, updateCount ->
                    when (updateCount) {
                        1 -> result shouldBe emptySet()

                        2 -> result shouldBe setOf(
                            ReactionsExpectation("😄", false, event[1], bob),
                            ReactionsExpectation("🥳", true, event[2], us),
                            ReactionsExpectation("😄", true, event[3], us),
                            ReactionsExpectation("🙂", false, event[4], alice),
                        )
                    }
                }
            }

            should("see if the message has no reactions").withCleanup {
                val env = TestEnv()
                val (roomId, timeline, roomUsers, event, us, alice, bob) = env
                val message = listOf(
                    cutTimelineEvent(event[0], alice, roomId),
                    cutTimelineEvent(event[1], alice, roomId),
                )
                timeline.addEvents { message.forEach { +it } }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", bob)
                }
                val cut = cutTimelineElementHolderViewModel(message[0], roomId = env.roomId)
                timeline.addEvents {
                    +messageEvent(sender = bob, eventId = event[2])
                    { reaction(event[1], "😄") }
                }
                launchAndCollectCut(
                    cut.reactions,
                    1,
                    2.seconds,
                ) { result, updateCount ->
                    when (updateCount) {
                        1 -> result shouldBe emptySet()
                    }
                }
            }
        }
    }

    private fun TestScope.cutTimelineElementHolderViewModel(
        timelineEvent: TimelineEvent = this@TimelineElementHolderViewModelTest.timelineEvent,
        eventId: EventId = timelineEvent.eventId,
        roomId: RoomId = this@TimelineElementHolderViewModelTest.roomId,
    ): TimelineElementHolderViewModel {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))
            )
        }.koin
        every { roomServiceMock.getTimelineEvent(timelineEvent.roomId, eventId) } returns flowOf(timelineEvent)
        val viewModelContext = testMatrixClientViewModelContext(
            di = di,
            userId = us,
        )
        return TimelineElementHolderViewModelImpl(
            viewModelContext = viewModelContext,
            key = "${timelineEvent.roomId}-$eventId",
            roomId = timelineEvent.roomId,
            eventId = eventId,
            senderUserId = timelineEvent.sender,
            formattedDate = "01.01.2000",
            formattedTime = "07:24",
            showUnreadMarker = roomServiceMock.getAccountData(roomId, FullyReadEventContent::class)
                .map { it?.eventId == eventId },
            showLoadingIndicatorBefore = flowOf(false),
            showLoadingIndicatorAfter = flowOf(false),
            timelineEventFlow = flowOf(timelineEvent),
            onMessageReplace = mock(),
            onMessageReply = mock(),
            onMessageReport = mock(),
            onOpenMention = mock(),
            readReceipts = ReadReceiptsHandleImpl(
                eventId = timelineEvent.eventId,
                roomId = timelineEvent.roomId,
                senderId = timelineEvent.sender,
                initials = Initials,
                client = viewModelContext.matrixClient,
                config = MatrixMessengerConfiguration(),
                cache = ReadReceiptsCacheImpl(
                    client = viewModelContext.matrixClient,
                ),
            ),
        )
    }

    private fun cutTimelineEvent(
        eventId: EventId,
        senderId: UserId,
        roomId: RoomId,
        previousEventId: EventId? = null,
    ) = TimelineEvent(
        event = MessageEvent(
            TextBased.Text("Hi!"),
            id = eventId,
            sender = senderId,
            roomId = roomId,
            originTimestamp = 123456789L,
        ),
        content = null,
        previousEventId = previousEventId,
        nextEventId = null,
        gap = null,
    )

    private data class ReactionsExpectation(
        val reactionKey: ReactionKey, val isMe: Boolean,
        val eventId: EventId, val senderId: UserId,
    )

    private inline infix fun Map<ReactionKey, Set<MessageUserReactions.ReactionEvent>>.shouldBe(
        expectation: Set<ReactionsExpectation>,
    ) = withClue(
        """
            didn't receive expected reactions!
            expected: ${expectation.joinToString("\n\t", "\n\t")}
            but received: ${
            this.map {
                it.value
                    .joinToString("\n\t${it.key} - ", "\n\t${it.key} - ") {
                        "ReactionEvent(isByMe=${it.isMe}, eventId=${it.eventId}, senderId=${
                            it.sender.userId
                        })"
                    }
            }.joinToString("")
        }
        """.trimIndent()
    ) {
        this.flatMap { it.value } shouldHaveSize expectation.size
        val expectedReactions = expectation.groupBy { it.reactionKey }
        expectation.forEach { expect ->
            this[expect.reactionKey]!!.let { events ->
                events shouldHaveSize expectedReactions[expect.reactionKey]!!.size
                if (events.find { got ->
                        got.isMe == expect.isMe &&
                                got.eventId == expect.eventId &&
                                got.sender.userId == expect.senderId
                    } == null) throw failure("did not find event: $expect")
            }
        }
    }
}
