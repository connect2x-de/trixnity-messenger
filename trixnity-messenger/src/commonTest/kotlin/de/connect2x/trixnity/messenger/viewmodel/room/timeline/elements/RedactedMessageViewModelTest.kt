package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.isTimelineEvent
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class)
class RedactedMessageViewModelTest : ShouldSpec() {

    private val roomId = RoomId("room1", "localhost")
    private val ourUserId = UserId("bob", "localhost")
    private val me = UserId("jonas", "localhost")
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

            every { matrixClientMock.userId } returns me
            every { userServiceMock.getById(roomId, ourUserId) } returns MutableStateFlow(
                roomUser(me, "TestUser")
            )
        }


        should("format generic message when redactedBy is null") {
            val timelineEventFlow = timelineEvent(messageEvent(RedactedEventContent("somethig"), sender = ourUserId))
            val cut = redactedMessageViewModel(
                timelineEvent = timelineEventFlow,
                coroutineContext = coroutineContext,
            )
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "This message has been deleted"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("append 'message deleted by me' when redactedBy ID matches current userId") {
            val timelineEventFlow = timelineEvent(messageEvent(RedactedEventContent("somethig"), sender = me))
            val cut = redactedMessageViewModel(
                timelineEvent = timelineEventFlow,
                coroutineContext = coroutineContext,
                redactedBy = me
            )
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "You deleted this message"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }


        should("append 'redacted by other user' when redactedBy does not match current userId") {
            val timelineEventFlow = timelineEvent(messageEvent(RedactedEventContent("somethig"), sender = ourUserId))

            val cut = redactedMessageViewModel(
                timelineEvent = timelineEventFlow,
                coroutineContext = coroutineContext,
                redactedBy = ourUserId
            )
            val subscriberJob = launch { cut.message.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe "message has been deleted by TestUser"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun redactedMessageViewModel(
        timelineEvent: TimelineEvent,
        redactedBy: UserId? = null,
        coroutineContext: CoroutineContext,
    ): RedactedTimelineElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return RedactedTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = timelineEvent,
            content = timelineEvent.event.content as RedactedEventContent,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(""),
            sender = MutableStateFlow(UserInfoElement("Bob", UserId("bob"))),
            formattedTime = "",
            isByMe = false,
            redactedBy = redactedBy,
            selectedRoomId = roomId,
            showBigGap = false,
            showSender = MutableStateFlow(false),
            showChatBubbleEdge = false

        )
    }

    private fun timelineEvent(
        event: ClientEvent.RoomEvent<*>,
        content: Result<RoomEventContent>? = null,
        previousEvent: TimelineEvent? = null
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = event,
            content = content,
            previousEventId = previousEvent?.eventId,
            nextEventId = null,
            gap = null,
        )

        every {
            roomServiceMock.getPreviousTimelineEvent(
                isTimelineEvent(timelineEvent),
                any(),
            )
        } returns
                previousEvent?.let { MutableStateFlow(it) }

        return timelineEvent
    }

    private fun messageEvent(content: MessageEventContent, sender: UserId) = ClientEvent.RoomEvent.MessageEvent(
        content,
        id = EventId(""),
        sender = sender,
        roomId = roomId,
        originTimestamp = 0L,
    )

    private fun roomUser(userId: UserId, name: String) = RoomUser(
        roomId,
        userId,
        name,
        event = ClientEvent.RoomEvent.StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            UserId(""),
            RoomId(""),
            0L,
            stateKey = ""
        )
    )
}
