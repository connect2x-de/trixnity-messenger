package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.RoomName
import de.connect2x.trixnity.messenger.viewmodel.RoomNameElement
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.UnknownMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.*
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomListElementViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    @Mock
    lateinit var userPresenceMock: UserPresence

    @Mock
    lateinit var roomNameMock: RoomName

    @Mock
    lateinit var clock: Clock

    private val roomId = RoomId("room", "localhost")
    private val roomId1 = RoomId("room1", "localhost")
    private val roomId2 = RoomId("room2", "localhost")
    private val roomId3 = RoomId("room3", "localhost")
    private val roomId4 = RoomId("room4", "localhost")

    private val me = UserId("me", "server")
    private val user2 = UserId("user2", "server")
    private val user3 = UserId("user3", "server")

    private val user2Flow = MutableStateFlow(
        RoomUser(
            roomId,
            userId = user2,
            name = "User 2",
            event = memberEvent()
        )
    )

    lateinit var roomByIdMocker: Mocker.Every<Flow<Room?>>

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
                every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.RUNNING)
                every { matrixClientMock.displayName } returns MutableStateFlow("")
                every { matrixClientMock.avatarUrl } returns MutableStateFlow(null)
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock

                every {
                    roomServiceMock.getState(
                        isEqual(roomId),
                        isEqual(CreateEventContent::class),
                        isAny()
                    )
                } returns flowOf(null)
                every {
                    roomServiceMock.getState(
                        isEqual(roomId),
                        isEqual(MemberEventContent::class),
                        isAny()
                    )
                } returns flowOf(null)
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(null)

                every { userServiceMock.getById(isEqual(roomId1), isEqual(me)) } returns
                        MutableStateFlow(
                            RoomUser(
                                roomId1,
                                userId = me,
                                name = "Me",
                                event = memberEvent(),
                            )
                        )
                every { userServiceMock.getById(isEqual(roomId2), isEqual(me)) } returns
                        MutableStateFlow(
                            RoomUser(
                                roomId2,
                                userId = me,
                                name = "Me",
                                event = memberEvent(),
                            )
                        )
                every { userServiceMock.getById(isEqual(roomId1), isEqual(user2)) } returns user2Flow
                every { userServiceMock.getById(isAny(), isEqual(user2)) } returns MutableStateFlow(
                    RoomUser(RoomId(""), UserId(""), "User2", memberEvent())
                )
                every { userServiceMock.getById(isEqual(roomId1), isEqual(user3)) } returns
                        MutableStateFlow(
                            RoomUser(
                                roomId1,
                                userId = user3,
                                name = "User 3",
                                event = memberEvent(),
                            )
                        )

                every {
                    roomServiceMock.getState(
                        isAny(),
                        isEqual(AvatarEventContent::class),
                        isAny()
                    )
                } returns MutableStateFlow(null)

                roomByIdMocker = mocker.every { roomServiceMock.getById(roomId) }
                roomByIdMocker returns
                        MutableStateFlow(
                            Room(
                                roomId,
                                isDirect = false,
                                unreadMessageCount = 0,
                                membership = Membership.INVITE,
                                membersLoaded = false
                            )
                        )

                every { userPresenceMock.presentEventContentFlow(isEqual(matrixClientMock), isAny()) } returns
                        MutableStateFlow(null)

                every { roomNameMock.getRoomNameElement(isAny<RoomId>(), isEqual(matrixClientMock)) } returns
                        flowOf(RoomNameElement("RoomName"))
                every { clock.now() } returns Instant.parse("2021-11-03T15:00:00Z")
            }
        }

        should("show time for last messages that were sent today and the date for messages from yesterday onwards") {
            val room1LastEvent = timelineEvent(EventId("event1"), clock.now().minus(1.seconds))
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(room1LastEvent))
            val room2LastEvent = timelineEvent(EventId("event2"), clock.now().minus(1.minutes))
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId2), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(room2LastEvent))
            val room3LastEvent = timelineEvent(EventId("event3"), clock.now().minus(1.hours))
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId3), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(room3LastEvent))
            val room4LastEvent = timelineEvent(EventId("event4"), clock.now().minus(1.days))
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId4), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(room4LastEvent))

            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val room3 = Room(roomId3)
            val room4 = Room(roomId4)
            with(mocker) {
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock.getById(roomId4) } returns MutableStateFlow(room4)
            }

            roomListElementViewModel(
                roomId1,
                coroutineContext
            ).time.first { it == "15:59" }
            roomListElementViewModel(
                roomId2,
                coroutineContext
            ).time.first { it == "15:59" }
            roomListElementViewModel(
                roomId3,
                coroutineContext
            ).time.first { it == "15:00" }
            roomListElementViewModel(
                roomId4,
                coroutineContext
            ).time.first { it == "02.11.21" }

            cancelNeverEndingCoroutines()
        }

        should("display last message for text messages and decrypted text messages") {
            val eventId1 = EventId("\$event1")
            val eventId2 = EventId("\$event2")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = true,
            )
            val room2 = Room(
                roomId2,
                lastEventId = eventId2,
                isDirect = true
            )
            with(mocker) {
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                        MutableStateFlow(MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!")))
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId2), isAny()) } returns
                        MutableStateFlow(
                            MutableStateFlow(
                                timelineEventEncrypted(
                                    eventId2,
                                    Clock.System.now().minus(1.days),
                                    "What's up?"
                                )
                            )
                        )
            }

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "Hello!" }
            roomListElementViewModel(roomId2, coroutineContext).lastMessage.first { it == "What's up?" }

            cancelNeverEndingCoroutines()
        }

        should("search for the last known message and display that instead of the last message of type UnknownMessageEventContent") {
            val eventId1 = EventId("\$event1")
            val eventId2 = EventId("\$event2")
            val room1 = Room(
                roomId1,
                lastEventId = eventId2,
                isDirect = true,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            val reaction = TimelineEvent(
                Event.MessageEvent(
                    UnknownMessageEventContent(
                        raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))),
                        eventType = "m.reaction"
                    ),
                    eventId2,
                    user2,
                    roomId1,
                    Clock.System.now().toEpochMilliseconds(),
                ),
                previousEventId = eventId1,
                nextEventId = null,
                gap = null,
            )
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(reaction))
            mocker.every {
                roomServiceMock.getPreviousTimelineEvent(
                    isEqual(reaction),
                    isAny(),
                )
            } returns
                    MutableStateFlow(
                        TimelineEvent(
                            Event.MessageEvent(
                                TextMessageEventContent(body = "Hola"),
                                eventId1,
                                user2,
                                roomId1,
                                1000L,
                            ),
                            previousEventId = null,
                            nextEventId = null,
                            gap = null,
                        )
                    )

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "Hola" }

            cancelNeverEndingCoroutines()
        }

        should("search for the last known message and display that instead of the last message is an encrypted UnknownMessageEventContent") {
            val eventId1 = EventId("\$event1")
            val eventId2 = EventId("\$event2")
            val room1 = Room(
                roomId1,
                lastEventId = eventId2,
                isDirect = true,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            val reaction = TimelineEvent(
                Event.MessageEvent(
                    EncryptedEventContent.MegolmEncryptedEventContent(
                        ciphertext = "78fd687dfdsf",
                        senderKey = Key.Curve25519Key(value = ""),
                        deviceId = "deviceId",
                        sessionId = "sessionId"
                    ),
                    eventId2,
                    user2,
                    roomId1,
                    1000L,
                ),
                Result.success(
                    UnknownMessageEventContent(
                        raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))),
                        eventType = "m.reaction"
                    ),
                ),
                previousEventId = eventId1,
                nextEventId = null,
                gap = null,
            )
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(reaction))
            mocker.every {
                roomServiceMock.getPreviousTimelineEvent(
                    isEqual(reaction),
                    isAny(),
                )
            } returns
                    MutableStateFlow(
                        TimelineEvent(
                            Event.MessageEvent(
                                TextMessageEventContent(body = "Hola"),
                                eventId1,
                                user2,
                                roomId1,
                                1000L,
                            ),
                            previousEventId = null,
                            nextEventId = null,
                            gap = null,
                        )
                    )

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "Hola" }

            cancelNeverEndingCoroutines()
        }

        should("display the text of the last text message even when there are multiple state events since then") {
            val lastEventNumber = 10
            val lastEventId = EventId("\$event-$lastEventNumber")
            val room = Room(
                roomId1,
                lastEventId = lastEventId,
                isDirect = true,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room)
            val lastTimelineEvent = TimelineEvent(
                Event.StateEvent(
                    NameEventContent("new name"),
                    lastEventId,
                    user2,
                    roomId1,
                    Clock.System.now().toEpochMilliseconds(),
                    stateKey = ""
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(lastTimelineEvent))
            var currentTimelineEvent = lastTimelineEvent
            (lastEventNumber - 1 downTo 0).forEach { i ->
                val eventId = EventId("\$event-$i")
                val previousTimelineEvent = if (i > 1) {
                    TimelineEvent(
                        Event.StateEvent(
                            NameEventContent("new name"),
                            eventId,
                            user2,
                            roomId1,
                            1000L,
                            stateKey = "",
                        ),
                        previousEventId = null,
                        nextEventId = null,
                        gap = null,
                    )
                } else if (i > 0) {
                    TimelineEvent(
                        Event.MessageEvent(
                            TextMessageEventContent("Hello!"),
                            eventId,
                            user2,
                            roomId1,
                            1000L,
                        ),
                        previousEventId = null,
                        nextEventId = null,
                        gap = null,
                    )
                } else null
                mocker.every {
                    roomServiceMock.getPreviousTimelineEvent(
                        isEqual(currentTimelineEvent),
                        isAny(),
                    )
                } returns previousTimelineEvent?.let { MutableStateFlow(it) }
                if (previousTimelineEvent != null) currentTimelineEvent = previousTimelineEvent
            }

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "Hello!" }

            cancelNeverEndingCoroutines()
        }

        should("show 'you' as the author in the last message in a direct room when last message was sent by me") {
            val eventId1 = EventId("\$event1")
            val eventId2 = EventId("\$event2")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = true,
            )
            val room2 = Room(
                roomId2,
                lastEventId = eventId2,
                isDirect = true
            )
            with(mocker) {
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                        MutableStateFlow(
                            MutableStateFlow(
                                timelineEvent(
                                    eventId1,
                                    Clock.System.now(),
                                    "Hello!",
                                    sender = me,
                                )
                            )
                        )
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId2), isAny()) } returns
                        MutableStateFlow(
                            MutableStateFlow(
                                timelineEventEncrypted(
                                    eventId2,
                                    Clock.System.now().minus(1.days),
                                    "What's up?",
                                    sender = me,
                                )
                            )
                        )
            }

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "you: Hello!" }
            roomListElementViewModel(roomId2, coroutineContext).lastMessage.first { it == "you: What's up?" }

            cancelNeverEndingCoroutines()
        }

        should("cap the search for the last text message at 100 non-text messages") {
            val lastEventNumber = 110
            val lastEventId = EventId("\$event-$lastEventNumber")
            val room = Room(
                roomId1,
                lastEventId = lastEventId,
                isDirect = true,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room)
            val lastTimelineEvent = TimelineEvent(
                Event.StateEvent(
                    NameEventContent("new name"),
                    lastEventId,
                    user2,
                    roomId1,
                    Clock.System.now().toEpochMilliseconds(),
                    stateKey = ""
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    flowOf(flowOf(lastTimelineEvent))
            var currentTimelineEvent = lastTimelineEvent
            (lastEventNumber - 1 downTo 0).forEach { i ->
                val eventId = EventId("\$event-$i")
                val previousTimelineEvent = if (i > 1) {
                    TimelineEvent(
                        Event.StateEvent(
                            NameEventContent("new name"),
                            eventId,
                            user2,
                            roomId1,
                            Clock.System.now().toEpochMilliseconds(),
                            stateKey = "",
                        ),
                        previousEventId = null,
                        nextEventId = null,
                        gap = null,
                    )
                } else if (i > 0) {
                    TimelineEvent(
                        Event.MessageEvent(
                            TextMessageEventContent("Hello!"),
                            eventId,
                            user2,
                            roomId1,
                            Clock.System.now().toEpochMilliseconds(),
                        ),
                        previousEventId = null,
                        nextEventId = null,
                        gap = null,
                    )
                } else null
                mocker.every {
                    roomServiceMock.getPreviousTimelineEvent(
                        isEqual(currentTimelineEvent),
                        isAny(),
                    )
                } returns previousTimelineEvent?.let { flowOf(it) }
                if (previousTimelineEvent != null) currentTimelineEvent = previousTimelineEvent
            }
            mocker.every {
                roomServiceMock.getState(isEqual(roomId1), isEqual(CreateEventContent::class), isAny())
            } returns flowOf(
                Event.StateEvent(
                    CreateEventContent(
                        creator = user2,
                        federate = false,
                        roomVersion = "6",
                        type = CreateEventContent.RoomType.Room,
                    ),
                    EventId("creation"),
                    sender = user2,
                    roomId1,
                    Instant.parse("2021-11-02T15:00:00Z").toEpochMilliseconds(),
                    stateKey = "",
                )
            )

            val cut = roomListElementViewModel(roomId1, coroutineContext)
            combine(cut.lastMessage, cut.time) { lastMessage, time ->
                lastMessage == "" && time == "02.11.21"
            }.first { it }

            cancelNeverEndingCoroutines()
        }

        should("display a special text for file based messages (not encrypted and decrypted)") {
            val eventId1 = EventId("\$event1")
            val eventId2 = EventId("\$event2")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = true,
            )
            val room2 = Room(
                roomId2,
                lastEventId = eventId2,
                isDirect = true,
            )
            with(mocker) {
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                        MutableStateFlow(
                            MutableStateFlow(
                                TimelineEvent(
                                    Event.MessageEvent(
                                        RoomMessageEventContent.ImageMessageEventContent(""),
                                        eventId1,
                                        user2,
                                        roomId1,
                                        Clock.System.now().toEpochMilliseconds(),
                                    ),
                                    previousEventId = null,
                                    nextEventId = null,
                                    gap = null,
                                )
                            )
                        )
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId2), isAny()) } returns
                        MutableStateFlow(
                            MutableStateFlow(
                                TimelineEvent(
                                    Event.MessageEvent(
                                        EncryptedEventContent.MegolmEncryptedEventContent(
                                            "",
                                            Key.Curve25519Key(value = ""),
                                            deviceId = "",
                                            sessionId = "",
                                        ),
                                        eventId2,
                                        user2,
                                        roomId2,
                                        Clock.System.now().toEpochMilliseconds(),
                                    ),
                                    Result.success(
                                        RoomMessageEventContent.VideoMessageEventContent(""),
                                    ),
                                    previousEventId = null,
                                    nextEventId = null,
                                    gap = null,
                                )
                            )
                        )
            }

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "Image" }
            roomListElementViewModel(roomId2, coroutineContext).lastMessage.first { it == "Video" }

            cancelNeverEndingCoroutines()
        }

        should("change the RoomElement when the last message in a room changes") {
            val eventId1 = EventId("\$event1")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = true,
            )
            val lastMessageEvent =
                MutableStateFlow(MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!")))

            with(mocker) {
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every {
                    roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny())
                } returns lastMessageEvent
            }

            val cut = roomListElementViewModel(roomId1, coroutineContext)

            cut.lastMessage.first { it == "Hello!" }

            val eventId2 = EventId("\$event2")
            lastMessageEvent.value = MutableStateFlow(timelineEvent(eventId2, Clock.System.now(), "A new message."))
            cut.lastMessage.first { it == "A new message." }

            cancelNeverEndingCoroutines()
        }

        should("change the RoomElement when the content of the last message changes") {
            val eventId1 = EventId("\$event1")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = true,
            )
            val messageStateFlow = MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!"))
            with(mocker) {
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                        MutableStateFlow(messageStateFlow)
            }

            val cut = roomListElementViewModel(roomId1, coroutineContext)

            cut.lastMessage.first { it == "Hello!" }

            messageStateFlow.value = timelineEvent(eventId1, Clock.System.now(), "Hello! I have more.")
            cut.lastMessage.first { it == "Hello! I have more." }

            cancelNeverEndingCoroutines()
        }
        should("show the author of a message in a room with multiple users") {
            val eventId1 = EventId("\$event1")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = false,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!")))

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "User 2: Hello!" }

            cancelNeverEndingCoroutines()
        }

        should("show the author of a message in a room with multiple users if the message was by me") {
            val eventId1 = EventId("\$event1")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = false,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(
                        MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!", sender = me))
                    )
            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "you: Hello!" }

            cancelNeverEndingCoroutines()
        }

        should("not show the author of a message in a direct room even it was not by me") {
            val eventId1 = EventId("\$event1")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = true,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(
                        MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!"))
                    )

            roomListElementViewModel(roomId1, coroutineContext).lastMessage.first { it == "Hello!" }

            cancelNeverEndingCoroutines()
        }

        should("change the username in the last message in case the username changes") {
            val eventId1 = EventId("\$event1")
            val room1 = Room(
                roomId1,
                lastEventId = eventId1,
                isDirect = false,
            )
            mocker.every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            mocker.every { roomServiceMock.getLastTimelineEvent(isEqual(roomId1), isAny()) } returns
                    MutableStateFlow(
                        MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!"))
                    )

            val cut = roomListElementViewModel(roomId1, coroutineContext)

            cut.lastMessage.first { it == "User 2: Hello!" }
            user2Flow.value = RoomUser(roomId1, user2, "User 2 new", memberEvent())
            cut.lastMessage.first { it == "User 2 new: Hello!" }

            cancelNeverEndingCoroutines()
        }
    }

    private fun roomListElementViewModel(
        roomId: RoomId = this.roomId,
        coroutineContext: CoroutineContext
    ): RoomListElementViewModelImpl {
        val di = koinApplication {
            modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                single { userPresenceMock }
                single { roomNameMock }
                single { clock }
            })
        }.koin
        di.get<I18n>().setCurrentLang("en")
        return RoomListElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                accountName = "test",
                coroutineContext = coroutineContext
            ),
            roomId,
        )
    }

    private fun timelineEvent(eventId: EventId, sentAt: Instant, body: String = "", sender: UserId = user2) =
        TimelineEvent(
            event = Event.MessageEvent(
                content = TextMessageEventContent(body),
                id = eventId,
                sender = sender,
                roomId = roomId1,
                originTimestamp = sentAt.toEpochMilliseconds(),
                unsigned = null
            ),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    private fun timelineEventEncrypted(eventId: EventId, sentAt: Instant, body: String = "", sender: UserId = user2) =
        TimelineEvent(
            event = Event.MessageEvent(
                content = EncryptedEventContent.MegolmEncryptedEventContent(
                    ciphertext = "",
                    senderKey = Key.Curve25519Key(value = ""),
                    deviceId = "",
                    sessionId = ""
                ),
                id = eventId,
                sender = sender,
                roomId = roomId1,
                originTimestamp = sentAt.toEpochMilliseconds(),
                unsigned = null
            ),
            content = Result.success(TextMessageEventContent(body)),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    private fun memberEvent() = Event.StateEvent(
        content = MemberEventContent(membership = Membership.JOIN),
        id = EventId(""),
        sender = me,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = ""
    )

}