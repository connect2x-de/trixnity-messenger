package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.isTimelineEvent
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class TimelineElementViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    private val roomId = RoomId("room", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val downloadManagerMock = mock<DownloadManager>()

    private lateinit var roomUserMeMocker: BlockingAnsweringScope<Flow<RoomUser?>>

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(matrixClientMock, roomServiceMock, userServiceMock, downloadManagerMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me

            roomUserMeMocker =
                every { userServiceMock.getById(eq(roomId), eq(me)) }
            roomUserMeMocker returns MutableStateFlow(roomUser(me, "Me"))
            every { userServiceMock.getById(eq(roomId), eq(alice)) } returns
                    MutableStateFlow(roomUser(alice, "Alice"))
            every { userServiceMock.getById(eq(roomId), eq(bob)) } returns
                    MutableStateFlow(roomUser(bob, "Bob"))

            every { userServiceMock.canRedactEvent(any(), any()) } returns flowOf(true)
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)
        }

        should("display a text message") {
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "Hello World")))
                ),
                eventId = EventId("bla"),
                coroutineContext = coroutineContext,
            )
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"
            viewModel.sender.first { it.name == "Me" }

            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val roomUserMutableStateFlow = MutableStateFlow(roomUser(me, "Me"))
            roomUserMeMocker returns roomUserMutableStateFlow
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "Hello World")))
                ),
                eventId = EventId("bla"),
                coroutineContext = coroutineContext,
            )
            roomUserMutableStateFlow.value = roomUser(me, "Me changed")
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.sender.first { it.name == "Me changed" }

            cancelNeverEndingCoroutines()
        }

        should("react to timeline event changes (from 'null', for example, still loading from DB)") {
            val timelineEventFlow = MutableStateFlow<TimelineEvent?>(null)
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                    coroutineContext = coroutineContext
                )
            timelineEventFlow.value =
                timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "Hello World")))
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"

            cancelNeverEndingCoroutines()
        }

        should("replace a previously encrypted message with a decrypted message") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(
                    messageEvent(
                        MegolmEncryptedMessageEventContent(
                            ciphertext = "",
                            senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                            deviceId = "",
                            sessionId = ""
                        )
                    )
                )
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                    coroutineContext = coroutineContext
                )
            timelineEventFlow.value = timelineEvent(
                messageEvent(
                    MegolmEncryptedMessageEventContent(
                        ciphertext = "",
                        senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                        deviceId = "",
                        sessionId = ""
                    )
                ),
                content = Result.success(RoomMessageEventContent.TextBased.Text(body = "Hello World"))
            )
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"

            cancelNeverEndingCoroutines()
        }

        should("replace any message with its redacted counterpart") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "Saying things I do not want to say")))
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                    coroutineContext = coroutineContext
                )
            timelineEventFlow.value = timelineEvent(
                messageEvent(RedactedEventContent(eventType = ""))
            )
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.timelineElementViewModel.first { it != null }
            viewModel should beInstanceOf<RedactedMessageViewModel>()

            cancelNeverEndingCoroutines()
        }

        // The NullTimelineElementViewModel is useful for elements that are encrypted and thus result in a view model and
        // later - after the decryption - we realise it is an element we cannot display. The view model should not return
        // the value 'null' in that case as this is seen as the view model not being ready
        should("return special null view model for unknown event types") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(
                    messageEvent(
                        MegolmEncryptedMessageEventContent(
                            ciphertext = "",
                            senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                            deviceId = "",
                            sessionId = "",
                        )
                    )
                )
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                    coroutineContext = coroutineContext
                )
            val subscriberJob = launch { cut.timelineElementViewModel.collect() }
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is EncryptedMessageViewModel)
            viewModel.waitForDecryption.value shouldBe true

            timelineEventFlow.value =
                timelineEvent(
                    messageEvent(
                        MegolmEncryptedMessageEventContent(
                            ciphertext = "",
                            senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                            deviceId = "",
                            sessionId = ""
                        )
                    ),
                    content = Result.success(UnknownEventContent(JsonObject(mapOf()), "body"))
                )

            testCoroutineScheduler.advanceUntilIdle()
            val viewModelNew = cut.timelineElementViewModel.first { it != null }
            viewModelNew.shouldBeInstanceOf<NullTimelineElementViewModel>()

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("return special null view model for events that replace another event") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(
                    messageEvent(
                        RoomMessageEventContent.TextBased.Text(
                            body = "I am replacing something else",
                            relatesTo = RelatesTo.Replace(eventId = EventId("I am replaced"))
                        )
                    )
                )
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                    coroutineContext = coroutineContext
                )
            testCoroutineScheduler.advanceUntilIdle()

            testCoroutineScheduler.advanceUntilIdle()
            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is NullTimelineElementViewModel)

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun timelineElementViewModel(
        timelineEventFlow: StateFlow<TimelineEvent?>,
        eventId: EventId,
        canLoadMoreBefore: StateFlow<Boolean> = MutableStateFlow(false),
        canLoadMoreAfter: StateFlow<Boolean> = MutableStateFlow(false),
        isDirect: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ): TimelineElementHolderViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return TimelineElementHolderViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)) +
                                module {
                                    single { downloadManagerMock }
                                })
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            key = eventId.full,
            selectedRoomId = roomId,
            timelineEventFlow = timelineEventFlow,
            eventId = eventId,
            canLoadMoreBefore = canLoadMoreBefore,
            canLoadMoreAfter = canLoadMoreAfter,
            isDirect = isDirect,
            isReadFlow = MutableStateFlow(false),
            readBy = MutableStateFlow(listOf()),
            shouldShowUnreadMarkerFlow = MutableStateFlow(false),
            onMessageEdited = mock(),
            onMessageRepliedTo = mock(),
            onMessageReportTo = mock(),
            onOpenModal = mock(),
            onOpenMention = mock(),
        )
    }

    private fun timelineEvent(
        event: RoomEvent<*>,
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

    private fun messageEvent(content: MessageEventContent, sender: UserId = me) = MessageEvent(
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
        event = StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            UserId(""),
            RoomId(""),
            0L,
            stateKey = ""
        )
    )

}
