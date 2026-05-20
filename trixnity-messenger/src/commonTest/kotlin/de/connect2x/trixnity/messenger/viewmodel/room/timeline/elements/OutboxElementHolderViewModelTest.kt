package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import de.connect2x.trixnity.core.model.events.UnknownEventContent
import de.connect2x.trixnity.core.model.events.block.EventContentBlocks
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.coroutineDispatcher
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.settle
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("NonAsciiCharacters")
class OutboxElementHolderViewModelTest {

    private val roomId = RoomId("!room1")
    private val eventId = EventId("event")

    private val us = UserId("mimi", "localhost")
    private val bob = UserId("bob", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()

    private val previousOutboxMessage =
        RoomOutboxMessage(
            roomId = roomId,
            transactionId = "t0",
            content = TextBased.Text("uhhh..."),
            createdAt = Instant.fromEpochMilliseconds(123456789L),
        )

    private val outboxMessage =
        RoomOutboxMessage(
            roomId = roomId,
            transactionId = "t1",
            content = TextBased.Text("Hi!"),
            createdAt = Instant.fromEpochMilliseconds(123456799L),
        )

    private val outbox = MutableStateFlow<List<RoomOutboxMessage<*>>>(listOf(outboxMessage))

    private val scope = TestScope()
    private val di =
        koinApplication { modules(scope.createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))) }.koin
    private val clock by lazy { di.get<Clock>() }
    private val config by lazy { di.get<MatrixMessengerConfiguration>() }

    init {
        Dispatchers.setMain(scope.coroutineDispatcher)
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns us
        every { userServiceMock.getById(roomId, any()) } calls
            { params ->
                val userId = params.args[1] as UserId
                flowOf(
                    RoomUser(
                        roomId,
                        userId,
                        userId.full,
                        ClientEvent.RoomEvent.StateEvent(
                            MemberEventContent(membership = Membership.JOIN),
                            id = eventId,
                            sender = userId,
                            roomId = roomId,
                            originTimestamp = 0L,
                            stateKey = userId.full,
                        ),
                    )
                )
            }
        outbox.value = listOf(outboxMessage)
        every { roomServiceMock.getOutbox(roomId) } returns outbox.map { it.map { flowOf(it) } }
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `isFirstInUserSequence » be true when last timeline event is not by us`() = scope.runTest {
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = bob) { text("Hi!") }
            // should be ignored
            +MessageEvent(
                content =
                    UnknownEventContent(
                        buildJsonObject { put("dino", JsonPrimitive("yes")) },
                        EventContentBlocks(),
                        "m.dino",
                    ),
                id = EventId("dino"),
                sender = us,
                roomId = roomId,
                originTimestamp = 1234,
            )
        }
        val cut = cut()
        backgroundScope.launch { cut.isFirstInUserSequence.collect() }
        delay(500.milliseconds)
        cut.isFirstInUserSequence.value shouldBe true
    }

    @Test
    fun `isFirstInUserSequence » ignore other outbox messages with transactionId same as last timeline event`() =
        scope.runTest {
            timeline(roomServiceMock, roomId) { +messageEvent(sender = bob, transactionId = "t0") { text("Hi!") } }
            outbox.value = listOf(previousOutboxMessage, outboxMessage)
            val cut = cut()
            backgroundScope.launch { cut.isFirstInUserSequence.collect() }
            delay(500.milliseconds)
            cut.isFirstInUserSequence.value shouldBe true
        }

    @Test
    fun `isFirstInUserSequence » be false when last timeline event is by us`() = scope.runTest {
        timeline(roomServiceMock, roomId) { +messageEvent(sender = us) { text("Hi!") } }
        val cut = cut()
        backgroundScope.launch { cut.isFirstInUserSequence.collect() }
        delay(500.milliseconds)
        cut.isFirstInUserSequence.value shouldBe false
    }

    @Test
    fun `isFirstInUserSequence » be false when not first outbox message`() = scope.runTest {
        timeline(roomServiceMock, roomId) { +messageEvent(sender = us) { text("Hi!") } }
        outbox.value =
            listOf(
                RoomOutboxMessage(
                    roomId = roomId,
                    transactionId = "t0",
                    content = TextBased.Text("Hi!"),
                    createdAt = Instant.fromEpochMilliseconds(123456799L),
                ),
                outboxMessage,
            )
        val cut = cut()
        backgroundScope.launch { cut.isFirstInUserSequence.collect() }
        delay(500.milliseconds)
        cut.isFirstInUserSequence.value shouldBe false
    }

    @Test
    fun `showSender » always be false`() = scope.runTest {
        timeline(roomServiceMock, roomId) { +messageEvent(sender = bob) { text("Hi!") } }
        val cut = cut()

        backgroundScope.launch { cut.showSender.collect() }
        delay(500.milliseconds)
        cut.showSender.value shouldBe false
    }

    @Test
    fun `showBigGapBefore » be true when first in a user sequence`() = scope.runTest {
        timeline(roomServiceMock, roomId) { +messageEvent(sender = bob, sentAt = clock.now()) { text("Hi!") } }
        val cut = cut()

        backgroundScope.launch { cut.showBigGapBefore.collect() }
        settle()
        cut.showBigGapBefore.value shouldBe true
    }

    @Test
    fun `showBigGapBefore » false when not first in a user sequence`() = scope.runTest {
        timeline(roomServiceMock, roomId) { +messageEvent(sender = us, sentAt = clock.now()) { text("Hi!") } }
        val cut = cut()

        backgroundScope.launch { cut.showBigGapBefore.collect() }
        settle()

        cut.showBigGapBefore.value shouldBe false
    }

    @Test
    fun `showBigGapBefore » be true when time gap is large enough`() = scope.runTest {
        delay(7.days)
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = us, sentAt = clock.now() - config.showBigGapBeforeThreshold - 1.seconds) {
                text("Hi!")
            }
        }
        val cut = cut()

        backgroundScope.launch { cut.showBigGapBefore.collect() }
        settle()
        cut.showBigGapBefore.value shouldBe true
    }

    @Test
    fun `showBigGapBefore » be false when time gap is not large enough`() = scope.runTest {
        delay(7.days)
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = us, sentAt = clock.now() - config.showBigGapBeforeThreshold) { text("Hi!") }
        }
        val cut = cut()
        backgroundScope.launch { cut.showBigGapBefore.collect() }
        settle()

        cut.showBigGapBefore.value shouldBe false
    }

    @Test
    fun `showBigGapBefore » skip sent transaction`() = scope.runTest {
        delay(7.days)
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = us, sentAt = clock.now() - config.showBigGapBeforeThreshold - 1.seconds) {
                text("Hi!")
            }

            +messageEvent(sender = us, sentAt = clock.now(), transactionId = "t1") { text("Hi!") }
        }
        val cut = cut()

        backgroundScope.launch { cut.showBigGapBefore.collect() }
        settle()

        cut.showBigGapBefore.value shouldBe true
    }

    @Test
    fun `showBigGapBefore » don't show if multiple elements in outbox`() = runTest {
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = us, sentAt = clock.now() - config.showBigGapBeforeThreshold - 1.seconds) {
                text("Hi!")
            }
        }

        outbox.value = listOf(previousOutboxMessage, outboxMessage)
        val cut = cut()

        backgroundScope.launch { cut.showBigGapBefore.collect() }
        settle()

        cut.showBigGapBefore.value shouldBe false
    }

    private fun TestScope.cut(evId: EventId = eventId): OutboxElementHolderViewModel {
        return OutboxElementHolderViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(di = di, userId = us),
            key = "$roomId-$evId",
            roomId = roomId,
            transactionId = outboxMessage.transactionId,
            outboxMessageFlow = flowOf(outboxMessage),
            formattedDate = "01.01.2000",
            formattedTime = "07:24",
            onOpenMention = mock(),
            jumpTo = { _, _ -> },
        )
    }
}
