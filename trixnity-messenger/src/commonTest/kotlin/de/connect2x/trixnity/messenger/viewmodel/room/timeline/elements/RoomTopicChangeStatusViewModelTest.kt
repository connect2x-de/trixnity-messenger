package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.mock
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
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent
import org.koin.dsl.koinApplication
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomTopicChangeStatusViewModelTest : ShouldSpec() {

    val matrixClientMock = mock<MatrixClient>()

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(matrixClientMock)
        }

        should("display who changed the room's topic (with reference to the old topic)") {
            val cut = roomTopicChangeStatusViewModel(
                timelineEvent =
                timelineEvent(
                    previousTopicEvent = UnsignedStateEventData(previousContent = TopicEventContent("old topic"))
                ),
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomTopicChangeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomTopicChangeMessage.value shouldBe """Bob has changed the topic of the group from 'old topic' to 'new topic'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display who changed the room's topic without the old topic if not set") {
            val cut =
                roomTopicChangeStatusViewModel(timelineEvent = timelineEvent(), coroutineContext = coroutineContext)
            val subscriberJob = launch { cut.roomTopicChangeMessage.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomTopicChangeMessage.value shouldBe """Bob has changed the topic of the group to 'new topic'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val usernameFlow = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost")))
            val cut = roomTopicChangeStatusViewModel(
                timelineEvent = timelineEvent(),
                usernameFlow = usernameFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomTopicChangeMessage.collect {} }
            usernameFlow.value = UserInfoElement("Bobby", UserId("bobby:localhost"))
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomTopicChangeMessage.first() shouldBe """Bobby has changed the topic of the group to 'new topic'"""
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to changes of room's direct value") {
            val isDirectFlow = MutableStateFlow(false)
            val cut = roomTopicChangeStatusViewModel(
                timelineEvent = timelineEvent(),
                isDirectFlow = isDirectFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.roomTopicChangeMessage.collect {} }
            isDirectFlow.value = true
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomTopicChangeMessage.first() shouldBe """Bob has changed the topic of the chat to 'new topic'"""

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomTopicChangeStatusViewModel(
        timelineEvent: TimelineEvent,
        usernameFlow: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Bob", UserId("bob:localhost"))),
        isDirectFlow: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ): RoomTopicChangeStatusViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return RoomTopicChangeStatusViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = timelineEvent,
            content = timelineEvent.event.content as TopicEventContent,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = usernameFlow,
            isDirectFlow = isDirectFlow,
        )
    }

    private fun timelineEvent(previousTopicEvent: UnsignedStateEventData? = null) =
        TimelineEvent(
            event = StateEvent(
                TopicEventContent("new topic"),
                id = EventId(""),
                sender = UserId(""),
                roomId = RoomId(""),
                originTimestamp = 0L,
                unsigned = previousTopicEvent,
                stateKey = ""
            ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
}
