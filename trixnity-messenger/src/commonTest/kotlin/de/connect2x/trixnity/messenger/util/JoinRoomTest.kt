package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import dev.mokkery.matcher.any
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds


class JoinRoomTest {
    private val cut = JoinRoomImpl()

    private val roomId = RoomId("room1", "localhost")
    private val room = Room(roomId)
    private val roomInvited = Room(roomId, membership = Membership.INVITE)

    private val matrixClientMock = mock<MatrixClient>()
    private val matrixApiClientMock = mock<MatrixClientServerApiClient>()
    private val roomApiClientMock = mock<RoomApiClient>()
    private val roomServiceMock = mock<RoomService>()

    @BeforeTest
    fun setup() {
        resetMocks(matrixClientMock, matrixApiClientMock, roomApiClientMock, roomServiceMock)

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                }
            )
        }.koin

        every { matrixClientMock.api } returns matrixApiClientMock
        every { matrixApiClientMock.room } returns roomApiClientMock
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.success(roomId)

        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        everySuspend { roomServiceMock.getById(eq(roomId)) } returns flowOf(room)
    }

    @Test
    fun `Knock room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Knock
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Knock)
    }

    @Test
    fun `Fail to knock unknown room`() = runTestWithCoroutineScope {
        val exception = MatrixServerException(
            HttpStatusCode.NotFound,
            ErrorResponse.NotFound("roomid not found")
        )
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns Result.failure(exception)

        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Knock
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Error(JoinRulesEventContent.JoinRule.Knock, exception)
    }

    @Test
    fun `Join room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Public
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Public)
    }


    @Test
    fun `Join allowed restricted room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Restricted
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Restricted)
    }


    @Test
    fun `Fail to join restricted room`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("error")))


        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Restricted
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Restricted)
    }


    @Test
    fun `Join allowed KnockRestricted room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.KnockRestricted
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.KnockRestricted)
    }


    @Test
    fun `Knock KnockRestricted room`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("error")))

        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.KnockRestricted
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Knock)
    }


    @Test
    fun `Join Invite room we are invited to`() = runTestWithCoroutineScope {
        everySuspend { roomServiceMock.getById(eq(roomId)) } returns flowOf(roomInvited)

        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Invite
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Public)
    }


    @Test
    fun `Not join Invite room we are not invited to`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Invite
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Invite)
    }

    @Test
    fun `Not join private room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Private
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Private)
    }

    @Test
    fun `Not join unknown`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            roomId,
            JoinRulesEventContent.JoinRule.Unknown("cooked")
        )

        delay(500.milliseconds)

        res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Unknown("cooked"))
    }
}
