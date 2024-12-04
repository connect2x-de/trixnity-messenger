package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class EncryptionStateTimelineElementViewModelTest : ShouldSpec() {

    val roomId = RoomId("room", "server")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

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
            senderName.value = "Bob"
            every { userServiceMock.getById(roomId, sender) } returns senderName.map {
                RoomUser(
                    roomId, sender, it, StateEvent(
                        content = MemberEventContent(membership = Membership.JOIN),
                        id = eventId,
                        sender = sender,
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = "",
                    )
                )
            }
            isDirect.value = false
            every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it) }

            every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(
                TimelineEvent(
                    event = StateEvent(
                        EncryptionEventContent(),
                        id = eventId,
                        sender = sender,
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = ""
                    ),
                    previousEventId = null,
                    nextEventId = null,
                    gap = null,
                )
            )
        }

        should("display who enabled to end-to-end encryption") {
            val cut = roomEncryptionEnabledViewModel()
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()
            cut.changeMessage.value shouldBe "Bob enabled end-to-end encryption"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val cut = roomEncryptionEnabledViewModel()
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()
            cut.changeMessage.value shouldBe "Bob enabled end-to-end encryption"

            senderName.value = "Bobby"
            advanceUntilIdle()
            cut.changeMessage.value shouldBe "Bobby enabled end-to-end encryption"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun TestScope.roomEncryptionEnabledViewModel(): EncryptionStateTimelineElementViewModel =
        EncryptionStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
                    )
                }.koin,
                userId = UserId("user1", "server"),
            ),
            roomId = roomId,
            eventId = eventId,
        )

}
