package de.connect2x.trixnity.messenger.viewmodel.util

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class UserPresenceTest {
    private val room = RoomId("room", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClient = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val directRoomMock = mock<DirectRoom>()

    init {
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.api } returns matrixClientServerApiClient
        every { matrixClientServerApiClient.user } returns usersApiClientMock
    }

    @Test
    fun `return null when not a direct room`() = runTest {
        val userPresenceFlow = MutableStateFlow(
            mapOf(
                alice to PresenceEventContent(presence = Presence.ONLINE),
                bob to PresenceEventContent(presence = Presence.UNAVAILABLE)
            )
        )
        every { userServiceMock.userPresence } returns userPresenceFlow
        every { directRoomMock.getUsers(eq(matrixClientMock), eq(room)) } returns MutableStateFlow(
            emptyList()
        )

        val cut = userPresence()
        val result = cut.presentEventContentFlow(matrixClientMock, room).stateIn(backgroundScope)
        delay(100)
        result.value shouldBe null
    }

    @Test
    fun `return the current presence status of the other user in a direct room`() = runTest {
        val userPresenceFlow = MutableStateFlow(
            mapOf(
                alice to PresenceEventContent(presence = Presence.ONLINE),
                bob to PresenceEventContent(presence = Presence.UNAVAILABLE)
            )
        )
        every { userServiceMock.userPresence } returns userPresenceFlow
        every { directRoomMock.getUsers(eq(matrixClientMock), eq(room)) } returns MutableStateFlow(
            listOf(alice)
        )

        val cut = userPresence()
        val result = cut.presentEventContentFlow(matrixClientMock, room).stateIn(backgroundScope)
        delay(100)
        result.value shouldBe PresenceEventContent(presence = Presence.ONLINE)
    }

    @Test
    fun `return 'offline' initially when no presence status found`() = runTest {
        val userPresenceFlow = MutableStateFlow(mapOf<UserId, PresenceEventContent>())
        every { userServiceMock.userPresence } returns userPresenceFlow
        every { directRoomMock.getUsers(eq(matrixClientMock), eq(room)) } returns MutableStateFlow(
            listOf(alice)
        )
        everySuspend { usersApiClientMock.getPresence(alice) } returns Result.failure(RuntimeException("Oh no!"))

        val cut = userPresence()
        val result = cut.presentEventContentFlow(matrixClientMock, room).stateIn(backgroundScope)
        result.value shouldBe PresenceEventContent(presence = Presence.OFFLINE)
        delay(100)
        result.value shouldBe PresenceEventContent(presence = Presence.OFFLINE)
    }

    @Test
    fun `update the presence information via a server request for users whose presence status is not known yet`() =
        runTest {
            val userPresenceFlow = MutableStateFlow(mapOf<UserId, PresenceEventContent>())
            every { userServiceMock.userPresence } returns userPresenceFlow
            every { directRoomMock.getUsers(eq(matrixClientMock), eq(room)) } returns MutableStateFlow(
                listOf(alice)
            )
            everySuspend { usersApiClientMock.getPresence(alice) } returns Result.success(PresenceEventContent(presence = Presence.ONLINE))

            val cut = userPresence()
            cut.presentEventContentFlow(matrixClientMock, room).take(2).collectIndexed { index, value ->
                    when (index) {
                        0 -> value shouldBe PresenceEventContent(presence = Presence.OFFLINE)
                        1 -> value shouldBe PresenceEventContent(presence = Presence.ONLINE)
                    }
                }
        }

    private fun userPresence() = UserPresenceImpl(directRoomMock)
}
