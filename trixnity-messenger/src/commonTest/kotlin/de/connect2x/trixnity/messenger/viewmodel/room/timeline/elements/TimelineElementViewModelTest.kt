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
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.instanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomOutboxMessage
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
import kotlin.time.Duration.Companion.milliseconds

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

            every { roomServiceMock.getOutbox(eq(roomId)) } returns flowOf(listOf())

            every { userServiceMock.canRedactEvent(any(), any()) } returns flowOf(true)
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)
        }

        should("display a text message") {
            val content = RoomMessageEventContent.TextBased.Text(body = "Hello World")
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(messageEvent(content), Result.success(content))
                ),
                eventId = EventId("bla"),
            )

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"
            viewModel.sender.first { it.name == "Me" }

            cancelNeverEndingCoroutines()
        }

        should("react to username changes") {
            val roomUserMutableStateFlow = MutableStateFlow(roomUser(me, "Me"))
            roomUserMeMocker returns roomUserMutableStateFlow
            val content = RoomMessageEventContent.TextBased.Text(body = "Hello World")
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(messageEvent(content), Result.success(content))
                ),
                eventId = EventId("bla"),
            )
            roomUserMutableStateFlow.value = roomUser(me, "Me changed")

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
                )
            val content = RoomMessageEventContent.TextBased.Text(body = "Hello World")
            timelineEventFlow.value =
                timelineEvent(messageEvent(content), Result.success(content))

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
                    eventId = EventId("bla")
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
            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is TextMessageViewModel)
            viewModel.fallbackMessage shouldBe "Hello World"

            cancelNeverEndingCoroutines()
        }

        should("replace any message that is either a room-message or encrypted with its redacted counterpart") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "Saying things I do not want to say")))
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                )
            val redactedEventContent = RedactedEventContent(eventType = "m.room.message")
            timelineEventFlow.value =
                timelineEvent(messageEvent(redactedEventContent), Result.success(redactedEventContent))

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
                )
            val subscriberJob = launch { cut.timelineElementViewModel.collect() }

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

            cut.timelineElementViewModel.first { it != null && it is NullTimelineElementViewModel }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("return special null view model for events that replace another event") {
            val content = RoomMessageEventContent.TextBased.Text(
                body = "I am replacing something else",
                relatesTo = RelatesTo.Replace(eventId = EventId("I am replaced"))
            )
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(messageEvent(content), Result.success(content))
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                )

            val viewModel = cut.timelineElementViewModel.first { it != null }
            require(viewModel is NullTimelineElementViewModel)

            cancelNeverEndingCoroutines()
        }

        should("return special null view model for redaction events that are not room messages or encrypted") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "Saying things I do not want to say")))
            )
            val cut =
                timelineElementViewModel(
                    timelineEventFlow = timelineEventFlow,
                    eventId = EventId("bla"),
                )
            val redactedEventContent = RedactedEventContent(eventType = "not a message or encrypted")
            timelineEventFlow.value =
                timelineEvent(messageEvent(redactedEventContent), Result.success(redactedEventContent))

            val viewModel = cut.timelineElementViewModel.first { it != null }
            viewModel should beInstanceOf<NullTimelineElementViewModel>()



            cancelNeverEndingCoroutines()
        }

        should("wait for 400 milliseconds when message cannot be decrypted (content == `null`)") {
            val cut = timelineElementViewModel(
                timelineEventFlow = MutableStateFlow(
                    timelineEvent(
                        messageEvent(
                            MegolmEncryptedMessageEventContent(
                                ciphertext = "",
                                senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                                deviceId = "",
                                sessionId = ""
                            )
                        ),
                        content = null, // not yet decrypted
                    )
                ),
                eventId = EventId("bla"),
            )
            val subscriberJob = launch { cut.timelineElementViewModel.collect() }

            continually(300.milliseconds) {
                cut.timelineElementViewModel.value should beNull()
            }
            eventually(600.milliseconds) {
                cut.timelineElementViewModel.value shouldNot beNull()
                cut.timelineElementViewModel.value shouldBe instanceOf<EncryptedMessageViewModel>()
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("take message content from outbox if available") {
            val timelineEventFlow = MutableStateFlow(
                timelineEvent(messageEvent(RoomMessageEventContent.TextBased.Text(body = "original text")))
            )
            val outbox = MutableStateFlow<List<Flow<RoomOutboxMessage<*>>>>(listOf(
                flowOf(outboxMessageReplace("dummy 1","first replace", EventId("bla"))),
                flowOf(outboxMessageReplace("dummy 2","second replace", EventId("bla"))),
                flowOf(outboxMessageReplace("dummy 3","third replace", EventId("bla"))),
                flowOf(outboxMessageReplace("dummy 4","other replace", EventId("otherBla"))),
            ))
            every { roomServiceMock.getOutbox(eq(roomId)) } returns outbox
            val cut = timelineElementViewModel(
                timelineEventFlow = timelineEventFlow,
                eventId = EventId("bla"),
            )

            cut.timelineElementViewModel.filterNotNull().first {
                require(it is TextMessageViewModel)
                it.message == "third replace"
            }
        }
    }

    private suspend fun timelineElementViewModel(
        timelineEventFlow: StateFlow<TimelineEvent?>,
        eventId: EventId,
        canLoadMoreBefore: StateFlow<Boolean> = MutableStateFlow(false),
        canLoadMoreAfter: StateFlow<Boolean> = MutableStateFlow(false),
        isDirect: StateFlow<Boolean> = MutableStateFlow(false),
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
            ),
            key = eventId.full,
            selectedRoomId = roomId,
            timelineEventFlow = timelineEventFlow,
            eventId = eventId,
            canLoadMoreBefore = canLoadMoreBefore,
            canLoadMoreAfter = canLoadMoreAfter,
            isDirect = isDirect,
            isReadFlow = MutableStateFlow(false),
            readBy = MutableStateFlow(emptyList()),
            _reactions = MutableStateFlow(emptyMap()),
            shouldShowUnreadMarkerFlow = MutableStateFlow(false),
            onMessageEdited = mock(),
            onMessageRepliedTo = mock(),
            onMessageReportTo = mock(),
            onOpenMedia = mock(),
            onOpenMention = mock(),
        )
    }

    private fun timelineEvent(
        event: RoomEvent<*>,
        content: Result<RoomEventContent>? = null,
        previousEvent: TimelineEvent = TimelineEvent(
            event = messageEvent(RoomMessageEventContent.TextBased.Text("")),
            content = null,
            previousEventId = null,
            nextEventId = event.id,
            gap = null
        ),
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = event,
            content = content,
            previousEventId = previousEvent.eventId,
            nextEventId = null,
            gap = null,
        )

        every {
            roomServiceMock.getPreviousTimelineEvent(
                isTimelineEvent(timelineEvent),
                any(),
            )
        } returns MutableStateFlow(previousEvent)

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

    private fun outboxMessageReplace(tid: String, text: String, replaceId: EventId) = RoomOutboxMessage(
        roomId = roomId,
        transactionId = tid,
        createdAt = Instant.fromEpochMilliseconds(0),
        content = RoomMessageEventContent.TextBased.Text(
            text,
            relatesTo = RelatesTo.Replace(
                eventId = replaceId,
                newContent = RoomMessageEventContent.TextBased.Text(body = text)
            )
        ),
    )

}
