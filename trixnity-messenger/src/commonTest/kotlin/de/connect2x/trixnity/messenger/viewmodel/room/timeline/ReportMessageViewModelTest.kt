package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class ReportMessageViewModelTest {
    private val roomId = RoomId("!room1")

    private val ourUserId = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private var canSendEventMocker: BlockingAnsweringScope<Flow<Boolean>>

    private val onMessageReportFinished = mock<Function0<Unit>>()


    init {
        val eventId = EventId("0")
        val aliceUserId = UserId("@alice:localhost")
        val aliceRoomUser = roomUser(aliceUserId, "Alice")
        val messageEvent = ClientEvent.RoomEvent.MessageEvent(
            content = RoomMessageEventContent.TextBased.Text("Hello"),
            id = eventId,
            sender = aliceUserId,
            roomId = roomId,
            originTimestamp = 0L,
        )

        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            matrixClientServerApiClientMock,
            roomsApiClientMock,
            onMessageReportFinished
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns ourUserId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

        canSendEventMocker = every {
            userServiceMock.canSendEvent(any(), any<KClass<out RoomEventContent>>())
        }

        canSendEventMocker returns flowOf(true)
        everySuspend {
            roomServiceMock.sendMessage(
                eq(roomId), any(), any()
            )
        } returns ""
        every {
            roomServiceMock.getTimelineEvent(any(), eq(eventId), any())
        } returns flowOf(
            TimelineEvent(
                event = messageEvent,
                content = Result.success(RoomMessageEventContent.TextBased.Text("Hello")),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(
            Room(
                roomId, isDirect = true
            )
        )
        every { userServiceMock.getById(roomId, aliceUserId) } returns MutableStateFlow(
            aliceRoomUser
        )
        every { onMessageReportFinished.invoke() } returns Unit

        everySuspend {
            roomsApiClientMock.reportEvent(
                any(), any(), any(), any(), any()
            )
        } returns Result.success(Unit)
    }


    @Test
    fun `Clear report reason after successfully report to message`() = runTest {
        val cut = reportToMessageViewModel()
        subscribe(cut)

        cut.messageReportReason.update("Report Reason")
        delay(500.milliseconds)

        cut.submitReportToMessage()
        delay(500.milliseconds)

        verify {
            onMessageReportFinished.invoke()
        }
        cut.messageReportReason.textValue shouldBe ""
    }

    private fun TestScope.reportToMessageViewModel(): ReportMessageViewModelImpl {
        return ReportMessageViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test", "server"
                                ) to matrixClientMock
                            )
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
            ),
            roomId = roomId,
            eventId = EventId("0"),
            onReportMessageFinished = onMessageReportFinished,
        )
    }

    private fun roomUser(userId: UserId, name: String) = RoomUser(
        roomId, userId, name, ClientEvent.RoomEvent.StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = EventId("123"),
            sender = userId,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = "",
        )
    )

    private fun TestScope.subscribe(cut: ReportMessageViewModelImpl) = backgroundScope.launch {
        cut.messageReportReason.collect()
    }

}
