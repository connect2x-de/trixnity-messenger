package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UsersApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class UserPresenceTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)

    private val room = RoomId("room", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiClient: MatrixClientServerApiClient

    @Mock
    lateinit var usersApiClientMock: UsersApiClient

    @Mock
    lateinit var directRoomMock: DirectRoom

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { userServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.api } returns matrixClientServerApiClient
                every { matrixClientServerApiClient.users } returns usersApiClientMock
            }
        }

        should("return null when not a direct room") {
            val userPresenceFlow = MutableStateFlow(
                mapOf(
                    alice to PresenceEventContent(presence = Presence.ONLINE),
                    bob to PresenceEventContent(presence = Presence.UNAVAILABLE)
                )
            )
            mocker.every { userServiceMock.userPresence } returns userPresenceFlow
            mocker.every { directRoomMock.getUsers(isEqual(matrixClientMock), isEqual(room)) } returns MutableStateFlow(
                emptyList()
            )

            val cut = userPresence()
            val result = cut.presentEventContentFlow(matrixClientMock, room).stateIn(scope)
            testCoroutineScheduler.advanceUntilIdle()
            result.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("return the current presence status of the other user in a direct room") {
            val userPresenceFlow = MutableStateFlow(
                mapOf(
                    alice to PresenceEventContent(presence = Presence.ONLINE),
                    bob to PresenceEventContent(presence = Presence.UNAVAILABLE)
                )
            )
            mocker.every { userServiceMock.userPresence } returns userPresenceFlow
            mocker.every { directRoomMock.getUsers(isEqual(matrixClientMock), isEqual(room)) } returns MutableStateFlow(
                listOf(alice)
            )

            val cut = userPresence()
            val result = cut.presentEventContentFlow(matrixClientMock, room).stateIn(scope)
            testCoroutineScheduler.advanceUntilIdle()
            result.value shouldBe PresenceEventContent(presence = Presence.ONLINE)

            cancelNeverEndingCoroutines()
        }

        should("return 'offline' initially when no presence status found") {
            val userPresenceFlow = MutableStateFlow(mapOf<UserId, PresenceEventContent>())
            mocker.every { userServiceMock.userPresence } returns userPresenceFlow
            mocker.every { directRoomMock.getUsers(isEqual(matrixClientMock), isEqual(room)) } returns MutableStateFlow(
                listOf(alice)
            )
            mocker.everySuspending { usersApiClientMock.getPresence(alice) } returns
                    Result.failure(RuntimeException("Oh no!"))

            val cut = userPresence()
            val result = cut.presentEventContentFlow(matrixClientMock, room).stateIn(scope)
            result.value shouldBe PresenceEventContent(presence = Presence.OFFLINE)
            testCoroutineScheduler.advanceUntilIdle()
            result.value shouldBe PresenceEventContent(presence = Presence.OFFLINE)

            cancelNeverEndingCoroutines()
        }

        should("update the presence information via a server request for users whose presence status is not known yet") {
            val userPresenceFlow = MutableStateFlow(mapOf<UserId, PresenceEventContent>())
            mocker.every { userServiceMock.userPresence } returns userPresenceFlow
            mocker.every { directRoomMock.getUsers(isEqual(matrixClientMock), isEqual(room)) } returns MutableStateFlow(
                listOf(alice)
            )
            mocker.everySuspending { usersApiClientMock.getPresence(alice) } returns
                    Result.success(PresenceEventContent(presence = Presence.ONLINE))

            val cut = userPresence()
            cut.presentEventContentFlow(matrixClientMock, room)
                .take(2)
                .collectIndexed { index, value ->
                    when (index) {
                        0 -> value shouldBe PresenceEventContent(presence = Presence.OFFLINE)
                        1 -> value shouldBe PresenceEventContent(presence = Presence.ONLINE)
                    }
                }

            cancelNeverEndingCoroutines()
        }
    }

    private fun userPresence() = UserPresenceImpl(directRoomMock)
}
