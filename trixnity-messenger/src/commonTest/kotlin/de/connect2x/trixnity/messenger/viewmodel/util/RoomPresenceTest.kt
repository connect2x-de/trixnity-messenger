package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.UserPresence
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock

class RoomPresenceTest {
    private val room = RoomId("!room")
    private val eventId = EventId("1")

    private val aliceId = UserId("alice")
    private val alice = RoomUser(
        room, aliceId, aliceId.full, ClientEvent.RoomEvent.StateEvent(
            content = MemberEventContent(
                membership = Membership.JOIN
            ),
            id = eventId,
            sender = aliceId,
            roomId = room,
            originTimestamp = 0L,
            stateKey = ""
        )
    )

    private val bobId = UserId("bob")
    private val bob = RoomUser(
        room, bobId, bobId.full, ClientEvent.RoomEvent.StateEvent(
            content = MemberEventContent(
                membership = Membership.JOIN
            ),
            id = eventId,
            sender = bobId,
            roomId = room,
            originTimestamp = 0L,
            stateKey = ""
        )
    )

    val usId = UserId("user")
    val us = RoomUser(
        room, usId, usId.full, ClientEvent.RoomEvent.StateEvent(
            content = MemberEventContent(
                membership = Membership.JOIN
            ),
            id = eventId,
            sender = usId,
            roomId = room,
            originTimestamp = 0L,
            stateKey = ""
        )
    )

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    lateinit var presences: Map<UserId, Presence?>

    var isDirect: Boolean = false
    lateinit var members: List<UserId>

    @BeforeTest
    fun beforeTest() {
        resetMocks(matrixClientMock, userServiceMock)
        isDirect = false
        members = listOf()
        presences = mapOf()

        every { matrixClientMock.userId } returns usId
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        every { roomServiceMock.getById(room) } calls {
            flowOf(Room(room, isDirect = isDirect))
        }
        every { userServiceMock.getById(room, aliceId) } returns flowOf(
            RoomUser(
                room, aliceId, aliceId.full,
                ClientEvent.RoomEvent.StateEvent(
                    MemberEventContent("", "", Membership.JOIN),
                    EventId("1"),
                    aliceId,
                    room,
                    0,
                    null,
                    ""
                )
            )
        )
        every { userServiceMock.getById(room, bobId) } returns flowOf(
            RoomUser(
                room, bobId, bobId.full,
                ClientEvent.RoomEvent.StateEvent(
                    MemberEventContent("", "", Membership.JOIN),
                    EventId("1"),
                    bobId,
                    room,
                    0,
                    null,
                    ""
                )
            )
        )
        every { userServiceMock.getPresence(any()) } calls { (userId: UserId) ->
            flowOf(presences[userId]?.let { UserPresence(it, Clock.System.now()) })
        }
    }

    @Test
    fun `is not direct - should be null`() = runTest {
        isDirect = false
        members = listOf(aliceId)
        presences = mapOf(aliceId to Presence.OFFLINE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe null
    }

    @Test
    fun `is direct - without members - should be null`() = runTest {
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                usId to flowOf(us)
            )
        )
        isDirect = true
        members = listOf()
        presences = mapOf(aliceId to Presence.OFFLINE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe null
    }

    @Test
    fun `is direct - single member - should be presence of single member`() = runTest {
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                usId to flowOf(us)
            )
        )
        isDirect = true
        members = listOf(aliceId)
        presences = mapOf(aliceId to Presence.OFFLINE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should be ONLINE when any is ONLINE`() = runTest {
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                bobId to flowOf(bob),
                usId to flowOf(us)
            )
        )
        isDirect = true
        members = listOf(aliceId, bobId)
        presences = mapOf(aliceId to Presence.ONLINE, bobId to Presence.OFFLINE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe Presence.ONLINE
    }

    @Test
    fun `is direct - multiple members - should be UNAVAILABLE when any is UNAVAILABLE`() = runTest {
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                bobId to flowOf(bob),
                usId to flowOf(us)
            )
        )
        isDirect = true
        members = listOf(aliceId, bobId)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to Presence.UNAVAILABLE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe Presence.UNAVAILABLE
    }

    @Test
    fun `is direct - multiple members - should be OFFLINE when none is ONLINE or UNAVAILABLE`() = runTest {
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                bobId to flowOf(bob),
                usId to flowOf(us)
            )
        )
        isDirect = true
        members = listOf(aliceId, bobId)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to null)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should ignore members that are not JOIN`() = runTest {
        val carol = UserId("carol", "localhost")
        val carolRoomUser = RoomUser(
            room, carol, carol.full, ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(
                    membership = Membership.LEAVE
                ),
                id = eventId,
                sender = carol,
                roomId = room,
                originTimestamp = 0L,
                stateKey = ""
            )
        )
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                bobId to flowOf(bob),
                carol to flowOf(carolRoomUser),
                usId to flowOf(us)
            )
        )
        every { userServiceMock.getById(room, carol) } returns flowOf(carolRoomUser)
        isDirect = true
        members = listOf(aliceId, bobId, carol)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to Presence.OFFLINE, carol to Presence.ONLINE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should ignore us`() = runTest {
        every { userServiceMock.getAll(any()) } returns flowOf(
            mapOf(
                aliceId to flowOf(alice),
                bobId to flowOf(bob),
                usId to flowOf(us)
            )
        )
        every { userServiceMock.getById(room, usId) } returns flowOf(us)
        isDirect = true
        members = listOf(aliceId, bobId, usId)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to Presence.OFFLINE, usId to Presence.ONLINE)
        RoomPresenceImpl(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }
}
