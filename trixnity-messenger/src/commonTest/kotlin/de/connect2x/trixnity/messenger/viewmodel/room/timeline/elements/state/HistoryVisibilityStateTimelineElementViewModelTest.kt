package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class HistoryVisibilityStateTimelineElementViewModelTest : ShouldSpec() {

    val roomId = RoomId("room", "server")
    val userId = UserId("user", "server")
    val eventId = EventId("event")
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
            every { userServiceMock.getById(roomId, userId) } returns flowOf(
                RoomUser(
                    roomId, userId, "bob", StateEvent(
                        content = MemberEventContent(membership = Membership.JOIN),
                        id = eventId,
                        sender = userId,
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = "",
                    )
                )
            )
            every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId, isDirect = true))
        }

        should("display who changed the room's history") {
            val previousHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.INVITED
            val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
            val cut = historyVisibilityChangeStatusViewModel(
                timelineEvent =
                    mockTimelineEvent(
                        previousHistoryVisibilityEvent = UnsignedRoomEventData.UnsignedStateEventData(
                            previousContent = HistoryVisibilityEventContent(
                                historyVisibility = previousHistoryVisibilityEvent
                            )
                        ),
                        newHistoryVisibilityEventContent = newHistoryVisibilityEvent
                    ),
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe """bob has changed the history visibility of the chat from 'invited' to 'shared'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display who changed the room's history visibility without the old history if not set") {
            val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
            val cut =
                historyVisibilityChangeStatusViewModel(
                    timelineEvent = mockTimelineEvent(newHistoryVisibilityEventContent = newHistoryVisibilityEvent),
                )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe """bob has changed the history visibility of the chat to 'shared'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun TestScope.historyVisibilityChangeStatusViewModel(
        timelineEvent: TimelineEvent,
    ): HistoryVisibilityStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return HistoryVisibilityStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            content = timelineEvent.event.content as HistoryVisibilityEventContent,
            roomId = roomId,
            eventId = eventId,
        )
    }

    private fun mockTimelineEvent(
        previousHistoryVisibilityEvent: UnsignedRoomEventData.UnsignedStateEventData? = null,
        newHistoryVisibilityEventContent: HistoryVisibilityEventContent.HistoryVisibility
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = ClientEvent.RoomEvent.StateEvent(
                HistoryVisibilityEventContent(historyVisibility = newHistoryVisibilityEventContent),
                id = eventId,
                sender = userId,
                roomId = roomId,
                originTimestamp = 0L,
                unsigned = previousHistoryVisibilityEvent,
                stateKey = ""
            ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return timelineEvent
    }
}
