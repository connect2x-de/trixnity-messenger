package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds


class EnterRoomTest {
    private val cut = EnterRoomImpl()

    private val roomId = RoomId("!room1")
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

        everySuspend { roomServiceMock.getById(roomId) } returns flowOf(room)
    }

    @Test
    fun `knock - should knock room successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(roomId, any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Success(JoinRule.Knock)
    }

    @Test
    fun `knock - should fail to knock if no permissions`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(roomId, any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Knock, i18n.enterRoomFailedNoPermission())
    }

    @Test
    fun `knock - should fail unknown room id`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(roomId, any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.NotFound, ErrorResponse.NotFound("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Knock, i18n.enterRoomFailedRoomDoesNotExist())
    }

    @Test
    fun `knock - should handle unexpected MatrixServerException`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.knockRoom(roomId, any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode(418, "I'm a tea pot"), ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Knock, i18n.enterRoomFailedGenericKnock())
    }

    @Test
    fun `knock - should handle error`() = runTestWithCoroutineScope {
        val error = Throwable("something went wrong :(")
        everySuspend { roomApiClientMock.knockRoom(roomId, any(), any(), any()) } returns
                Result.failure(error)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Knock,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Error(JoinRule.Knock, error)
    }

    @Test
    fun `public - should join room successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Success(JoinRule.Public)
    }

    @Test
    fun `public - should handle unexpected forbidden error`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Public, i18n.enterRoomFailedGenericJoin())
    }

    @Test
    fun `public - should handle unexpected MatrixServerException`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode(418, "I'm a tea pot"), ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Public, i18n.enterRoomFailedGenericJoin())
    }

    @Test
    fun `public - should handle error`() = runTestWithCoroutineScope {
        val error = Throwable("something went wrong :(")
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.failure(error)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Public,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Error(JoinRule.Public, error)
    }

    @Test
    fun `invite - should join room successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Invite,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Success(JoinRule.Invite)
    }

    @Test
    fun `invite - should fail room we're not invited in`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Invite,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Invite, i18n.enterRoomFailedInvite())
    }

    @Test
    fun `restricted - should join room successfully`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Restricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Success(JoinRule.Restricted)
    }

    @Test
    fun `invite - should fail room we do not qualify for`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Restricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Invite, i18n.enterRoomFailedRestricted())
    }

    @Test
    fun `knock restricted - should join room successfully and return strategy`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.KnockRestricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Success(JoinRule.Restricted)
    }

    @Test
    fun `knock restricted - should knock room successfully and return strategy`() = runTestWithCoroutineScope {
        everySuspend { roomApiClientMock.joinRoom(roomId, any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))
        everySuspend { roomApiClientMock.knockRoom(roomId, any(), any(), any()) } returns
                Result.success(roomId)

        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.KnockRestricted,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Success(JoinRule.Knock)
    }

    @Test
    fun `private - should fail private room`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Private,
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Private, i18n.enterRoomFailedGenericJoin())
    }

    @Test
    fun `unknown - should fail unknown JoinRule`() = runTestWithCoroutineScope {
        val res = cut.invoke(
            i18n,
            matrixClientMock,
            JoinRule.Unknown("cooked_rule"),
            roomId
        )

        delay(500.milliseconds)

        res shouldBe EnterRoom.Result.Failed(JoinRule.Unknown("cooked_rule"), i18n.enterRoomFailedGenericJoin())
    }
}
