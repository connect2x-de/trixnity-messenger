package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.i18n.I18n
import io.ktor.http.HttpStatusCode
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.model.rooms.JoinRoom.Request
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import net.folivo.trixnity.core.model.keys.Signed

/**
 * Joins a room based on its joinRule
 */
interface EnterRoom {
    suspend operator fun invoke(
        i18n: I18n,
        matrixClient: MatrixClient,
        joinRule: JoinRule,
        roomId: RoomId,
        reason: String? = null,
        via: Set<String>? = null,
        thirdPartySigned: Signed<Request.ThirdParty, String>? = null
    ): Result

    sealed interface Result {
        data class Success(val kind: JoinRule) : Result
        data class Failed(val kind: JoinRule, val reason: String) : Result
        data class Error(val kind: JoinRule, val error: Throwable) : Result

        fun fold(onSuccess: (Success) -> Unit = {}, onFailure: (Failed) -> Unit = {}, onError: (Error) -> Unit = {}) {
            when (this) {
                is Success -> onSuccess(this)
                is Failed -> onFailure(this)
                is Error -> onError(this)
            }
        }
    }
}

class EnterRoomImpl() : EnterRoom {
    override suspend operator fun invoke(
        i18n: I18n,
        matrixClient: MatrixClient,
        joinRule: JoinRule,
        roomId: RoomId,
        reason: String?,
        via: Set<String>?,
        thirdPartySigned: Signed<Request.ThirdParty, String>?
    ): EnterRoom.Result {
        return when (joinRule) {
            JoinRule.Invite, JoinRule.Public, JoinRule.Restricted, JoinRule.KnockRestricted ->
                matrixClient.api.room.joinRoom(roomId, via, reason, thirdPartySigned).fold(
                    onFailure = {
                        if (
                            it is MatrixServerException &&
                            it.statusCode == HttpStatusCode.Forbidden
                        ) {
                            when (joinRule) {
                                JoinRule.KnockRestricted ->
                                    invoke(i18n, matrixClient, JoinRule.Knock, roomId, reason, via, thirdPartySigned)

                                JoinRule.Invite ->
                                    EnterRoom.Result.Failed(JoinRule.Invite, i18n.enterRoomFailedInvite())

                                JoinRule.Restricted ->
                                    EnterRoom.Result.Failed(JoinRule.Invite, i18n.enterRoomFailedRestricted())

                                else ->
                                    EnterRoom.Result.Failed(joinRule, i18n.enterRoomFailedGenericJoin())
                            }
                        } else if (it is MatrixServerException) {
                            EnterRoom.Result.Failed(joinRule, i18n.enterRoomFailedGenericJoin())
                        } else EnterRoom.Result.Error(joinRule, it)
                    },
                    onSuccess = {
                        EnterRoom.Result.Success(
                            if (joinRule == JoinRule.KnockRestricted) JoinRule.Restricted
                            else joinRule
                        )
                    }
                )

            JoinRule.Knock ->
                matrixClient.api.room.knockRoom(roomId, via, reason).fold(
                    onFailure = {
                        if (it is MatrixServerException) {
                            when (it.statusCode) {
                                HttpStatusCode.Forbidden ->
                                    EnterRoom.Result.Failed(joinRule, i18n.enterRoomFailedNoPermission())

                                HttpStatusCode.NotFound ->
                                    EnterRoom.Result.Failed(joinRule, i18n.enterRoomFailedRoomDoesNotExist())

                                else ->
                                    EnterRoom.Result.Failed(joinRule, i18n.enterRoomFailedGenericKnock())
                            }
                        } else EnterRoom.Result.Error(joinRule, it)
                    },
                    onSuccess = {
                        EnterRoom.Result.Success(joinRule)
                    }
                )

            JoinRule.Private, is JoinRule.Unknown ->
                EnterRoom.Result.Failed(joinRule, i18n.enterRoomFailedGenericJoin())
        }
    }
}
