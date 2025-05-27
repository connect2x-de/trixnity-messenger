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
import net.folivo.trixnity.client.store.UserPresence
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomPresenceTest {
    private val room = RoomId("room", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val userServiceMock = mock<UserService>()
    val directRoomMock = mock<DirectRoom>()

    lateinit var presences: Map<UserId, Presence?>
    lateinit var members: List<UserId>
    lateinit var cut: RoomPresence

    @BeforeTest
    fun beforeTest() {
        resetMocks(matrixClientMock, userServiceMock, directRoomMock)
        members = listOf()
        presences = mapOf()

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                })
        }.koin
        every { directRoomMock.getUsers(matrixClientMock, room) } calls {
            flowOf(members)
        }

        every { userServiceMock.getPresence(any()) } calls { (userId: UserId) ->
            flowOf(presences[userId]?.let { UserPresence(it, Clock.System.now()) })
        }

        cut = RoomPresenceImpl(directRoomMock)
    }

    @Test
    fun `is not direct - should be null`() = runTest {
        members = listOf()
        presences = mapOf(alice to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe null
    }

    @Test
    fun `is direct - single member - should be presence of single member`() = runTest {
        members = listOf(alice)
        presences = mapOf(alice to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }

    @Test
    fun `is direct - multiple members - should be ONLINE when any is ONLINE`() = runTest {
        members = listOf(alice, bob)
        presences = mapOf(alice to Presence.ONLINE, bob to Presence.OFFLINE)
        cut(matrixClientMock, room).first() shouldBe Presence.ONLINE
    }

    @Test
    fun `is direct - multiple members - should be UNAVAILABLE when any is UNAVAILABLE`() = runTest {
        members = listOf(alice, bob)
        presences = mapOf(alice to Presence.OFFLINE, bob to Presence.UNAVAILABLE)
        cut(matrixClientMock, room).first() shouldBe Presence.UNAVAILABLE
    }

    @Test
    fun `is direct - multiple members - should be OFFLINE when none is ONLINE or UNAVAILABLE`() = runTest {
        members = listOf(alice, bob)
        presences = mapOf(alice to Presence.OFFLINE, bob to null)
        cut(matrixClientMock, room).first() shouldBe Presence.OFFLINE
    }
}
