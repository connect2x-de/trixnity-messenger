package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class HistoryVisibilityStateTimelineElementViewModelTest {

    val roomId = RoomId("!room")
    val userId = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { userServiceMock.getById(roomId, userId) } returns MutableStateFlow(
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
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = true))
    }

    @Test
    fun `display who changed the room's history`() = runTest {
        val previousHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.INVITED
        val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
        val cut = historyVisibilityChangeStatusViewModel(
            timelineEvent = mockTimelineEvent(
                previousHistoryVisibilityEvent = UnsignedRoomEventData.UnsignedStateEventData(
                    previousContent = HistoryVisibilityEventContent(
                        historyVisibility = previousHistoryVisibilityEvent
                    )
                ), newHistoryVisibilityEventContent = newHistoryVisibilityEvent
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe """bob has changed the history visibility of the chat from 'invited' to 'shared'"""
        }
    }

    @Test
    fun `display who changed the room's history visibility without the old history if not set`() = runTest {
        val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
        val cut = historyVisibilityChangeStatusViewModel(
            timelineEvent = mockTimelineEvent(newHistoryVisibilityEventContent = newHistoryVisibilityEvent),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe """bob has changed the history visibility of the chat to 'shared'"""
        }
    }

    private fun TestScope.historyVisibilityChangeStatusViewModel(
        timelineEvent: TimelineEvent,
    ): HistoryVisibilityStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                )
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
            event = StateEvent(
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
