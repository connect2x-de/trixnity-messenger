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
import kotlinx.datetime.Clock
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

class RoomPresenceTest {
    private val room = RoomId("!room")
    private val us = UserId("us", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val directRoomMock = mock<DirectRoom>()

    lateinit var presences: Map<UserId, Presence?>

    var isDirect: Boolean = false
    lateinit var members: List<UserId>
    lateinit var cut: RoomPresence

    @BeforeTest
    fun beforeTest() {
        resetMocks(matrixClientMock, userServiceMock, directRoomMock)
        isDirect = false
        members = listOf()
        presences = mapOf()

        every { matrixClientMock.userId } returns us
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
        every { directRoomMock.getUsers(matrixClientMock, room) } calls {
            flowOf(members)
        }
        every { userServiceMock.getById(room, alice) } returns flowOf(
            RoomUser(
                room, alice, alice.full,
                ClientEvent.RoomEvent.StateEvent(
                    MemberEventContent("", "", Membership.JOIN),
                    EventId("1"),
                    alice,
                    room,
                    0,
                    null,
                    ""
                )
            )
        )
        every { userServiceMock.getById(room, bob) } returns flowOf(
            RoomUser(
                room, bob, bob.full,
                ClientEvent.RoomEvent.StateEvent(
                    MemberEventContent("", "", Membership.JOIN),
                    EventId("1"),
                    bob,
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

        cut = RoomPresenceImpl(directRoomMock)
    }

    @Test
    fun `is not direct - should be null`() = runTest {
        isDirect = false
        members = listOf(alice)
        presences = mapOf(alice to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe null
    }

    @Test
    fun `is direct - without members - should be null`() = runTest {
        isDirect = true
        members = listOf()
        presences = mapOf(alice to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe null
    }

    @Test
    fun `is direct - single member - should be presence of single member`() = runTest {
        isDirect = true
        members = listOf(alice)
        presences = mapOf(alice to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should be ONLINE when any is ONLINE`() = runTest {
        isDirect = true
        members = listOf(alice, bob)
        presences = mapOf(alice to Presence.ONLINE, bob to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.ONLINE
    }

    @Test
    fun `is direct - multiple members - should be UNAVAILABLE when any is UNAVAILABLE`() = runTest {
        isDirect = true
        members = listOf(alice, bob)
        presences = mapOf(alice to Presence.OFFLINE, bob to Presence.UNAVAILABLE)
        cut(matrixClientMock, room).first() shouldBe Presence.UNAVAILABLE
    }

    @Test
    fun `is direct - multiple members - should be OFFLINE when none is ONLINE or UNAVAILABLE`() = runTest {
        isDirect = true
        members = listOf(alice, bob)
        presences = mapOf(alice to Presence.OFFLINE, bob to null)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should ignore members that are not JOIN`() = runTest {
        val carol = UserId("carol", "localhost")
        every { userServiceMock.getById(room, carol) } returns flowOf(
            RoomUser(
                room, carol, carol.full,
                ClientEvent.RoomEvent.StateEvent(
                    MemberEventContent("", "", Membership.LEAVE),
                    EventId("1"),
                    carol,
                    room,
                    0,
                    null,
                    ""
                )
            )
        )
        isDirect = true
        members = listOf(alice, bob, carol)
        presences = mapOf(alice to Presence.OFFLINE, bob to Presence.OFFLINE, carol to Presence.ONLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should ignore us`() = runTest {
        every { userServiceMock.getById(room, us) } returns flowOf(
            RoomUser(
                room, us, us.full,
                ClientEvent.RoomEvent.StateEvent(
                    MemberEventContent("", "", Membership.JOIN),
                    EventId("1"),
                    us,
                    room,
                    0,
                    null,
                    ""
                )
            )
        )
        isDirect = true
        members = listOf(alice, bob, us)
        presences = mapOf(alice to Presence.OFFLINE, bob to Presence.OFFLINE, us to Presence.ONLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }
}
