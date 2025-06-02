package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.util.ImmediateDispatcherElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class VerificationDoneTimelineElementViewModelTest {
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val initialsMock = mock<Initials>()

    private val userId = UserId("test", "server")
    private val otherUserId = UserId("other", "server")
    private val roomId = RoomId("room", "localhost")

    init {
        resetMocks(matrixClientMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns userId

        every { initialsMock.compute(any()) } calls { (name: String) -> name }
        every { userServiceMock.getById(roomId, userId) } returns flowOf(
            RoomUser(
                roomId,
                userId,
                "user",
                event = ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId("999"),
                    roomId = roomId,
                    sender = otherUserId,
                    stateKey = userId.full,
                    originTimestamp = 0,
                )
            )
        )
        every { userServiceMock.getById(roomId, otherUserId) } returns flowOf(
            RoomUser(
                roomId,
                otherUserId,
                "otherUser",
                event = ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId("999"),
                    roomId = roomId,
                    sender = userId,
                    stateKey = otherUserId.full,
                    originTimestamp = 0,
                )
            )
        )
    }

    @Test
    fun `done event is own event - ccnnbvcoriginal request by other user`() = runTest {
        val verificationDoneEventContent = VerificationDoneEventContent(
            relatesTo = RelatesTo.Reference(EventId("2")),
            transactionId = null,
        )
        every { roomServiceMock.getTimelineEvent(roomId, EventId("1")) } returns flowOf(
            TimelineEvent(
                event = ClientEvent.RoomEvent.MessageEvent(
                    content = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
                        ciphertext = "",
                        sessionId = "",
                    ),
                    id = EventId("1"),
                    sender = userId,
                    roomId = roomId,
                    originTimestamp = 0,
                ),
                content = Result.success(verificationDoneEventContent),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )
        every { roomServiceMock.getTimelineEvent(roomId, EventId("2")) } returns flowOf(
            TimelineEvent(
                event = ClientEvent.RoomEvent.MessageEvent(
                    content = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
                        ciphertext = "",
                        sessionId = "",
                    ),
                    id = EventId("2"),
                    sender = otherUserId,
                    roomId = roomId,
                    originTimestamp = 0,
                ),
                content = Result.success(
                    RoomMessageEventContent.VerificationRequest(
                        fromDevice = "device2",
                        to = userId,
                        methods = setOf(),
                    )
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )

        val cut = verificationDoneEventContentTimelineElementViewModel(verificationDoneEventContent)
        delay(100.milliseconds)

        cut.isOwn.value shouldBe true
        cut.verificationStartedBy.value?.name shouldBe "otherUser"
        cut.message.lowercase() shouldContain "successful"
    }

    @Test
    fun `done event is not our own event - original request by current user`() = runTest {
        val verificationDoneEventContent = VerificationDoneEventContent(
            relatesTo = RelatesTo.Reference(EventId("2")),
            transactionId = null,
        )
        every { roomServiceMock.getTimelineEvent(roomId, EventId("1")) } returns flowOf(
            TimelineEvent(
                event = ClientEvent.RoomEvent.MessageEvent(
                    content = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
                        ciphertext = "",
                        sessionId = "",
                    ),
                    id = EventId("1"),
                    sender = otherUserId,
                    roomId = roomId,
                    originTimestamp = 0,
                ),
                content = Result.success(verificationDoneEventContent),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )
        every { roomServiceMock.getTimelineEvent(roomId, EventId("2")) } returns flowOf(
            TimelineEvent(
                event = ClientEvent.RoomEvent.MessageEvent(
                    content = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
                        ciphertext = "",
                        sessionId = "",
                    ),
                    id = EventId("2"),
                    sender = userId,
                    roomId = roomId,
                    originTimestamp = 0,
                ),
                content = Result.success(
                    RoomMessageEventContent.VerificationRequest(
                        fromDevice = "device2",
                        to = userId,
                        methods = setOf(),
                    )
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )

        val cut = verificationDoneEventContentTimelineElementViewModel(verificationDoneEventContent)
        delay(100.milliseconds)

        cut.isOwn.value shouldBe false
        cut.verificationStartedBy.value?.name shouldBe "user"
        cut.message.lowercase() shouldContain "successful"
    }

    fun TestScope.verificationDoneEventContentTimelineElementViewModel(verificationDoneEventContent: VerificationDoneEventContent): VerificationDoneTimelineElementViewModelImpl {
        val result =
            VerificationDoneTimelineElementViewModelImpl(
                viewModelContext = MatrixClientViewModelContextImpl(
                    di = koinApplication {
                        modules(
                            createTestDefaultTrixnityMessengerModules(
                                mapOf(userId to matrixClientMock)
                            ) + module {
                                single { initialsMock }
                            }
                        )
                    }.koin,
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    userId = userId,
                    coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher),
                ),
                content = verificationDoneEventContent,
                roomId = roomId,
                eventIdOrTransactionId = EventIdOrTransactionId(eventId = EventId("1")),
            )

        backgroundScope.launch { result.isOwn.collect() }
        backgroundScope.launch { result.verificationStartedBy.collect() }

        return result
    }

}
