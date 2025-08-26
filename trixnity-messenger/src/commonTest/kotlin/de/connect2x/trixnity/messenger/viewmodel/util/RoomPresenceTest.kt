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
    private val usId = UserId("us", "localhost")
    private val aliceId = UserId("alice", "localhost")
    private val bobId = UserId("bob", "localhost")

    private val us = RoomUser(
        room, usId, usId.full,
        ClientEvent.RoomEvent.StateEvent(
            MemberEventContent("", "", Membership.JOIN),
            EventId("1"),
            usId,
            room,
            0,
            null,
            ""
        )
    )

    private val alice = RoomUser(
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

    private val bob = RoomUser(
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

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    lateinit var presences: Map<UserId, Presence?>

    lateinit var members: Map<UserId, RoomUser>
    lateinit var cut: RoomPresence

    @BeforeTest
    fun beforeTest() {
        resetMocks(matrixClientMock, userServiceMock)
        members = mapOf()
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
            flowOf(Room(room))
        }
        every { userServiceMock.getAll(room) } calls {
            flowOf(members.mapValues { (_, user) -> flowOf(user) })
        }
        every { userServiceMock.getById(room, aliceId) } returns flowOf(alice)
        every { userServiceMock.getById(room, bobId) } returns flowOf(bob)
        every { userServiceMock.getPresence(any()) } calls { (userId: UserId) ->
            flowOf(presences[userId]?.let { UserPresence(it, Clock.System.now()) })
        }

        cut = RoomPresenceImpl(RoomUsers, IsDirectRoomImpl(RoomUsers))
    }

    @Test
    fun `without members - should be null`() = runTest {
        members = mapOf()
        presences = mapOf(aliceId to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe null
    }

    @Test
    fun `single member - should be presence of single member`() = runTest {
        members = mapOf(aliceId to alice)
        presences = mapOf(aliceId to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `multiple members - should be ONLINE when any is ONLINE`() = runTest {
        members = mapOf(aliceId to alice, bobId to bob)
        presences = mapOf(aliceId to Presence.ONLINE, bobId to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.ONLINE
    }

    @Test
    fun `multiple members - should be UNAVAILABLE when any is UNAVAILABLE`() = runTest {
        members = mapOf(aliceId to alice, bobId to bob)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to Presence.UNAVAILABLE)
        cut(matrixClientMock, room).first() shouldBe Presence.UNAVAILABLE
    }

    @Test
    fun `multiple members - should be OFFLINE when none is ONLINE or UNAVAILABLE`() = runTest {
        members = mapOf(aliceId to alice, bobId to bob)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to null)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `multiple members - should ignore members that are not JOIN`() = runTest {
        val carolId = UserId("carol", "localhost")
        val carol = RoomUser(
            room, carolId, carolId.full,
            ClientEvent.RoomEvent.StateEvent(
                MemberEventContent("", "", Membership.LEAVE),
                EventId("1"),
                carolId,
                room,
                0,
                null,
                ""
            )
        )
        every { userServiceMock.getById(room, carolId) } returns flowOf(carol)
        members = mapOf(aliceId to alice, bobId to bob, carolId to carol)
        presences = mapOf(aliceId to Presence.OFFLINE, bobId to Presence.OFFLINE, carolId to Presence.ONLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }
}
