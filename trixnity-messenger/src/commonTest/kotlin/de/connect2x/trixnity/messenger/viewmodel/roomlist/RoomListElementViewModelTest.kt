package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviter
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.KeyValue
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RoomListElementViewModelTest {
    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val userPresenceMock = mock<UserPresence>()

    val roomNameMock = mock<RoomName>()

    val userBlockingMock = mock<UserBlocking>()

    val roomInviter = mock<RoomInviter>()

    val clock = mock<Clock>()

    private val onRoomSelectedMock = mock<Function0<Unit>>()

    private val roomId = RoomId("room", "localhost")
    private val roomId1 = RoomId("room1", "localhost")
    private val roomId2 = RoomId("room2", "localhost")
    private val roomId3 = RoomId("room3", "localhost")
    private val roomId4 = RoomId("room4", "localhost")

    private val me = UserId("test", "server")
    private val user2 = UserId("user2", "server")
    private val user3 = UserId("user3", "server")

    private val user2Flow = MutableStateFlow(
        RoomUser(
            roomId, userId = user2, name = "User 2", event = memberEvent()
        )
    )

    var roomByIdMocker: BlockingAnsweringScope<Flow<Room?>>

    init {
        resetMocks(
            matrixClientMock,
            matrixClientServerApiClientMock,
            roomsApiClientMock,
            usersApiClientMock,
            userPresenceMock,
            roomNameMock,
            roomInviter,
            userBlockingMock,
            clock,
            onRoomSelectedMock,
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns me
        every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.RUNNING)
        every { matrixClientMock.displayName } returns MutableStateFlow("")
        every { matrixClientMock.avatarUrl } returns MutableStateFlow(null)
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        everySuspend { userBlockingMock.blockUser(any(), any(), any(), any()) } returns Unit

        every {
            roomServiceMock.getState(
                eq(roomId), eq(CreateEventContent::class), any()
            )
        } returns flowOf(null)
        every {
            roomServiceMock.getState(
                eq(roomId), eq(MemberEventContent::class), any()
            )
        } returns flowOf(null)
        every { roomServiceMock.getTimelineEvent(eq(roomId), any()) } returns MutableStateFlow(null)
        every {
            roomServiceMock.getState(
                any(), eq(JoinRulesEventContent::class), any()
            )
        } returns MutableStateFlow(
            StateEvent(
                content = JoinRulesEventContent(
                    joinRule = JoinRulesEventContent.JoinRule.Private
                ),
                EventId("1"),
                me,
                roomId,
                0L,
                stateKey = "",
            )
        )

        every { userServiceMock.getById(eq(roomId1), eq(me)) } returns MutableStateFlow(
            RoomUser(
                roomId1,
                userId = me,
                name = "Me",
                event = memberEvent(),
            )
        )
        every { userServiceMock.getById(eq(roomId2), eq(me)) } returns MutableStateFlow(
            RoomUser(
                roomId2,
                userId = me,
                name = "Me",
                event = memberEvent(),
            )
        )
        every { userServiceMock.getById(any(), eq(user2)) } returns MutableStateFlow(
            RoomUser(RoomId(""), UserId(""), "User 2", memberEvent())
        )
        every { userServiceMock.getById(eq(roomId1), eq(user2)) } returns user2Flow
        every { userServiceMock.getById(eq(roomId1), eq(user3)) } returns MutableStateFlow(
            RoomUser(
                roomId1,
                userId = user3,
                name = "User 3",
                event = memberEvent(),
            )
        )

        every {
            roomServiceMock.getState(
                any(), eq(AvatarEventContent::class), any()
            )
        } returns MutableStateFlow(null)
        everySuspend { roomsApiClientMock.leaveRoom(any(), any(), any()) } returns Result.success(Unit)
        every { roomServiceMock.usersTyping } returns MutableStateFlow(mapOf())

        roomByIdMocker = every { roomServiceMock.getById(roomId) }
        roomByIdMocker returns MutableStateFlow(
            Room(
                roomId,
                isDirect = false,
                encrypted = true,
                unreadMessageCount = 0,
                membership = Membership.INVITE,
                membersLoaded = false,
            )
        )

        every { userPresenceMock.presentEventContentFlow(eq(matrixClientMock), any()) } returns MutableStateFlow(null)

        every { roomNameMock.getRoomName(any<RoomId>(), eq(matrixClientMock), any()) } returns flowOf("RoomName")
        every { clock.now() } returns Instant.parse("2021-11-03T15:00:00Z")

    }

    @Test
    fun `show time for last messages that were sent today and the date for messages from yesterday onwards`() =
        runTest {
            val room1LastEvent = timelineEvent(EventId("event1"), clock.now().minus(1.seconds))
            every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(room1LastEvent.eventId)) } returns flowOf(
                room1LastEvent
            )
            val room2LastEvent = timelineEvent(EventId("event2"), clock.now().minus(1.minutes))
            every { roomServiceMock.getTimelineEvent(eq(roomId2), eq(room2LastEvent.eventId)) } returns flowOf(
                room2LastEvent
            )
            val room3LastEvent = timelineEvent(EventId("event3"), clock.now().minus(1.hours))
            every { roomServiceMock.getTimelineEvent(eq(roomId3), eq(room3LastEvent.eventId)) } returns flowOf(
                room3LastEvent
            )
            val room4LastEvent = timelineEvent(EventId("event4"), clock.now().minus(1.days))
            every { roomServiceMock.getTimelineEvent(eq(roomId4), eq(room4LastEvent.eventId)) } returns flowOf(
                room4LastEvent
            )

            val room1 = Room(
                roomId1,
                lastRelevantEventId = room1LastEvent.eventId,
                lastRelevantEventTimestamp = Instant.fromEpochMilliseconds(room1LastEvent.originTimestamp)
            )
            val room2 = Room(
                roomId2,
                lastRelevantEventId = room2LastEvent.eventId,
                lastRelevantEventTimestamp = Instant.fromEpochMilliseconds(room2LastEvent.originTimestamp)
            )
            val room3 = Room(
                roomId3,
                lastRelevantEventId = room3LastEvent.eventId,
                lastRelevantEventTimestamp = Instant.fromEpochMilliseconds(room3LastEvent.originTimestamp)
            )
            val room4 = Room(
                roomId4,
                lastRelevantEventId = room4LastEvent.eventId,
                lastRelevantEventTimestamp = Instant.fromEpochMilliseconds(room4LastEvent.originTimestamp)
            )

            every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
            every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
            every { roomServiceMock.getById(roomId4) } returns MutableStateFlow(room4)

            roomListElementViewModel(
                roomId1,
            ).time.first { it == "15:59" }
            roomListElementViewModel(
                roomId2,
            ).time.first { it == "15:59" }
            roomListElementViewModel(
                roomId3,
            ).time.first { it == "15:00" }
            roomListElementViewModel(
                roomId4,
            ).time.first { it == "02.11.21" }
        }

    @Test
    fun `display last message for text messages and decrypted text messages`() = runTest {
        val eventId1 = EventId("\$event1")
        val eventId2 = EventId("\$event2")
        val room1 = Room(
            roomId1, lastEventId = eventId1, isDirect = true, lastRelevantEventId = eventId1
        )
        val room2 = Room(
            roomId2, lastEventId = eventId2, isDirect = true, lastRelevantEventId = eventId2
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!"
            )
        )
        every { roomServiceMock.getTimelineEvent(eq(roomId2), eq(eventId2)) } returns flowOf(
            timelineEventEncrypted(
                eventId2, Clock.System.now().minus(1.days), "What's up?"
            )
        )

        roomListElementViewModel(roomId1).lastMessage.first { it == "Hello!" }
        roomListElementViewModel(roomId2).lastMessage.first { it == "What's up?" }
    }

    @Test
    fun `display last message as empty when unknown`() = runTest {
        val eventId1 = EventId("\$event1")
        val room1 = Room(
            roomId1, lastEventId = eventId1, isDirect = true, lastRelevantEventId = eventId1
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(null)

        roomListElementViewModel(roomId1).lastMessage.first { it == "" }
    }

    @Test
    fun `show 'you' as the author in the last message in a direct room when last message was sent by me`() = runTest {
        val eventId1 = EventId("\$event1")
        val eventId2 = EventId("\$event2")
        val room1 = Room(
            roomId1, lastEventId = eventId1, isDirect = true, lastRelevantEventId = eventId1
        )
        val room2 = Room(
            roomId2, lastEventId = eventId2, isDirect = true, lastRelevantEventId = eventId2
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!",
                sender = me,
            )
        )
        every { roomServiceMock.getTimelineEvent(eq(roomId2), eq(eventId2)) } returns flowOf(
            timelineEventEncrypted(
                eventId2,
                Clock.System.now().minus(1.days),
                "What's up?",
                sender = me,
            )
        )

        roomListElementViewModel(roomId1).lastMessage.first { it == "you: Hello!" }
        roomListElementViewModel(roomId2).lastMessage.first { it == "you: What's up?" }
    }

    @Test
    fun `display a special text for file based messages not encrypted and decrypted`() = runTest {
        val eventId1 = EventId("\$event1")
        val eventId2 = EventId("\$event2")
        val room1 = Room(
            roomId1,
            lastEventId = eventId1,
            isDirect = true,
            lastRelevantEventId = eventId1,
        )
        val room2 = Room(
            roomId2,
            lastEventId = eventId2,
            isDirect = true,
            lastRelevantEventId = eventId2,
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            TimelineEvent(
                MessageEvent(
                    RoomMessageEventContent.FileBased.Image(""),
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
        every { roomServiceMock.getTimelineEvent(eq(roomId2), eq(eventId2)) } returns flowOf(
            TimelineEvent(
                MessageEvent(
                    MegolmEncryptedMessageEventContent(
                        "",
                        KeyValue.Curve25519KeyValue(""),
                        deviceId = "",
                        sessionId = "",
                    ),
                    eventId2,
                    user2,
                    roomId2,
                    Clock.System.now().toEpochMilliseconds(),
                ),
                Result.success(
                    RoomMessageEventContent.FileBased.Video(""),
                ),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )

        roomListElementViewModel(roomId1).lastMessage.first { it == "Image" }
        roomListElementViewModel(roomId2).lastMessage.first { it == "Video" }
    }

    @Test
    fun `change the RoomElement when the last message in a room changes`() = runTest {
        val eventId1 = EventId("\$event1")
        val eventId2 = EventId("\$event2")
        val room1 = Room(
            roomId1, lastEventId = eventId1, isDirect = true, lastRelevantEventId = eventId1
        )
        val roomFlow = MutableStateFlow(room1)

        every { roomServiceMock.getById(roomId1) } returns roomFlow
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!"
            )
        )
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId2)) } returns flowOf(
            timelineEvent(
                eventId2,
                Clock.System.now(),
                "A new message."
            )
        )

        val cut = roomListElementViewModel(roomId1)

        cut.lastMessage.first { it == "Hello!" }
        roomFlow.update { it.copy(lastRelevantEventId = eventId2) }
        cut.lastMessage.first { it == "A new message." }
    }

    @Test
    fun `change the RoomElement when the content of the last message changes`() = runTest {
        val eventId1 = EventId("\$event1")
        val room1 = Room(
            roomId1, lastEventId = eventId1, isDirect = true, lastRelevantEventId = eventId1
        )
        val messageStateFlow = MutableStateFlow(timelineEvent(eventId1, Clock.System.now(), "Hello!"))
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns messageStateFlow

        val cut = roomListElementViewModel(roomId1)

        cut.lastMessage.first { it == "Hello!" }

        messageStateFlow.value = timelineEvent(eventId1, Clock.System.now(), "Hello! I have more.")
        cut.lastMessage.first { it == "Hello! I have more." }
    }

    @Test
    fun `show the author of a message in a room with multiple users`() = runTest {
        val eventId1 = EventId("\$event1")
        val room1 = Room(
            roomId1, lastEventId = eventId1, isDirect = false, lastRelevantEventId = eventId1
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!"
            )
        )

        roomListElementViewModel(roomId1).lastMessage.firstWithClue("User 2: Hello!")
    }

    @Test
    fun `show the author of a message in a room with multiple users if the message was by me`() = runTest {
        val eventId1 = EventId("\$event1")
        val room1 = Room(
            roomId1,
            lastEventId = eventId1,
            isDirect = false,
            lastRelevantEventId = eventId1,
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!",
                sender = me
            )
        )

        roomListElementViewModel(roomId1).lastMessage.first { it == "you: Hello!" }
    }

    @Test
    fun `not show the author of a message in a direct room even it was not by me`() = runTest {
        val eventId1 = EventId("\$event1")
        val room1 = Room(
            roomId1,
            lastEventId = eventId1,
            isDirect = true,
            lastRelevantEventId = eventId1,
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!"
            )
        )

        roomListElementViewModel(roomId1).lastMessage.first { it == "Hello!" }
    }

    @Test
    fun `change the username in the last message in case the username changes`() = runTest {
        val eventId1 = EventId("\$event1")
        val room1 = Room(
            roomId1,
            lastEventId = eventId1,
            isDirect = false,
            lastRelevantEventId = eventId1,
        )
        every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock.getTimelineEvent(eq(roomId1), eq(eventId1)) } returns flowOf(
            timelineEvent(
                eventId1,
                Clock.System.now(),
                "Hello!"
            )
        )


        val cut = roomListElementViewModel(roomId1)

        cut.lastMessage.firstWithClue("User 2: Hello!")
        user2Flow.value = RoomUser(roomId1, user2, "User 2 new", memberEvent())
        cut.lastMessage.firstWithClue("User 2 new: Hello!")
    }

    @Test
    fun `reject invitation to room and block user when requested`() = runTest {
        val eventId1 = EventId("\$event1")
        val room = Room(
            roomId,
            lastEventId = eventId1,
            isDirect = false,
        )
        val baseIgnored = mapOf(
            UserId("do_not_want", "localhost") to JsonObject(emptyMap()),
        )
        val user2Ignored = mapOf(user2 to JsonObject(emptyMap()))
        val newIgnored = baseIgnored + user2Ignored

        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(room)
        every { roomServiceMock.getTimelineEvent(eq(roomId), eq(eventId1)) } returns flowOf(
            TimelineEvent(
                event = inviteEvent(),
                content = Result.success(inviteEvent().content),
                previousEventId = null,
                nextEventId = null,
                gap = null,
            )
        )

        every {
            userServiceMock.getAccountData(IgnoredUserListEventContent::class)
        } returns MutableStateFlow(IgnoredUserListEventContent(baseIgnored))

        everySuspend {
            usersApiClientMock.setAccountData(
                    content = any(), userId = any(), key = any(), asUserId = any()
                )
        } returns Result.success(Unit)

        everySuspend { roomInviter.getInviter(any(), any()) } returns user2

        val cut = roomListElementViewModel(roomId)
        cut.rejectInvitationAndBlockInviter()
        yield()

        verifySuspend {
            userServiceMock.getAccountData(IgnoredUserListEventContent::class)
            usersApiClientMock.setAccountData(
                eq(IgnoredUserListEventContent(newIgnored)),
                eq(me),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `provide correct information on whether the room is public and whether it is encrypted or not`() = runTest {
        val cut = roomListElementViewModel(roomId)

        eventually(100.milliseconds) {
            cut.isPublic.value shouldBe false
            cut.isEncrypted.value shouldBe true
        }
    }

    private fun TestScope.roomListElementViewModel(
        roomId: RoomId
    ): RoomListElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(
                        UserId(
                            "test", "server"
                        ) to matrixClientMock
                    ),
                ) + module {
                    single { userPresenceMock }
                    single { roomNameMock }
                    single { roomInviter }
                    single { clock }
                })
        }.koin
        val cut = RoomListElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            roomId,
            onRoomSelected = onRoomSelectedMock,
        )
        backgroundScope.launch { cut.isPublic.collect() }
        backgroundScope.launch { cut.isEncrypted.collect() }
        return cut
    }

    private fun timelineEvent(eventId: EventId, sentAt: Instant, body: String = "", sender: UserId = user2) =
        TimelineEvent(
            event = MessageEvent(
                content = RoomMessageEventContent.TextBased.Text(body),
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
            event = MessageEvent(
                content = MegolmEncryptedMessageEventContent(
                    ciphertext = "", senderKey = KeyValue.Curve25519KeyValue(""), deviceId = "", sessionId = ""
                ),
                id = eventId,
                sender = sender,
                roomId = roomId1,
                originTimestamp = sentAt.toEpochMilliseconds(),
                unsigned = null
            ),
            content = Result.success(RoomMessageEventContent.TextBased.Text(body)),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    private fun memberEvent() = StateEvent(
        content = MemberEventContent(membership = Membership.JOIN),
        id = EventId(""),
        sender = me,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = ""
    )

    private fun inviteEvent() = StateEvent(
        content = MemberEventContent(membership = Membership.INVITE),
        id = EventId("\$event1"),
        sender = user2,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = me.full,
    )

}
