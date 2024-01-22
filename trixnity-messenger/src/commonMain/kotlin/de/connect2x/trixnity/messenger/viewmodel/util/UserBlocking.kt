package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

interface UserBlocking {
    suspend fun blockUser(
        matrixClient: MatrixClient,
        userToBlock: UserId,
        onSuccess: suspend () -> Unit = {},
        onFailure: (Throwable) -> Unit,
    )

    suspend fun unblockUser(
        matrixClient: MatrixClient,
        userToUnblock: UserId,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit,
    )

    fun isUserBlocked(matrixClient: MatrixClient, userId: UserId): Flow<Boolean>
}

class UserBlockingImpl : UserBlocking {
    override suspend fun blockUser(
        matrixClient: MatrixClient,
        userToBlock: UserId,
        onSuccess: suspend () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val result = withTimeoutOrNull(5.seconds) {
            val accountData = matrixClient.user.getAccountData<IgnoredUserListEventContent>().first()
                ?: IgnoredUserListEventContent(emptyMap())
            accountData.ignoredUsers.let { ignoredUsers ->
                val newIgnoredUsers = ignoredUsers + (userToBlock to JsonObject(emptyMap()))
                matrixClient.api.user.setAccountData(
                    content = IgnoredUserListEventContent(newIgnoredUsers),
                    userId = matrixClient.userId,
                )
                    .onSuccess {
                        log.info { "successfully blocked user '${userToBlock.full}'" }
                        onSuccess()
                    }
                    .onFailure {
                        log.error(it) { "cannot block user'${userToBlock.full}'" }
                        onFailure(it)
                    }
            }
        }
        if (result == null) {
            onFailure(RuntimeException("getting account data timed out"))
        }
    }

    override suspend fun unblockUser(
        matrixClient: MatrixClient,
        userToUnblock: UserId,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        matrixClient.user.getAccountData<IgnoredUserListEventContent>().first()?.ignoredUsers?.let { ignoredUsers ->
            val newIgnoredUsers = ignoredUsers - userToUnblock
            matrixClient.api.user.setAccountData(
                content = IgnoredUserListEventContent(newIgnoredUsers),
                userId = matrixClient.userId,
            )
                .onSuccess {
                    log.info { "successfully unblocked user '${userToUnblock.full}'" }
                    onSuccess()
                }
                .onFailure {
                    log.error(it) { "cannot unblock user'${userToUnblock.full}'" }
                    onFailure(it)
                }
        }
    }

    override fun isUserBlocked(matrixClient: MatrixClient, userId: UserId): Flow<Boolean> {
        return matrixClient.user.getAccountData<IgnoredUserListEventContent>().map {
            it?.ignoredUsers?.containsKey(userId)
                ?: false
        }
    }
}