package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.RoomId
import dev.mokkery.matcher.any
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds


class JoinRoomTest {
    private val cut = JoinRoomImpl()

    private val roomId = RoomId("room1", "localhost")
    private val room = Room(roomId)

    private val matrixClientMock = mock<MatrixClient>()
    private val matrixApiClientMock = mock<MatrixClientServerApiClient>()
    private val roomApiClientMock = mock<RoomApiClient>()
    private val roomServiceMock = mock<RoomService>()
    private val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}

    @BeforeTest
    fun setup() {
        resetMocks(matrixClientMock, matrixApiClientMock, roomApiClientMock, roomServiceMock)

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single<I18n> { i18n }
                }
            )
        }.koin

        every { matrixClientMock.api } returns matrixApiClientMock
        every { matrixApiClientMock.room } returns roomApiClientMock

        everySuspend { roomServiceMock.getById(eq(roomId)) } returns flowOf(room)
    }

    @Test
    fun `Knock - Knock room Successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Success(JoinRule.Knock)
    }

    @Test
    fun `Knock - Fail to knock if no permissions`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Knock, i18n.joinRoomFailedNoPermission())
    }

    @Test
    fun `Knock - Fail unknown room id`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.NotFound, ErrorResponse.NotFound("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Knock, i18n.joinRoomFailedRoomDoesNotExist())
    }

    @Test
    fun `Knock - Fail too many requests`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.NotFound, ErrorResponse.NotFound("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Knock, i18n.joinRoomFailedRoomDoesNotExist())
    }

    @Test
    fun `Public - Join Room Successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Success(JoinRule.Public)
    }

    @Test
    fun `Invite - Join Room Successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Invite,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Success(JoinRule.Invite)
    }

    @Test
    fun `Invite - Fail room we're not invited in`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Invite,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Invite, i18n.joinRoomFailedInvite())
    }

    @Test
    fun `Restricted - Join Room Successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Restricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Success(JoinRule.Restricted)
    }

    @Test
    fun `Invite - Fail room we do not qualify for`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Restricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Invite, i18n.joinRoomFailedRestricted())
    }

    @Test
    fun `KnockRestricted - Join Room Successfully and return strategy`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.KnockRestricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Success(JoinRule.Restricted)
    }

    @Test
    fun `KnockRestricted - Knock Room Successfully and return strategy`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.KnockRestricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Success(JoinRule.Knock)
    }

    @Test
    fun `Private - Fail private room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Private,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Private, i18n.joinRoomFailedGenericJoin())
    }

    @Test
    fun `Unknown - Fail unknown JoinRule`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Unknown("cooked_rule"),
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Unknown("cooked_rule"), i18n.joinRoomFailedGenericJoin())
    }
}
