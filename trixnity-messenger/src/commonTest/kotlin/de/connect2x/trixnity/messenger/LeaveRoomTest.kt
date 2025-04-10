package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.LeaveRoomImpl
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomUtilsTest {

    private val roomId = RoomId("room1", "localhost")

    private val room: MutableStateFlow<Room?> = MutableStateFlow(null)
    private val matrixClient: MatrixClient = mock()
    private val matrixClientServerApiClient: MatrixClientServerApiClient = mock()
    private val roomApiClient: RoomApiClient = mock()
    private val roomService: RoomService = mock()

    @BeforeTest
    fun beforeTests() {
        room.value = Room(roomId)
        resetMocks(matrixClient, roomApiClient, roomService, matrixClientServerApiClient)
        every { roomService.getById(roomId) } returns room
        every { matrixClientServerApiClient.room } returns roomApiClient
        every { matrixClient.api } returns matrixClientServerApiClient
        every { matrixClient.di } returns koinApplication {
            modules(module {
                single { roomService }
            })
        }.koin
    }

    @Test
    fun `should call all functions when being told to do so`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
        everySuspend { roomApiClient.leaveRoom(any()) } calls {
            room.value = Room(roomId, membership = Membership.LEAVE)
            Result.success(Unit)
        }

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe true
        verifySuspend { roomApiClient.leaveRoom(any()) }
        verifySuspend { roomApiClient.forgetRoom(any()) }
    }

    @Test
    fun `should not call forget when told not to do so`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
        everySuspend { roomApiClient.leaveRoom(any()) } calls {
            room.value = Room(roomId, membership = Membership.LEAVE)
            Result.success(Unit)
        }

        LeaveRoomImpl().invoke(matrixClient, roomId, false).isSuccess shouldBe true
        verifySuspend(VerifyMode.not) { roomApiClient.forgetRoom(any()) }
    }

    @Test
    fun `should not call leaveRoom when membership is leave`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.LEAVE)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
        everySuspend { roomApiClient.leaveRoom(any()) } returns Result.success(Unit)

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe true
        verifySuspend(VerifyMode.not) { roomApiClient.leaveRoom(any()) }
    }

    @Test
    fun `should not forget room when leaveRoom call failed`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
        everySuspend { roomApiClient.leaveRoom(any()) } calls {
            room.value = Room(roomId, membership = Membership.LEAVE)
            Result.failure(RuntimeException())
        }

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe false
        verifySuspend(VerifyMode.not) { roomApiClient.forgetRoom(any()) }
    }

    @Test
    fun `should forget room locally when API call failed with unrelated exception`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.LEAVE)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.failure(RuntimeException())
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
        everySuspend { roomApiClient.leaveRoom(any()) } calls {
            room.value = Room(roomId, membership = Membership.LEAVE)
            Result.success(Unit)
        }

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe true
        verifySuspend { matrixClient.room.forgetRoom(any()) }
    }

    @Test
    fun `should forget room locally when API call failed with matrix server exception`() = runTestWithCoroutineScope {
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.failure(MatrixServerException(
            statusCode = HttpStatusCode.InternalServerError,
            errorResponse = ErrorResponse.Unknown("error")
        ))
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
        everySuspend { roomApiClient.leaveRoom(any()) } calls {
            room.value = Room(roomId, membership = Membership.LEAVE)
            Result.success(Unit)
        }

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe false
        verifySuspend(VerifyMode.not) { matrixClient.room.forgetRoom(any()) }
    }



}
