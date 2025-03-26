package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.flow.Flow
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
import net.folivo.trixnity.client.store.TimelineEventRelation
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.RelationType
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds


class TimelineElementHolderViewModelTest : ShouldSpec() {
    private val roomId = RoomId("room1", "localhost")
    private val eventId = EventId("event")

    private val us = UserId("mimi", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val timelineEvent = TimelineEvent(
        event = MessageEvent(
            TextBased.Text("Hi!"),
            id = eventId,
            sender = alice,
            roomId = roomId,
            originTimestamp = 123456789L,
        ),
        content = null,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private val receipts = MutableStateFlow<Map<EventId, Set<UserId>>>(mapOf())
    private val di = koinApplication {
        modules(
            createTestDefaultTrixnityMessengerModules()
        )
    }.koin
    private val config = di.get<MatrixMessengerConfiguration>()
    init {
        coroutineTestScope = true

        beforeTest {
            resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
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
            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns emptyFlow()

            receipts.value = mapOf()
        }

        should("isFirstInUserSequence: be true when first in a user sequence") {
            timeline(roomServiceMock, roomId) {
                (1..4).forEach {
                    +messageEvent(sender = bob) {
                        text("World-$it")
                    }
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
            val cut = cut()
            launch { cut.isFirstInUserSequence.collect() }
            advanceUntilIdle()
            cut.isFirstInUserSequence.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("isFirstInUserSequence: false when not first in a user sequence") {
            timeline(roomServiceMock, roomId) {
                (1..4).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
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
            val cut = cut()
            launch { cut.isFirstInUserSequence.collect() }
            advanceUntilIdle()
            cut.isFirstInUserSequence.value shouldBe false
            cancelNeverEndingCoroutines()
        }
        should("showSender: be true when first in a user sequence") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = bob) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showSender.collect() }
            advanceUntilIdle()
            cut.showSender.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("showSender: be true when state event before") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
                +stateEvent(sender = alice) {
                    content = MemberEventContent(membership = Membership.JOIN)
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showSender.collect() }
            advanceUntilIdle()
            cut.showSender.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("showSender: false when not first in a user sequence") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showSender.collect() }
            advanceUntilIdle()
            cut.showSender.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("showSender: be false when we are the sender") {
            val ourTimelineEvent = timelineEvent.copy(event = (timelineEvent.event as MessageEvent).copy(sender = us))
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = bob) {
                    text("Hi!")
                }
                +ourTimelineEvent
            }
            val cut = cut(timelineEvent = ourTimelineEvent)

            launch { cut.showSender.collect() }
            advanceUntilIdle()
            cut.showSender.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("showSender: be false when room is direct") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = bob) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showSender.collect() }
            advanceUntilIdle()
            cut.showSender.value shouldBe true

            timeline.room.update { it.copy(isDirect = true) }
            advanceUntilIdle()
            cut.showSender.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("isRead: should use latest replacement") {
            val replaceEventId = EventId("replace")
            val replacement = MutableStateFlow<Map<EventId, Flow<TimelineEventRelation?>>?>(null)
            every {
                roomServiceMock.getTimelineEventRelations(roomId, eventId, RelationType.Replace)
            } returns replacement
            val timeline = timeline(roomServiceMock, roomId) {
                +timelineEvent
            }
            val cut = cut()
            receipts.value = mapOf(eventId to setOf(bob))
            launch { cut.isRead.collect() }
            advanceUntilIdle()
            cut.isRead.value shouldBe true

            timeline.addEvents {
                +TimelineEvent(
                    event = MessageEvent(
                        TextBased.Text("Hi (edit)!"),
                        id = replaceEventId,
                        sender = alice,
                        roomId = roomId,
                        originTimestamp = 123456789L,
                    ),
                    previousEventId = null,
                    nextEventId = null,
                    gap = null,
                )
            }
            replacement.value = mapOf(
                replaceEventId to
                        flowOf(TimelineEventRelation(roomId, replaceEventId, RelationType.Replace, eventId))
            )

            advanceUntilIdle()
            cut.isRead.value shouldBe false
            cancelNeverEndingCoroutines()
        }
        should("showBigGapBefore: be true when first in a user sequence") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(
                    sender = bob,
                    sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                ) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showBigGapBefore.collect() }
            advanceUntilIdle()
            cut.showBigGapBefore.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("showBigGapBefore: false when not first in a user sequence") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                ) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showBigGapBefore.collect() }
            advanceUntilIdle()
            cut.showBigGapBefore.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("showBigGapBefore: be true when time gap is large enough") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp) - config.showBigGapBeforeThreshold - 1.seconds
                ) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showBigGapBefore.collect() }
            advanceUntilIdle()
            cut.showBigGapBefore.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("showBigGapBefore: be false when time gap is not large enough") {
            timeline(roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp) - config.showBigGapBeforeThreshold
                ) {
                    text("Hi!")
                }
                +timelineEvent
            }
            val cut = cut()

            launch { cut.showBigGapBefore.collect() }
            advanceUntilIdle()
            cut.showBigGapBefore.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("showUnreadMarker: show the unread marker when event is set") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
                +messageEvent(sender = bob) {
                    text("Hi!")
                }
            }
            val cut = cut(eventId = EventId("0"))

            launch { cut.showUnreadMarker.collect() }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe false

            timeline.fullyReadEventIndex.value = 0
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("showUnreadMarker: show the unread marker when subsequent event is added") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
            }
            val cut = cut(eventId = EventId("0"))

            timeline.fullyReadEventIndex.value = 0
            launch { cut.showUnreadMarker.collect() }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe false

            timeline.addEvents {
                +messageEvent(sender = bob) {
                    text("Hi!")
                }
            }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe true

            cancelNeverEndingCoroutines()
        }
        should("showUnreadMarker: not show the unread marker when subsequent event is added but by us") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
            }
            val cut = cut(eventId = EventId("0"))

            timeline.fullyReadEventIndex.value = 0
            launch { cut.showUnreadMarker.collect() }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe false

            timeline.addEvents {
                +messageEvent(sender = us) {
                    text("Hi!")
                }
            }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("showUnreadMarker: remove the unread marker when marker removed") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
                +messageEvent(sender = bob) {
                    text("Hi!")
                }
            }
            val cut = cut(eventId = EventId("0"))

            timeline.fullyReadEventIndex.value = 0
            launch { cut.showUnreadMarker.collect() }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe true

            timeline.fullyReadEventIndex.value = 1
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe false

            cancelNeverEndingCoroutines()
        }
        should("showUnreadMarker: not show the unread marker, when no subsequent event") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
            }
            val cut = cut(eventId = EventId("0"))

            timeline.fullyReadEventIndex.value = 0
            launch { cut.showUnreadMarker.collect() }
            advanceUntilIdle()
            cut.showUnreadMarker.value shouldBe false
            cancelNeverEndingCoroutines()
        }
        should("showUnreadMarker: not show the unread marker, when none of the next events is supported") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hi!")
                }
            }
            val cut = cut(eventId = EventId("0"))

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

            cancelNeverEndingCoroutines()
        }
        should("element: not create a new viewModel when a new message is sent afterwards") {
            val timeline = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Don't change my viewModel!")
                }
            }
            val event = timeline.eventsInStore.value[0]
            val eventHolder = cut(
                timelineEvent = event.value,
                timelineEventFlow = event
            )
            launch { eventHolder.element.collect() }
            val eventElement = eventHolder.element
            eventually(3.seconds) {
                eventElement.value.shouldBeInstanceOf<RoomMessageTimelineElementViewModel.TextBased.Text>()
            }
            val currentViewModel = eventElement.value
            timeline.addEvents {
                +messageEvent(sender = bob) {
                    text("This shouldn't change the former messages viewModel")
                }
            }
            continually(2.seconds) { eventElement.value shouldBeSameInstanceAs currentViewModel }

            cancelNeverEndingCoroutines()
        }
    }

    private fun TestScope.cut(
        timelineEvent: TimelineEvent = this@TimelineElementHolderViewModelTest.timelineEvent,
        timelineEventFlow: Flow<TimelineEvent>? = null,
        eventId: EventId = timelineEvent.eventId
    ): TimelineElementHolderViewModel {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))
            )
        }.koin
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return TimelineElementHolderViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = us,
            ),
            key = "$roomId-$eventId",
            roomId = roomId,
            eventId = eventId,
            senderUserId = timelineEvent.sender,
            formattedDate = "01.01.2000",
            formattedTime = "07:24",
            showUnreadMarker = roomServiceMock.getAccountData(roomId, FullyReadEventContent::class)
                .map { it?.eventId == eventId },
            showLoadingIndicatorBefore = flowOf(false),
            showLoadingIndicatorAfter = flowOf(false),
            ignoreReplacedEvents = false,
            getReceipts = { receipts },
            timelineEventFlow = timelineEventFlow ?: flowOf(timelineEvent),
            onMessageReplace = mock(),
            onMessageReply = mock(),
            onMessageReport = mock(),
            onOpenMention = mock(),
            onOpenMetadata = mock(),
        )
    }
}
