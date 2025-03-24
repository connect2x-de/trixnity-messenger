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
    fun `knock - should knock room successfully`() = runTestWithCoroutineScope {
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
    fun `knock - should fail to knock if no permissions`() = runTestWithCoroutineScope {
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
    fun `knock - should fail unknown room id`() = runTestWithCoroutineScope {
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
    fun `knock - should handle unexpected MatrixServerException`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode(418, "I'm a tea pot"), ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Knock, i18n.joinRoomFailedGenericKnock())
    }

    @Test
    fun `knock - should handle error`() = runTestWithCoroutineScope {
        val error = Throwable("something went wrong :(")
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.failure(error)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Error(JoinRule.Knock, error)
    }

    @Test
    fun `public - should join room successfully`() = runTestWithCoroutineScope {
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
    fun `public - should handle unexpected forbidden error`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Public, i18n.joinRoomFailedGenericJoin())
    }

    @Test
    fun `public - should handle unexpected MatrixServerException`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode(418, "I'm a tea pot"), ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Public, i18n.joinRoomFailedGenericJoin())
    }

    @Test
    fun `public - should handle error`() = runTestWithCoroutineScope {
        val error = Throwable("something went wrong :(")
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(error)

        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Error(JoinRule.Public, error)
    }

    @Test
    fun `invite - should join room successfully`() = runTestWithCoroutineScope {
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
    fun `invite - should fail room we're not invited in`() = runTestWithCoroutineScope {
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
    fun `restricted - should join room successfully`() = runTestWithCoroutineScope {
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
    fun `invite - should fail room we do not qualify for`() = runTestWithCoroutineScope {
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
    fun `knock restricted - should join room successfully and return strategy`() = runTestWithCoroutineScope {
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
    fun `knock restricted - should knock room successfully and return strategy`() = runTestWithCoroutineScope {
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
    fun `private - should fail private room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Private,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Private, i18n.joinRoomFailedGenericJoin())
    }

    @Test
    fun `unknown - should fail unknown JoinRule`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            matrixClientMock,
            JoinRule.Unknown("cooked_rule"),
            roomId
        )

        delay(500.milliseconds)

        res shouldBe JoinRoom.Result.Failed(JoinRule.Unknown("cooked_rule"), i18n.joinRoomFailedGenericJoin())
    }
}
