package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import dev.mokkery.matcher.any
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds

class JoinRoomTest : ShouldSpec() {

    private val cut = JoinRoom.Companion
    private val roomId = RoomId("room1", "localhost")
    private val room = Room(
        roomId,
    )
    private val roomInvited = Room(
        roomId,
        membership = Membership.INVITE,
    )

    private val matrixClientMock = mock<MatrixClient>()
    private val matrixApiClientMock = mock<MatrixClientServerApiClient>()
    private val roomApiClientMock = mock<RoomApiClient>()
    private val roomServiceMock = mock<RoomService>()

    init {
        beforeTest {
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

        should("Knock room") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Knock
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Knock)
            }
        }

        should("Join room") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Public
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Public)
            }
        }

        should("Join allowed restricted room") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Restricted
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Restricted)
            }
        }

        should("Fail to join restricted room") {
            everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                    Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("error")))


            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Restricted
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Restricted)
            }
        }

        should("Join allowed KnockRestricted room") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.KnockRestricted
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.KnockRestricted)
            }
        }

        should("Knock KnockRestricted room") {
            everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                    Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("error")))

            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.KnockRestricted
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Knock)
            }
        }

        should("Join Invite room we are invited to") {
            everySuspend { roomServiceMock.getById(eq(roomId)) } returns flowOf(roomInvited)

            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Invite
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Success(JoinRulesEventContent.JoinRule.Public)
            }
        }

        should("Not join Invite room we are not invited to") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Invite
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Invite)
            }
        }

        should("Not join private room") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Private
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Private)
            }
        }

        should("Not join unknown") {
            val res = cut.invoke(
                matrixClientMock,
                roomId,
                JoinRulesEventContent.JoinRule.Unknown("cooked")
            )

            eventually(500.milliseconds) {
                res shouldBe JoinResult.Failed(JoinRulesEventContent.JoinRule.Unknown("cooked"))
            }
        }
    }
}
