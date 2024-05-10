package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData.UnsignedStateEventData
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class HistoryVisibilityChangeStatusViewModelTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
        }

        should("display who changed the room's history") {
            val previousHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.INVITED
            val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
            val cut = historyVisibilityChangeStatusViewModel(
                timelineEvent =
                timelineEvent(
                    previousHistoryVisibilityEvent = UnsignedStateEventData(
                        previousContent = HistoryVisibilityEventContent(
                            historyVisibility = previousHistoryVisibilityEvent
                        )
                    ),
                    newHistoryVisibilityEventContent = newHistoryVisibilityEvent
                ),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.historyVisibilityMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.historyVisibilityMessage.value shouldBe """Bob has changed the history visibility of the group from '${cut.translateVisibility(previousHistoryVisibilityEvent)}' to '${cut.translateVisibility(newHistoryVisibilityEvent)}'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display who changed the room's history visibility without the old history if not set") {
            val newHistoryVisibilityEvent = HistoryVisibilityEventContent.HistoryVisibility.SHARED
            val cut =
                historyVisibilityChangeStatusViewModel(timelineEvent = timelineEvent(newHistoryVisibilityEventContent = newHistoryVisibilityEvent), coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.historyVisibilityMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.historyVisibilityMessage.value shouldBe """Bob has changed the history visibility of the group to '${cut.translateVisibility(newHistoryVisibilityEvent)}'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun historyVisibilityChangeStatusViewModel(
        timelineEvent: TimelineEvent,
        usernameFlow: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost"))),
        isDirectFlow: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ): HistoryVisibilityChangeStatusViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return HistoryVisibilityChangeStatusViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = timelineEvent,
            content = timelineEvent.event.content as HistoryVisibilityEventContent,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = usernameFlow,
            isDirectFlow = isDirectFlow,
        )
    }

    private fun timelineEvent(
        previousHistoryVisibilityEvent: UnsignedStateEventData? = null,
        newHistoryVisibilityEventContent: HistoryVisibilityEventContent.HistoryVisibility
    ) =
        TimelineEvent(
            event = StateEvent(
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
}
