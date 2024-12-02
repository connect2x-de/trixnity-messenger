package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class HistoryVisibilityStateTimelineElementViewModelTest : ShouldSpec() {

    val roomId = RoomId("room", "server")
    val userId = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        beforeTest {
            coroutineTestScope = true
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
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.changeMessage.value shouldBe """Bob has changed the history visibility of the group from 'FIXME' to 'FIXME'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display who changed the room's history visibility without the old history if not set") {
            val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
            val cut =
                historyVisibilityChangeStatusViewModel(
                    timelineEvent = mockTimelineEvent(newHistoryVisibilityEventContent = newHistoryVisibilityEvent),
                    coroutineContext = coroutineContext
                )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.changeMessage.value shouldBe """Bob has changed the history visibility of the group to 'FIXME'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun historyVisibilityChangeStatusViewModel(
        timelineEvent: TimelineEvent,
        coroutineContext: CoroutineContext,
    ): HistoryVisibilityStateTimelineElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher.Key]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return HistoryVisibilityStateTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
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
                id = EventId(""),
                sender = UserId(""),
                roomId = RoomId(""),
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
