package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class LeaveRoomTest {

    private val roomId = RoomId("!room1")
    private val upgradedRoomId = RoomId("!room0")

    private val room: MutableStateFlow<Room?> = MutableStateFlow(null)
    private val upgradedRoom: MutableStateFlow<Room?> = MutableStateFlow(null)
    private val matrixClient: MatrixClient = mock()
    private val matrixClientServerApiClient: MatrixClientServerApiClient = mock()
    private val roomApiClient: RoomApiClient = mock()
    private val roomService: RoomService = mock()

    @BeforeTest
    fun beforeTests() {
        configureTestLogging()
        room.value = Room(roomId)
        resetMocks(matrixClient, roomApiClient, roomService, matrixClientServerApiClient)
        every { roomService.getState(roomId, CreateEventContent::class, any()) } returns
            flowOf(
                ClientEvent.RoomEvent.StateEvent(
                    content =
                        CreateEventContent(
                            predecessor = CreateEventContent.PreviousRoom(upgradedRoomId, EventId("bla"))
                        ),
                    id = EventId("blub"),
                    sender = UserId("sender"),
                    roomId = roomId,
                    originTimestamp = 1234,
                    unsigned = null,
                    stateKey = "",
                )
            )
        every { roomService.getState(upgradedRoomId, CreateEventContent::class, any()) } returns flowOf(null)
        every { roomService.getById(roomId) } returns room
        every { roomService.getById(upgradedRoomId) } returns upgradedRoom
        everySuspend { roomService.forgetRoom(any(), any()) } returns Unit
        every { matrixClientServerApiClient.room } returns roomApiClient
        every { matrixClient.api } returns matrixClientServerApiClient
        every { matrixClient.di } returns koinApplication { modules(module { single { roomService } }) }.koin
    }

    @Test
    fun `should leave and forget`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(any()) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }

        LeaveRoomImpl().invoke(matrixClient, roomId).getOrThrow()
        verifySuspend {
            roomApiClient.leaveRoom(roomId)
            roomApiClient.forgetRoom(roomId)
        }
    }

    @Test
    fun `should leave and forget upgraded room`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        upgradedRoom.value = Room(upgradedRoomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(roomId) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }
        everySuspend { roomApiClient.leaveRoom(upgradedRoomId) } calls
            {
                upgradedRoom.value = Room(upgradedRoomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }

        LeaveRoomImpl().invoke(matrixClient, roomId).getOrThrow()
        verifySuspend {
            roomApiClient.leaveRoom(upgradedRoomId)
            roomApiClient.forgetRoom(upgradedRoomId)
            roomApiClient.leaveRoom(roomId)
            roomApiClient.forgetRoom(roomId)
        }
    }

    @Test
    fun `should not call forget when not requested`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(any()) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }

        LeaveRoomImpl().invoke(matrixClient, roomId, false).getOrThrow()
        verifySuspend(VerifyMode.not) { roomApiClient.forgetRoom(roomId) }
    }

    @Test
    fun `should not call leaveRoom when membership is already LEAVE`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.LEAVE)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(any()) } returns Result.success(Unit)

        LeaveRoomImpl().invoke(matrixClient, roomId).getOrThrow()
        verifySuspend(VerifyMode.not) { roomApiClient.leaveRoom(roomId) }
    }

    @Test
    fun `should forget room when leaveRoom call failed with MatrixServerException`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(any()) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.failure(
                    MatrixServerException(
                        statusCode = HttpStatusCode.InternalServerError,
                        errorResponse = ErrorResponse.Unknown("error"),
                    )
                )
            }

        LeaveRoomImpl().invoke(matrixClient, roomId).getOrThrow()
        verifySuspend { roomApiClient.forgetRoom(roomId) }
    }

    @Test
    fun `should not forget room when leaveRoom call failed with unrelated exception`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.JOIN)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(any()) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.failure(RuntimeException())
            }

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe false
        verifySuspend(VerifyMode.not) { roomApiClient.forgetRoom(roomId) }
    }

    @Test
    fun `should forget room locally when API call failed with MatrixServerException`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.LEAVE)
        everySuspend { roomApiClient.forgetRoom(any()) } returns
            Result.failure(
                MatrixServerException(
                    statusCode = HttpStatusCode.InternalServerError,
                    errorResponse = ErrorResponse.Unknown("error"),
                )
            )
        everySuspend { roomApiClient.leaveRoom(any()) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }

        LeaveRoomImpl().invoke(matrixClient, roomId).getOrThrow()
        verifySuspend { matrixClient.room.forgetRoom(roomId, true) }
    }

    @Test
    fun `should forget room locally when room is already deleted`() = runTestWithCoroutineScope {
        room.value = Room(roomId, membership = Membership.INVITE)
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.success(Unit)
        everySuspend { roomApiClient.leaveRoom(any()) } returns Result.success(Unit)
        LeaveRoomImpl().invoke(matrixClient, roomId).getOrThrow()
        verifySuspend { matrixClient.room.forgetRoom(roomId, true) }
    }

    @Test
    fun `should forget room locally when API call failed with unrelated exception`() = runTestWithCoroutineScope {
        everySuspend { roomApiClient.forgetRoom(any()) } returns Result.failure(RuntimeException("unrelated"))
        everySuspend { roomApiClient.leaveRoom(any()) } calls
            {
                room.value = Room(roomId, membership = Membership.LEAVE)
                Result.success(Unit)
            }

        LeaveRoomImpl().invoke(matrixClient, roomId).isSuccess shouldBe false
        verifySuspend(VerifyMode.not) { matrixClient.room.forgetRoom(roomId) }
    }
}
