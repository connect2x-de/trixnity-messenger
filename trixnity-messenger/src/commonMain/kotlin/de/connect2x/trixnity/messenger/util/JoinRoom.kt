package de.connect2x.trixnity.messenger.util

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import net.folivo.trixnity.core.model.events.m.room.Membership

/**
 * Joins a room based on its joinRule
 */
interface JoinRoom {
    suspend operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
        joinRule: JoinRule,
        reason: String? = null
    ): JoinResult

    companion object : JoinRoom {
        override suspend operator fun invoke(
            matrixClient: MatrixClient,
            roomId: RoomId,
            joinRule: JoinRule,
            reason: String?
        ): JoinResult =
            when (joinRule) {
                JoinRule.Knock ->
                    matrixClient.api.room.knockRoom(roomId = roomId, reason = reason).fold(
                        onFailure = {
                            JoinResult.Error(joinRule, it)
                        },
                        onSuccess = {
                            JoinResult.Success(joinRule)
                        }
                    )

                JoinRule.Public, JoinRule.Restricted, JoinRule.KnockRestricted ->
                    matrixClient.api.room.joinRoom(roomId).fold(
                        onFailure = {
                            if (
                                it is MatrixServerException &&
                                it.statusCode == HttpStatusCode.Forbidden
                            ) {
                                if (joinRule == JoinRule.KnockRestricted)
                                    invoke(matrixClient, roomId, JoinRule.Knock, reason)
                                else JoinResult.Failed(joinRule)
                            } else JoinResult.Error(joinRule, it)
                        },
                        onSuccess = {
                            JoinResult.Success(joinRule)
                        }
                    )

                JoinRule.Invite ->
                    if (matrixClient.room.getById(roomId).filterNotNull().firstOrNull()?.membership == Membership.INVITE
                    )
                        invoke(matrixClient, roomId, JoinRule.Public, reason)
                    else
                        JoinResult.Failed(joinRule)

                JoinRule.Private ->
                    JoinResult.Failed(joinRule)

                is JoinRule.Unknown ->
                    JoinResult.Failed(joinRule)
            }
    }
}

sealed interface JoinResult {
    data class Success(val kind: JoinRule) : JoinResult
    data class Failed(val kind: JoinRule) : JoinResult
    data class Error(val kind: JoinRule, val error: Throwable) : JoinResult

    fun fold(onSuccess: (Success) -> Unit = {}, onFailure: (Failed) -> Unit = {}, onError: (Error) -> Unit = {}) {
        when (this) {
            is Success -> onSuccess(this)
            is Failed -> onFailure(this)
            is Error -> onError(this)
        }
    }
}
