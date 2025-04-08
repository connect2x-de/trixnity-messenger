package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.ForgetRoomImpl
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
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
    fun `forgetRoom - should leave and forget room when existing`() = runTestWithCoroutineScope {
        everySuspend { roomApiClient.leaveRoom(any()) } calls {
            room.value = Room(roomId, membership = Membership.LEAVE)
            Result.success(Unit)
        }
        everySuspend { roomApiClient.forgetRoom(any()) } calls {
            Result.success(Unit)
        }
        everySuspend { matrixClient.room.forgetRoom(any()) } calls {
            if (room.value?.membership != Membership.LEAVE)
                return@calls
            room.value = null
        }

        ForgetRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe true
        room.value shouldBe null
    }

    @Test
    fun `forgetRoom - should not call forgetRoom after failed leaveRoom`() = runTestWithCoroutineScope {
        everySuspend { roomApiClient.leaveRoom(any()) } returns Result.failure(RuntimeException())
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit

        ForgetRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe false
        verifySuspend(VerifyMode.not) { roomApiClient.forgetRoom(any()) }
        verifySuspend(VerifyMode.not) { matrixClient.room.forgetRoom(any()) }
    }

    @Test
    fun `forgetRoom - should forget room from room service when room API client don't work because of IOException`() =
        runTestWithCoroutineScope {
            everySuspend { roomApiClient.leaveRoom(any()) } calls {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }
            everySuspend { roomApiClient.forgetRoom(any()) } returns Result.failure(RuntimeException(""))
            everySuspend { matrixClient.room.forgetRoom(any()) } calls {
                if (room.value?.membership != Membership.LEAVE)
                    return@calls
                room.value = null
            }

            ForgetRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe true
            verifySuspend { matrixClient.room.forgetRoom(any()) }
            room.value shouldBe null
        }

    @Test
    fun `forgetRoom - should not forget room in room service when room API client fails because of Matrix server`() =
        runTestWithCoroutineScope {
            everySuspend { roomApiClient.leaveRoom(any()) } returns Result.success(Unit)
            everySuspend { matrixClient.room.forgetRoom(any()) } returns Unit
            everySuspend { roomApiClient.forgetRoom(any()) } returns Result.failure(
                MatrixServerException(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse.CustomErrorResponse("", "")
                )
            )

            ForgetRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe false
            verifySuspend(VerifyMode.not) { matrixClient.room.forgetRoom(any()) }
        }

}
