package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.TimelineEventRelation
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.originTimestamp
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.UnknownEventContent
import de.connect2x.trixnity.core.model.events.block.EventContentBlocks
import de.connect2x.trixnity.core.model.events.m.DirectEventContent
import de.connect2x.trixnity.core.model.events.m.FullyReadEventContent
import de.connect2x.trixnity.core.model.events.m.Mentions
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.RelationType
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Suppress("NonAsciiCharacters")
class TimelineElementHolderViewModelTest {
    private val roomId = RoomId("!room1")
    private val eventId = EventId("event")
    private val usId = UserId("mimi", "localhost")
    private val aliceId = UserId("alice", "localhost")
    private val bobId = UserId("bob", "localhost")

    private val us = RoomUser(
        roomId, usId, usId.full, ClientEvent.RoomEvent.StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            id = eventId,
            sender = usId,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = usId.full,
        )
    )

    private val alice = RoomUser(
        roomId, aliceId, aliceId.full, ClientEvent.RoomEvent.StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            id = eventId,
            sender = aliceId,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = aliceId.full,
        )
    )

    private val bob = RoomUser(
        roomId, bobId, bobId.full, ClientEvent.RoomEvent.StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            id = eventId,
            sender = bobId,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = bobId.full,
        )
    )

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val timelineEvent = TimelineEvent(
        event = MessageEvent(
            TextBased.Text("Hi!"),
            id = eventId,
            sender = aliceId,
            roomId = roomId,
            originTimestamp = 123456789L,
        ),
        content = null,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private val receipts = MutableStateFlow<Map<EventId, Set<UserId>>>(mapOf())
    private val scope = TestScope()
    private val di = koinApplication {
        modules(
            scope.createTestDefaultTrixnityMessengerModules(mapOf(usId to matrixClientMock)),
        )
    }.koin
    private val config by lazy { di.get<MatrixMessengerConfiguration>() }

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns usId
        every { userServiceMock.getAccountData(DirectEventContent::class, any()) } returns flowOf(
            DirectEventContent(
                mapOf(
                    bobId to emptySet(),
                    aliceId to emptySet()
                )
            )
        )
        every { userServiceMock.getAll(roomId) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                bobId to flowOf(bob),
                usId to flowOf(us)
            )
        )
        every { userServiceMock.canSendEvent(roomId, any<KClass<out RoomEventContent>>()) } returns flowOf(true)
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
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns flowOf(null)
        receipts.value = mapOf()
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `isFirstInUserSequence » be true when first in a user sequence`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            (1..4).forEach {
                +messageEvent(sender = bobId) {
                    text("World-$it")
                }
            }
            // should be ignored
            +MessageEvent(
                content = UnknownEventContent(
                    buildJsonObject { put("dino", JsonPrimitive("yes")) },
                    EventContentBlocks(),
                    "m.dino"
                ),
                id = EventId("dino"),
                sender = aliceId,
                roomId = roomId,
                originTimestamp = 1234,
            )
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.isFirstInUserSequence.collect() }
        eventually(100.milliseconds) {
            cut.isFirstInUserSequence.value shouldBe true
        }
    }

    @Test
    fun `isFirstInUserSequence » false when not first in a user sequence`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            (1..4).forEach {
                +messageEvent(sender = aliceId) {
                    text("World-$it")
                }
            }
            // should be ignored
            +MessageEvent(
                content = UnknownEventContent(
                    buildJsonObject { put("dino", JsonPrimitive("yes")) },
                    EventContentBlocks(),
                    "m.dino"
                ),
                id = EventId("dino"),
                sender = bobId,
                roomId = roomId,
                originTimestamp = 1234,
            )
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.isFirstInUserSequence.collect() }
        eventually(100.milliseconds) {
            cut.isFirstInUserSequence.value shouldBe false
        }
    }

    @Test
    fun `showSender » be true when first in a user sequence`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = bobId) {
                text("Hi!")
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showSender.collect() }
        eventually(100.milliseconds) {
            cut.showSender.value shouldBe true
        }
    }

    @Test
    fun `showSender » be true when state event before`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
            +stateEvent(sender = aliceId) {
                content = MemberEventContent(membership = Membership.JOIN)
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showSender.collect() }
        eventually(100.milliseconds) {
            cut.showSender.value shouldBe true
        }
    }

    @Test
    fun `showSender » false when not first in a user sequence`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        every { userServiceMock.getAll(roomId) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice)
            )
        )
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hello!")
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showSender.collect() }
        eventually(100.milliseconds) {
            cut.showSender.value shouldBe false
        }
    }

    @Test
    fun `showSender » be false when we are the sender`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        every { userServiceMock.getAll(roomId) } returns flowOf(
            mapOf(
                bobId to flowOf(bob),
                usId to flowOf(us)
            )
        )
        val ourTimelineEvent = timelineEvent.copy(event = (timelineEvent.event as MessageEvent).copy(sender = usId))
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = bobId) {
                text("Hi!")
            }
            +ourTimelineEvent
        }
        val cut = cut(timelineEvent = ourTimelineEvent)
        backgroundScope.launch { cut.showSender.collect() }
        eventually(100.milliseconds) {
            cut.showSender.value shouldBe false
        }
    }

    @Test
    fun `showSender » be false when room is direct`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        every { userServiceMock.getAccountData(DirectEventContent::class, any()) } returns flowOf(
            DirectEventContent(
                mapOf(
                    aliceId to emptySet()
                )
            )
        )
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hello!")
            }
            +timelineEvent
            +messageEvent(sender = usId) {
                text("Hello!")
            }
        }

        val cut = cut()
        backgroundScope.launch { cut.showSender.collect() }
        timeline.room.update { it.copy(isDirect = true) }
        eventually(100.milliseconds) {
            cut.showSender.value shouldBe false
        }
    }

    @Test
    fun `isSent » should be false when outbox contains replaced element`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(
            listOf(
                flowOf(
                    RoomOutboxMessage(
                        roomId = roomId,
                        transactionId = "",
                        createdAt = Instant.DISTANT_PAST,
                        content = object : MessageEventContent {
                            override val relatesTo: RelatesTo = RelatesTo.Replace(eventId = eventId)
                            override val externalUrl: String? = null
                            override val mentions: Mentions? = null
                            override fun copyWith(relatesTo: RelatesTo?): MessageEventContent = this
                        }
                    ))))
        timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.isSent.collect() }
        delay(100.milliseconds)
        cut.isSent.value shouldBe false
    }

    @Test
    fun `isSent » should be true when outbox element is sent`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(
            listOf(
                flowOf(
                    RoomOutboxMessage(
                        roomId = roomId,
                        transactionId = "",
                        createdAt = Instant.DISTANT_PAST,
                        sentAt = Instant.DISTANT_PAST,
                        content = object : MessageEventContent {
                            override val relatesTo: RelatesTo = RelatesTo.Replace(eventId = eventId)
                            override val externalUrl: String? = null
                            override val mentions: Mentions? = null
                            override fun copyWith(relatesTo: RelatesTo?): MessageEventContent = this
                        }
                    ))))
        timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.isSent.collect() }
        delay(100.milliseconds)
        cut.isSent.value shouldBe true
    }

    @Test
    fun `isSent » should be true when outbox is empty`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.isSent.collect() }
        delay(100.milliseconds)
        cut.isSent.value shouldBe true
    }

    @Test
    fun `isRead » should use latest replacement`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val replaceEventId = EventId("replace")
        val replacement = MutableStateFlow<Map<EventId, Flow<TimelineEventRelation?>>?>(null)
        every {
            roomServiceMock.getTimelineEventRelations(roomId, eventId, RelationType.Replace)
        } returns replacement
        val timeline = timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut()
        receipts.value = mapOf(eventId to setOf(bobId))
        backgroundScope.launch { cut.isRead.collect() }
        eventually(100.milliseconds) {
            cut.isRead.value shouldBe true
        }
        timeline.addEvents {
            +TimelineEvent(
                event = MessageEvent(
                    TextBased.Text("Hi (edit)!"),
                    id = replaceEventId,
                    sender = aliceId,
                    roomId = roomId,
                    originTimestamp = 123456789L,
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        }
        replacement.value = mapOf(
            replaceEventId to flowOf(TimelineEventRelation(roomId, replaceEventId, RelationType.Replace, eventId))
        )
        eventually(100.milliseconds) {
            cut.isRead.value shouldBe false
        }
    }

    @Test
    fun `showBigGapBefore » be true when first in a user sequence`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +messageEvent(
                sender = bobId, sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
            ) {
                text("Hi!")
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showBigGapBefore.collect() }
        eventually(100.milliseconds) {
            cut.showBigGapBefore.value shouldBe true
        }
    }

    @Test
    fun `showBigGapBefore » false when not first in a user sequence`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +messageEvent(
                sender = aliceId, sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
            ) {
                text("Hi!")
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showBigGapBefore.collect() }
        eventually(100.milliseconds) {
            cut.showBigGapBefore.value shouldBe false
        }
    }

    @Test
    fun `showBigGapBefore » be true when time gap is large enough`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +messageEvent(
                sender = aliceId,
                sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                        - config.showBigGapBeforeThreshold - 1.seconds
            ) {
                text("Hi!")
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showBigGapBefore.collect() }
        eventually(100.milliseconds) {
            cut.showBigGapBefore.value shouldBe true
        }
    }

    @Test
    fun `showBigGapBefore » be false when time gap is not large enough`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +messageEvent(
                sender = aliceId, sentAt = Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                        - config.showBigGapBeforeThreshold
            ) {
                text("Hi!")
            }
            +timelineEvent
        }
        val cut = cut()
        backgroundScope.launch { cut.showBigGapBefore.collect() }
        eventually(100.milliseconds) {
            cut.showBigGapBefore.value shouldBe false
        }
    }

    @Test
    fun `showUnreadMarker » show the unread marker when event is set`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
            +messageEvent(sender = bobId) {
                text("Hi!")
            }
        }
        val cut = cut(eventId = EventId("0"))
        backgroundScope.launch { cut.showUnreadMarker.collect() }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
        timeline.fullyReadEventIndex.value = 0
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe true
        }
    }

    @Test
    fun `showUnreadMarker » show the unread marker when subsequent event is added`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
        }
        val cut = cut(eventId = EventId("0"))
        timeline.fullyReadEventIndex.value = 0
        backgroundScope.launch { cut.showUnreadMarker.collect() }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
        timeline.addEvents {
            +messageEvent(sender = bobId) {
                text("Hi!")
            }
        }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe true
        }
    }

    @Test
    fun `showUnreadMarker » not show the unread marker when subsequent event is added but by us`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
        }
        val cut = cut(eventId = EventId("0"))
        timeline.fullyReadEventIndex.value = 0
        backgroundScope.launch { cut.showUnreadMarker.collect() }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
        timeline.addEvents {
            +messageEvent(sender = usId) {
                text("Hi!")
            }
        }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
    }

    @Test
    fun `showUnreadMarker » remove the unread marker when marker removed`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
            +messageEvent(sender = bobId) {
                text("Hi!")
            }
        }
        val cut = cut(eventId = EventId("0"))
        timeline.fullyReadEventIndex.value = 0
        backgroundScope.launch { cut.showUnreadMarker.collect() }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe true
        }
        timeline.fullyReadEventIndex.value = 1
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
    }

    @Test
    fun `showUnreadMarker » not show the unread marker when no subsequent event`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
        }
        val cut = cut(eventId = EventId("0"))
        timeline.fullyReadEventIndex.value = 0
        backgroundScope.launch { cut.showUnreadMarker.collect() }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
    }

    @Test
    fun `showUnreadMarker » not show the unread marker when none of the next events is supported`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Hi!")
            }
        }
        val cut = cut(eventId = EventId("0"))
        timeline.fullyReadEventIndex.value = 0
        backgroundScope.launch { cut.showUnreadMarker.collect() }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
        timeline.addEvents {
            // should be ignored
            +MessageEvent(
                content = UnknownEventContent(
                    buildJsonObject { put("dino", JsonPrimitive("yes")) },
                    EventContentBlocks(),
                    "m.dino"
                ),
                id = EventId("dino"),
                sender = bobId,
                roomId = roomId,
                originTimestamp = 1234,
            )
        }
        eventually(100.milliseconds) {
            cut.showUnreadMarker.value shouldBe false
        }
    }

    @Test
    fun `element » not create a new viewModel when a new message is sent afterwards`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        val timeline = timeline(roomServiceMock, roomId) {
            +messageEvent(sender = aliceId) {
                text("Don't change my viewModel!")
            }
        }
        val event = timeline.eventsInStore.value[0]
        val eventHolder = cut(
            timelineEvent = event.value, timelineEventFlow = event
        )
        backgroundScope.launch { eventHolder.element.collect() }
        val eventElement = eventHolder.element
        eventually(3.seconds) {
            eventElement.value.shouldBeInstanceOf<RoomMessageTimelineElementViewModel.TextBased.Text>()
        }
        val currentViewModel = eventElement.value
        timeline.addEvents {
            +messageEvent(sender = bobId) {
                text("This shouldn't change the former messages viewModel")
            }
        }
        continually(2.seconds) { eventElement.value shouldBeSameInstanceAs currentViewModel }
    }

    @Test
    fun `isReplaced » ignoreReplacedEvents is false » should be false`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut(eventId = eventId, ignoreReplacedEvents = false)
        backgroundScope.launch { cut.isReplaced.collect() }
        eventually(100.milliseconds) {
            cut.isReplaced.value shouldBe false
        }
    }

    @Test
    fun `isReplaced » should be true when new content in outbox`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(
            listOf(
                flowOf(
                    RoomOutboxMessage(
                        roomId = roomId,
                        transactionId = "",
                        createdAt = Instant.DISTANT_PAST,
                        content = object : MessageEventContent {
                            override val relatesTo: RelatesTo =
                                RelatesTo.Replace(eventId = eventId, newContent = TextBased.Text("edit"))
                            override val externalUrl: String? = null
                            override val mentions: Mentions? = null
                            override fun copyWith(relatesTo: RelatesTo?): MessageEventContent = this
                        }
                    ))))
        timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut(eventId = eventId)
        backgroundScope.launch { cut.isReplaced.collect() }
        eventually(100.milliseconds) {
            cut.isReplaced.value shouldBe true
        }
    }

    @Test
    fun `isReplaced » should be true when replaced`() = runTest {
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        every { roomServiceMock.getTimelineEventRelations(roomId, eventId, RelationType.Replace) } returns flowOf(
            mapOf(
                EventId("0") to flowOf(TimelineEventRelation(roomId, EventId("0"), RelationType.Replace, eventId))
            )
        )
        timeline(roomServiceMock, roomId) {
            +timelineEvent
            +messageEvent(sender = aliceId) {
                text("edit")
            }
        }
        val cut = cut(eventId = eventId)
        backgroundScope.launch { cut.isReplaced.collect() }
        eventually(100.milliseconds) {
            cut.isReplaced.value shouldBe true
        }
    }

    @Test
    fun `errorIfReplaced » should show an error when sendError of replacing event is not null`() = runTest {
        val sendError = RoomOutboxMessage.SendError.Unknown(
            ErrorResponse.Unknown("Too large")
        )
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(
            listOf(
                flowOf(
                    RoomOutboxMessage(
                        roomId,
                        "0",
                        TextBased.Text("", relatesTo = RelatesTo.Replace(eventId)),
                        createdAt = Clock.System.now(),
                        sendError = sendError
                    )
                )
            )
        )
        every { roomServiceMock.getTimelineEventRelations(roomId, eventId, RelationType.Replace) } returns flowOf(
            mapOf()
        )
        timeline(roomServiceMock, roomId) {
            +timelineEvent
        }
        val cut = cut(eventId = eventId)
        cut.sendError.launchIn(backgroundScope)
        eventually(100.milliseconds) {
            cut.sendError.value shouldNotBe null
        }
    }

    private fun TestScope.cut(
        timelineEvent: TimelineEvent = this@TimelineElementHolderViewModelTest.timelineEvent,
        timelineEventFlow: Flow<TimelineEvent>? = null,
        eventId: EventId = timelineEvent.eventId,
        ignoreReplacedEvents: Boolean = true,
    ): TimelineElementHolderViewModel {
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return TimelineElementHolderViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = usId,
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
            ignoreReplacedEvents = ignoreReplacedEvents,
            getReceipts = { receipts },
            timelineEventFlow = timelineEventFlow ?: flowOf(timelineEvent),
            onMessageReplace = mock(),
            onMessageReply = mock(),
            onMessageReport = mock(),
            onOpenMention = mock(),
            onOpenMetadata = mock(),
            jumpTo = { _, _ -> }
        )
    }
}
