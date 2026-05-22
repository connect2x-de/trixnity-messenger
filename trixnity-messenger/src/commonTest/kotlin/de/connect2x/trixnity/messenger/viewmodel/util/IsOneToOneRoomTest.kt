package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class IsOneToOneRoomTest {
    val eventId = EventId("1")
    val roomId = RoomId("the-room")

    val aliceId = UserId("alice")
    val alice =
        RoomUser(
            roomId,
            aliceId,
            aliceId.full,
            ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(membership = Membership.JOIN),
                id = eventId,
                sender = aliceId,
                roomId = roomId,
                originTimestamp = 0L,
                stateKey = "",
            ),
        )

    val bobId = UserId("bob")
    val bob =
        RoomUser(
            roomId,
            bobId,
            bobId.full,
            ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(membership = Membership.JOIN),
                id = eventId,
                sender = bobId,
                roomId = roomId,
                originTimestamp = 0L,
                stateKey = "",
            ),
        )

    val usId = UserId("user")
    val us =
        RoomUser(
            roomId,
            usId,
            usId.full,
            ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(membership = Membership.JOIN),
                id = eventId,
                sender = usId,
                roomId = roomId,
                originTimestamp = 0L,
                stateKey = "",
            ),
        )

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        resetMocks(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { userServiceMock }
                            single { roomServiceMock }
                        }
                    )
                }
                .koin
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId))
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `no direct room - returns false`() = runTest {
        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId).first()
        isOneToOneRoom shouldBe false
    }

    @Test
    fun `direct room - no users returns false`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId, isDirect = true))
        every { userServiceMock.getAll(roomId) } returns flowOf(mapOf())
        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId).first()
        isOneToOneRoom shouldBe false
    }

    @Test
    fun `direct room - one user returns false`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId, isDirect = true))
        every { userServiceMock.getAll(roomId) } returns flowOf(mapOf(usId to flowOf(us)))
        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId).first()
        isOneToOneRoom shouldBe false
    }

    @Test
    fun `direct room - two users returns true`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId, isDirect = true))
        every { userServiceMock.getAll(roomId) } returns flowOf(mapOf(usId to flowOf(us), aliceId to flowOf(alice)))
        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId).first()
        isOneToOneRoom shouldBe true
    }

    @Test
    fun `direct room - started with two users invited another user and user accepted returns false`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId, isDirect = true))
        val roomUsers =
            MutableStateFlow(
                mapOf(
                    usId to flowOf(us),
                    aliceId to flowOf(alice),
                    bobId to
                        flowOf(
                            bob.copy(
                                event =
                                    ClientEvent.RoomEvent.StateEvent(
                                        content = MemberEventContent(membership = Membership.INVITE),
                                        id = eventId,
                                        sender = bobId,
                                        roomId = roomId,
                                        originTimestamp = 0L,
                                        stateKey = "",
                                    )
                            )
                        ),
                )
            )
        every { userServiceMock.getAll(roomId) } returns roomUsers

        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId)
        isOneToOneRoom.first() shouldBe true

        roomUsers.value += bobId to flowOf(bob)
        isOneToOneRoom.first() shouldBe false
    }

    @Test
    fun `direct room - two users and one invited returns true`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId, isDirect = true))
        every { userServiceMock.getAll(roomId) } returns
            flowOf(
                mapOf(
                    usId to flowOf(us),
                    aliceId to flowOf(alice),
                    bobId to
                        flowOf(
                            bob.copy(
                                event =
                                    ClientEvent.RoomEvent.StateEvent(
                                        content = MemberEventContent(membership = Membership.INVITE),
                                        id = eventId,
                                        sender = bobId,
                                        roomId = roomId,
                                        originTimestamp = 0L,
                                        stateKey = "",
                                    )
                            )
                        ),
                )
            )
        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId).first()
        isOneToOneRoom shouldBe true
    }

    @Test
    fun `direct room - more than two users returns false`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(roomId = roomId, isDirect = true))
        every { userServiceMock.getAll(roomId) } returns
            flowOf(mapOf(usId to flowOf(us), aliceId to flowOf(alice), bobId to flowOf(bob)))
        val isOneToOneRoom = IsOneToOneRoomImpl(matrixClientMock, roomId).first()
        isOneToOneRoom shouldBe false
    }
}
