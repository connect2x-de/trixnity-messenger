package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import isTimelineEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.*
import net.folivo.trixnity.core.model.events.Event.RoomEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent.MegolmEncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction1
import org.kodein.mock.mockFunction4
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class TimelineElementViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var downloadManagerMock: DownloadManager

    private lateinit var roomUserMeMocker: Mocker.Every<Flow<RoomUser?>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

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
                every { matrixClientMock.userId } returns me

                roomUserMeMocker =
                    every { userServiceMock.getById(isEqual(roomId), isEqual(me)) }
                roomUserMeMocker returns MutableStateFlow(roomUser(me, "Me"))
                every { userServiceMock.getById(isEqual(roomId), isEqual(alice)) } returns
                        MutableStateFlow(roomUser(alice, "Alice"))
                every { userServiceMock.getById(isEqual(roomId), isEqual(bob)) } returns
                        MutableStateFlow(roomUser(bob, "Bob"))

                every { userServiceMock.canRedactEvent(isAny(), isAny()) } returns flowOf(true)
                every { userServiceMock.canSendEvent(isAny(), isAny()) } returns flowOf(true)
            }
        }

        should("display a text message") {
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(messageEvent(TextMessageEventContent(body = "Hello World")))
                ),
                eventId = EventId("bla"),
                coroutineContext = coroutineContext,
            )
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.viewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"
            viewModel.sender.first { it == "Me" }

            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val roomUserMutableStateFlow = MutableStateFlow(roomUser(me, "Me"))
            roomUserMeMocker returns roomUserMutableStateFlow
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(messageEvent(TextMessageEventContent(body = "Hello World")))
                ),
                eventId = EventId("bla"),
                coroutineContext = coroutineContext,
            )
            roomUserMutableStateFlow.value = roomUser(me, "Me changed")
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.viewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.sender.first { it == "Me changed" }

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
            timelineEventFlow.value = timelineEvent(messageEvent(TextMessageEventContent(body = "Hello World")))
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.viewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"

            cancelNeverEndingCoroutines()
        }

        should("replace a previously encrypted message with a decrypted message") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(
                    messageEvent(
                        MegolmEncryptedEventContent(
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
                    MegolmEncryptedEventContent(
                        ciphertext = "",
                        senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                        deviceId = "",
                        sessionId = ""
                    )
                ),
                content = Result.success(TextMessageEventContent(body = "Hello World"))
            )
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.viewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"

            cancelNeverEndingCoroutines()
        }

        should("replace any message with its redacted counterpart") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(messageEvent(TextMessageEventContent(body = "Saying things I do not want to say")))
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                    coroutineContext = coroutineContext
                )
            timelineEventFlow.value = timelineEvent(
                messageEvent(RedactedMessageEventContent(eventType = ""))
            )
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.viewModel.first { it != null }
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
                        MegolmEncryptedEventContent(
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
            val subscriberJob = launch { cut.viewModel.collect() }
            testCoroutineScheduler.advanceUntilIdle()

            val viewModel = cut.viewModel.first { it != null }
            require(viewModel is EncryptedMessageViewModel)
            viewModel.waitForDecryption.value shouldBe true

            timelineEventFlow.value =
                timelineEvent(
                    messageEvent(
                        MegolmEncryptedEventContent(
                            ciphertext = "",
                            senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                            deviceId = "",
                            sessionId = ""
                        )
                    ),
                    content = Result.success(UnknownMessageEventContent(JsonObject(mapOf()), "body"))
                )

            testCoroutineScheduler.advanceUntilIdle()
            val viewModelNew = cut.viewModel.first { it != null }
            viewModelNew.shouldBeInstanceOf<NullTimelineElementViewModel>()

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("return special null view model for events that replace another event") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(
                    messageEvent(
                        TextMessageEventContent(
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
            val viewModel = cut.viewModel.first { it != null }
            require(viewModel is NullTimelineElementViewModel)

            cancelNeverEndingCoroutines()
        }
    }

    private fun timelineElementViewModel(
        timelineEventFlow: StateFlow<TimelineEvent?>,
        eventId: EventId,
        canLoadMoreBefore: StateFlow<Boolean> = MutableStateFlow(false),
        canLoadMoreAfter: StateFlow<Boolean> = MutableStateFlow(false),
        isDirect: StateFlow<Boolean> = MutableStateFlow(false),
        coroutineContext: CoroutineContext,
    ) = TimelineElementViewModelImpl(
        viewModelContext = MatrixClientViewModelContextImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            di = koinApplication {
                modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                    single { downloadManagerMock }
                })
            }.koin,
            accountName = "test",
            coroutineContext = coroutineContext
        ),
        selectedRoomId = roomId,
        timelineEventFlow = timelineEventFlow,
        eventId = eventId,
        canLoadMoreBefore = canLoadMoreBefore,
        canLoadMoreAfter = canLoadMoreAfter,
        isDirect = isDirect,
        isReadFlow = MutableStateFlow(false),
        readBy = MutableStateFlow(listOf()),
        shouldShowUnreadMarkerFlow = MutableStateFlow(false),
        onMessageEdited = mockFunction1(mocker),
        onMessageRepliedTo = mockFunction1(mocker),
        onOpenModal = mockFunction4(mocker),
    )

    private fun timelineEvent(
        event: RoomEvent<*>,
        content: Result<RoomEventContent>? = null,
        previousEvent: TimelineEvent? = null
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = event,
            content = content,
            roomId = roomId,
            eventId = EventId(uuid4().toString()),
            previousEventId = previousEvent?.eventId,
            nextEventId = null,
            gap = null,
        )

        mocker.every {
            roomServiceMock.getPreviousTimelineEvent(
                isTimelineEvent(timelineEvent),
                isAny(),
            )
        } returns
                previousEvent?.let { MutableStateFlow(it) }

        return timelineEvent
    }

    private fun messageEvent(content: MessageEventContent, sender: UserId = me) = Event.MessageEvent(
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
        event = Event.StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            UserId(""),
            RoomId(""),
            0L,
            stateKey = ""
        )
    )

}