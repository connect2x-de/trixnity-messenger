package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class)
class RedactedTimelineElementViewModelTest : ShouldSpec() {

    private val roomId = RoomId("room1", "localhost")
    private val someUserId = UserId("bob", "localhost")
    private val ourUserId = UserId("jonas", "localhost")
    val eventId = EventId("0")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

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

            every { matrixClientMock.userId } returns ourUserId
            every { userServiceMock.getById(roomId, someUserId) } returns roomUserFlow(ourUserId)
        }

        should("create generic message") {
            every {
                roomServiceMock.getTimelineEvent(roomId, eventId)
            } returns redactedTimelineEventFLow(sender = someUserId, redactedBy = null)
            val cut = redactedMessageViewModel(
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "This message has been deleted"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
        should("create message when deleted by us") {
            every {
                roomServiceMock.getTimelineEvent(roomId, eventId)
            } returns redactedTimelineEventFLow(sender = someUserId, redactedBy = ourUserId)
            val cut = redactedMessageViewModel(
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "You deleted this message"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
        should("create message when deleted by other user") {
            every {
                roomServiceMock.getTimelineEvent(roomId, eventId)
            } returns redactedTimelineEventFLow(sender = someUserId, redactedBy = someUserId)

            val cut = redactedMessageViewModel(
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "message has been deleted by Other User"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun redactedMessageViewModel(
        coroutineContext: CoroutineContext,
    ): RedactedTimelineElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(ourUserId to matrixClientMock))
            )
        }.koin
        return RedactedTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = ourUserId,
                coroutineContext = coroutineContext
            ),
            roomId = roomId,
            eventId = eventId
        )
    }

    private fun redactedTimelineEventFLow(sender: UserId, redactedBy: UserId?) = flowOf(
        TimelineEvent(
            event = ClientEvent.RoomEvent.MessageEvent(
                RedactedEventContent("m.room.message"),
                id = EventId("event"),
                sender = sender,
                roomId = roomId,
                originTimestamp = 0L,
                unsigned = UnsignedRoomEventData.UnsignedMessageEventData(
                    redactedBecause = redactedBy?.let {
                        ClientEvent.RoomEvent.MessageEvent(
                            RedactionEventContent(EventId("event")),
                            id = EventId("redaction"),
                            sender = it,
                            roomId = roomId,
                            originTimestamp = 24L,
                        )
                    }
                )
            ),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
    )

    private fun roomUserFlow(userId: UserId) = flowOf(
        RoomUser(
            roomId,
            userId,
            "Other User",
            event = ClientEvent.RoomEvent.StateEvent(
                MemberEventContent(membership = Membership.JOIN),
                EventId(""),
                UserId(""),
                RoomId(""),
                0L,
                stateKey = ""
            )
        )
    )
}
