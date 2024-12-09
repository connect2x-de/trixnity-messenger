package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import korlibs.io.async.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class OutboxElementHolderViewModelTest : ShouldSpec() {

    private val roomId = RoomId("room1", "localhost")
    private val eventId = EventId("event")

    private val us = UserId("mimi", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()

    private val outboxMessage = RoomOutboxMessage(
        roomId = roomId,
        transactionId = "t1",
        content = TextBased.Text("Hi!"),
        createdAt = Instant.fromEpochMilliseconds(123456799L),
    )

    private val outbox = MutableStateFlow<List<RoomOutboxMessage<*>>>(listOf(outboxMessage))

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
                            stateKey = userId.full
                        )
                    )
                )
            }
            outbox.value = listOf(outboxMessage)
            every { roomServiceMock.getOutbox(roomId) } returns outbox.map { it.map { flowOf(it) } }
        }

        context(OutboxElementHolderViewModel::isFirstInUserSequence.name) {
            should("be true when last timeline event is not by us") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob) {
                        text("Hi!")
                    }
                    // should be ignored
                    +MessageEvent(
                        content = UnknownEventContent(buildJsonObject { put("dino", JsonPrimitive("yes")) }, "m.dino"),
                        id = EventId("dino"),
                        sender = us,
                        roomId = roomId,
                        originTimestamp = 1234,
                    )
                }
                val cut = cut()
                async { cut.isFirstInUserSequence.collect() }
                advanceUntilIdle()
                cut.isFirstInUserSequence.value shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("ignore other outbox messages with transactionId same as last timeline event") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob, transactionId = "t0") {
                        text("Hi!")
                    }
                }
                outbox.value = listOf(
                    RoomOutboxMessage(
                        roomId = roomId,
                        transactionId = "t0",
                        content = TextBased.Text("Hi!"),
                        createdAt = Instant.fromEpochMilliseconds(123456799L),
                    ), outboxMessage
                )
                val cut = cut()
                async { cut.isFirstInUserSequence.collect() }
                advanceUntilIdle()
                cut.isFirstInUserSequence.value shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("be false when last timeline event is by us") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = us) {
                        text("Hi!")
                    }
                }
                val cut = cut()
                async { cut.isFirstInUserSequence.collect() }
                advanceUntilIdle()
                cut.isFirstInUserSequence.value shouldBe false

                cancelNeverEndingCoroutines()
            }
            should("be false when not first outbox message") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = us) {
                        text("Hi!")
                    }
                }
                outbox.value = listOf(
                    RoomOutboxMessage(
                        roomId = roomId,
                        transactionId = "t0",
                        content = TextBased.Text("Hi!"),
                        createdAt = Instant.fromEpochMilliseconds(123456799L),
                    ), outboxMessage
                )
                val cut = cut()
                async { cut.isFirstInUserSequence.collect() }
                advanceUntilIdle()
                cut.isFirstInUserSequence.value shouldBe false

                cancelNeverEndingCoroutines()
            }
        }
        context(OutboxElementHolderViewModel::showSender.name) {
            should("be true when first in a user sequence (showSender)") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob) {
                        text("Hi!")
                    }
                }
                val cut = cut()

                async { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("false when not first in a user sequence (showSender)") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = us) {
                        text("Hi!")
                    }
                }
                val cut = cut()

                async { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe false

                cancelNeverEndingCoroutines()
            }
            should("be false when room is direct (showSender)") {
                val timeline = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = bob) {
                        text("Hi!")
                    }
                }
                val cut = cut()

                async { cut.showSender.collect() }
                advanceUntilIdle()
                cut.showSender.value shouldBe true

                timeline.room.update { it.copy(isDirect = true) }
                advanceUntilIdle()
                cut.showSender.value shouldBe false

                cancelNeverEndingCoroutines()
            }
        }
    }

    private fun TestScope.cut(eventId: EventId = this@OutboxElementHolderViewModelTest.eventId): OutboxElementHolderViewModel {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(us to matrixClientMock))
            )
        }.koin
        return OutboxElementHolderViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = us,
            ),
            key = "$roomId-$eventId",
            roomId = roomId,
            transactionId = outboxMessage.transactionId,
            outboxMessageFlow = flowOf(outboxMessage),
            formattedDate = "01.01.2000",
            formattedTime = "07:24",
            onOpenMention = mock()
        )
    }
}
