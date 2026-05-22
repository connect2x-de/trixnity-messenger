package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class EncryptionStateTimelineElementViewModelTest {

    val roomId = RoomId("!room")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin
        senderName.value = "Bob"
        every { userServiceMock.getById(roomId, sender) } returns
            senderName.map {
                RoomUser(
                    roomId,
                    sender,
                    it,
                    StateEvent(
                        content = MemberEventContent(membership = Membership.JOIN),
                        id = eventId,
                        sender = sender,
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = "",
                    ),
                )
            }
        isDirect.value = false
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it) }

        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns
            flowOf(
                TimelineEvent(
                    event =
                        StateEvent(
                            EncryptionEventContent(),
                            id = eventId,
                            sender = sender,
                            roomId = roomId,
                            originTimestamp = 0L,
                            stateKey = "",
                        ),
                    previousEventId = null,
                    nextEventId = null,
                    gap = null,
                )
            )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `display who enabled to end-to-end encryption`() = runTest {
        val cut = roomEncryptionEnabledViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) { cut.changeMessage.value shouldBe "Bob enabled end-to-end encryption" }
    }

    @Test
    fun `react to username changes`() = runTest {
        val cut = roomEncryptionEnabledViewModel()
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) { cut.changeMessage.value shouldBe "Bob enabled end-to-end encryption" }

        senderName.value = "Bobby"

        eventually(100.milliseconds) { cut.changeMessage.value shouldBe "Bobby enabled end-to-end encryption" }
    }

    private fun TestScope.roomEncryptionEnabledViewModel(): EncryptionStateTimelineElementViewModel =
        EncryptionStateTimelineElementViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("user1", "server") to matrixClientMock)
                                    )
                                )
                            }
                            .koin,
                    userId = UserId("user1", "server"),
                ),
            roomId = roomId,
            eventId = eventId,
        )
}
