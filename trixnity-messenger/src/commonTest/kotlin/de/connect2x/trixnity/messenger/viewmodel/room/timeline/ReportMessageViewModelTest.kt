package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class)
class ReportMessageViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    private val roomId = RoomId("room1", "localhost")

    private val ourUserId = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private lateinit var canSendEventMocker: BlockingAnsweringScope<Flow<Boolean>>

    private val onMessageReportFinished = mock<Function1<EventId, Unit>>()


    init {
        coroutineTestScope = true
        val eventId = EventId("0")
        val aliceUserId = UserId("@alice:localhost")
        val aliceRoomUser = roomUser(aliceUserId, "Alice")
        val bobRoomUser = roomUser(ourUserId, "Bob") // our == bob
        val alvinUserId = UserId("@alvin:localhost")
        val alvinRoomUser = roomUser(alvinUserId, "Alvin")
        val zoopUserId = UserId("@completelyDifferent:anotherplanet")
        val zoopRoomUser = roomUser(zoopUserId, "Zoop")
        val messageEvent = ClientEvent.RoomEvent.MessageEvent(
            content = RoomMessageEventContent.TextBased.Text("Hello"),
            id = eventId,
            sender = aliceUserId,
            roomId = roomId,
            originTimestamp = 0L,
        )

        beforeTest {
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
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns ourUserId
            every { matrixClientMock.api } returns matrixClientServerApiClientMock
            every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

            canSendEventMocker = every {
                userServiceMock.canSendEvent(any(), any())
            }

            canSendEventMocker returns flowOf(true)
            everySuspend {
                roomServiceMock.sendMessage(
                    eq(roomId),
                    any(),
                    any()
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
                    roomId,
                    isDirect = true
                )
            )
            every { userServiceMock.getById(roomId, aliceUserId) } returns MutableStateFlow(
                aliceRoomUser
            )
            every { onMessageReportFinished.invoke(any()) } returns Unit

            everySuspend {
                roomsApiClientMock.reportEvent(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns Result.success(Unit)
        }


        should("Clear report reason after successfully report to message") {
            val cut = reportToMessageViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.messageReportReason.value = "Report Reason"
            testCoroutineScheduler.advanceUntilIdle()

            cut.submitReportToMessage()

            verify {
                onMessageReportFinished.invoke(messageEvent.id)
            }


            cut.messageReportReason.value shouldBe null

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }


    private suspend fun reportToMessageViewModel(coroutineContext: CoroutineContext): ReportMessageViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return ReportMessageViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test",
                                    "server"
                                ) to matrixClientMock
                            )
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
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

    private fun CoroutineScope.subscribe(cut: ReportMessageViewModelImpl) = launch {
        launch { cut.messageReportReason.collect() }
    }

}
