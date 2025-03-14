package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.clientserverapi.client.getStateEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import net.folivo.trixnity.core.model.events.m.room.Membership

val JoinRule.isKnock: Boolean
    get() =
        when (this) {
            JoinRule.Knock -> true
            JoinRule.KnockRestricted -> true
            else -> false
        }

/**
 * Joins a room based on its joinRule
 */
interface joinRoom {
    suspend operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
        joinRule: JoinRule,
        reason: String? = null
    ): JoinResult

    companion object: joinRoom {
        /**
         * Whether you pass the restrictions to join. See [JoinRule.Restricted] and [JoinRule.KnockRestricted]
         */
        private suspend fun passesJoinRestriction(matrixClient: MatrixClient, roomId: RoomId): Boolean =
            matrixClient.api.room.getStateEvent<JoinRulesEventContent>(roomId)
                .map {
                    it.allow?.any {
                        matrixClient.room.getById(roomId).filterNotNull().firstOrNull()?.membership == Membership.JOIN
                    } == true
                }.getOrElse { false }

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

                JoinRule.Public ->
                    matrixClient.api.room.joinRoom(roomId).fold(
                        onFailure = {
                            JoinResult.Error(joinRule, it)
                        },
                        onSuccess = {
                            JoinResult.Success(joinRule)
                        }
                    )

                JoinRule.KnockRestricted ->
                    if (passesJoinRestriction(matrixClient, roomId))
                        invoke(matrixClient, roomId, JoinRule.Public, reason)
                    else
                        invoke(matrixClient, roomId, JoinRule.Knock, reason)

                JoinRule.Restricted ->
                    if (passesJoinRestriction(matrixClient, roomId))
                        invoke(matrixClient, roomId, JoinRule.Public, reason)
                    else
                        JoinResult.Failed(joinRule)

                JoinRule.Invite ->
                    if (matrixClient.room.getById(roomId).filterNotNull()
                            .firstOrNull()?.membership == Membership.INVITE
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
