package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    fun getBlockedUsers(matrixClient: MatrixClient): Flow<List<UserId>>

    fun isUserBlocked(matrixClient: MatrixClient, userId: UserId): Flow<Boolean>
}

class UserBlockingImpl : UserBlocking {

    private val mutex = Mutex()

    override suspend fun blockUser(
        matrixClient: MatrixClient,
        userToBlock: UserId,
        onSuccess: suspend () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        mutex.withLock {
            withTimeoutOrNull(5.seconds) {
                if (matrixClient.userId == userToBlock) {
                    onFailure(IllegalArgumentException("Cannot block yourself"))
                    return@withTimeoutOrNull
                }

                matrixClient.user.getAccountData<IgnoredUserListEventContent>()
                    .map { it ?: IgnoredUserListEventContent(emptyMap()) }.first()
                    .ignoredUsers.let { ignoredUsers ->
                        if (ignoredUsers.contains(userToBlock)) {
                            onFailure(IllegalArgumentException("user to block is already blocked"))
                            return@let
                        }
                        val newIgnoredUsers = ignoredUsers + (userToBlock to JsonObject(emptyMap()))
                        matrixClient.api.user.setAccountData(
                            content = IgnoredUserListEventContent(newIgnoredUsers),
                            userId = matrixClient.userId,
                        )
                            .onFailure {
                                log.error(it) { "cannot block user'${userToBlock.full}'" }
                                onFailure(it)
                                return@let
                            }

                        // Verify the success via the account data to avoid race conditions.
                        matrixClient.user.getAccountData<IgnoredUserListEventContent>().first {
                            // To avoid concurrency issues, only check if the userToBlock is present.
                            it != null && it.ignoredUsers.contains(userToBlock)
                        }.let {
                            log.info { "successfully blocked user '${userToBlock.full}'" }
                            onSuccess()
                        }
                    }
            } ?: onFailure(RuntimeException("user blocking timed out"))
        }
    }

    override suspend fun unblockUser(
        matrixClient: MatrixClient,
        userToUnblock: UserId,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        mutex.withLock {
            withTimeoutOrNull(5.seconds) {
                matrixClient.user.getAccountData<IgnoredUserListEventContent>().first()
                    ?.ignoredUsers?.let { ignoredUsers ->
                        if (ignoredUsers.contains(userToUnblock).not()) {
                            onFailure(IllegalArgumentException("user to unblock is not blocked"))
                            return@let
                        }
                        val newIgnoredUsers = ignoredUsers - userToUnblock
                        matrixClient.api.user.setAccountData(
                            content = IgnoredUserListEventContent(newIgnoredUsers),
                            userId = matrixClient.userId,
                        )
                            .onFailure {
                                log.error(it) { "cannot unblock user'${userToUnblock.full}'" }
                                onFailure(it)
                                return@let
                            }

                        // Verify the success via the account data to avoid race conditions.
                        matrixClient.user.getAccountData<IgnoredUserListEventContent>().first {
                            // To avoid concurrency issues, only check if the userToUnblock is not present.
                            it != null && it.ignoredUsers.contains(userToUnblock).not()
                        }.let {
                            log.info { "successfully unblocked user '${userToUnblock.full}'" }
                            onSuccess()
                        }
                    }
            } ?: onFailure(RuntimeException("user unblocking timed out"))
        }
    }

    override fun getBlockedUsers(matrixClient: MatrixClient): Flow<List<UserId>> =
        matrixClient.user.getAccountData<IgnoredUserListEventContent>()
            .map { it?.ignoredUsers?.keys?.toList() ?: listOf() }

    override fun isUserBlocked(matrixClient: MatrixClient, userId: UserId): Flow<Boolean> =
        matrixClient.user.getAccountData<IgnoredUserListEventContent>().map {
            it?.ignoredUsers?.containsKey(userId)
                ?: false
        }
}
