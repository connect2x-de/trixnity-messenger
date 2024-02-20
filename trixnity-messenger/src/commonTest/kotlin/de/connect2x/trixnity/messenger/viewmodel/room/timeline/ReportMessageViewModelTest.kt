package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import io.ktor.utils.io.core.toByteArray
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
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class)
class ReportMessageViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val roomId = RoomId("room1", "localhost")

    private val ourUserId = UserId("bob", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    private lateinit var canSendEventMocker: Mocker.Every<Flow<Boolean>>

    private val onMessageReportFinished = mockFunction1<Unit, EventId>(mocker)


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
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
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
                    userServiceMock.canSendEvent(isAny(), isAny())
                }

                canSendEventMocker returns flowOf(true)
                everySuspending {
                    roomServiceMock.sendMessage(
                        isEqual(roomId),
                        isAny(),
                        isAny()
                    )
                } returns ""
                every {
                    roomServiceMock.getTimelineEvent(isAny(), isEqual(eventId), isAny())
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
                every { onMessageReportFinished.invoke(isAny()) } returns Unit

                everySuspending {
                    roomsApiClientMock.reportEvent(
                        isAny(),
                        isAny(),
                        isAny(),
                        isAny(),
                        isAny()
                    )
                } returns Result.success(Unit)
            }
        }


        should("Clear report reason after successfully report to message") {
            val cut = reportToMessageViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.messageReportReason.value = "Report Reason"
            testCoroutineScheduler.advanceUntilIdle()

            cut.submitReportToMessage()

            mocker.verify(exhaustive = false) {
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