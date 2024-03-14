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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData.UnsignedStateEventData
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomNameChangeStatusViewModelTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
        }

        should("display who changed the room's name (with reference to the old name)") {
            val cut = roomNameChangeStatusViewModel(
                timelineEvent =
                timelineEvent(
                    previousNameEvent = UnsignedStateEventData(previousContent = NameEventContent("old name"))
                ),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomNameChangeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomNameChangeMessage.value shouldBe """Bob has changed the name of the group from 'old name' to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display who changed the room's name without the old name if not set") {
            val cut =
                roomNameChangeStatusViewModel(timelineEvent = timelineEvent(), coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.roomNameChangeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomNameChangeMessage.value shouldBe """Bob has changed the name of the group to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val usernameFlow = MutableStateFlow(UserInfoElement("Bob"))
            val cut = roomNameChangeStatusViewModel(
                timelineEvent = timelineEvent(),
                usernameFlow = usernameFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomNameChangeMessage.collect {} }
            usernameFlow.value = UserInfoElement("Bobby")
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomNameChangeMessage.first() shouldBe """Bobby has changed the name of the group to 'new name'"""
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to changes of room's direct value") {
            val isDirectFlow = MutableStateFlow(false)
            val cut = roomNameChangeStatusViewModel(
                timelineEvent = timelineEvent(),
                isDirectFlow = isDirectFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomNameChangeMessage.collect {} }
            isDirectFlow.value = true
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomNameChangeMessage.first() shouldBe """Bob has changed the name of the chat to 'new name'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomNameChangeStatusViewModel(
        timelineEvent: TimelineEvent,
        usernameFlow: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Bob")),
        isDirectFlow: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ): RoomNameChangeStatusViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return RoomNameChangeStatusViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = timelineEvent,
            content = timelineEvent.event.content as NameEventContent,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = usernameFlow,
            isDirectFlow = isDirectFlow,
        )
    }

    private fun timelineEvent(previousNameEvent: UnsignedStateEventData? = null) =
        TimelineEvent(
            event = StateEvent(
                NameEventContent("new name"),
                id = EventId(""),
                sender = UserId(""),
                roomId = RoomId(""),
                originTimestamp = 0L,
                unsigned = previousNameEvent,
                stateKey = ""
            ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
}
