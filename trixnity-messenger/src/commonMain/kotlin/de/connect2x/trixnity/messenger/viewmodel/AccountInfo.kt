package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.clientserverapi.model.user.avatarUrl
import de.connect2x.trixnity.clientserverapi.model.user.displayName
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.AccountInfoKt")

data class AccountInfo(
    val userId: UserId,
    val displayName: String,
    val displayColor: Long?,
    val initials: String,
    val avatar: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AccountInfo

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (initials != other.initials) return false
        if (avatar != null) {
            if (other.avatar == null) return false
            if (!avatar.contentEquals(other.avatar)) return false
        } else if (other.avatar != null) return false
        if (displayColor != other.displayColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + initials.hashCode()
        result = 31 * result + (avatar?.contentHashCode() ?: 0)
        result = 31 * result + (displayColor?.hashCode() ?: 0)
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MatrixClients.toAccountInfo(
    coroutineScope: CoroutineScope,
    settings: MatrixMessengerSettingsHolder,
    initials: Initials,
    maxMediaSizeInMemory: Long,
) = flatMapLatest { matrixClients ->
    combine(
        matrixClients.map { (userId, matrixClient) ->
            val serverDisplayNameFlow = matrixClient.profile.map { it?.displayName ?: userId.localpart }
            val avatarFlow =
                matrixClient.profile.map { profile ->
                    profile?.avatarUrl?.let { avatarUrl ->
                        matrixClient.media
                            .getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                            .fold(
                                onSuccess = { it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory) },
                                onFailure = {
                                    log.error(it) { "Cannot load user avatar" }
                                    null
                                },
                            )
                    }
                }
            combine(serverDisplayNameFlow, settings[userId], avatarFlow) { serverDisplayName, settings, avatar ->
                AccountInfo(
                    userId = userId,
                    displayName = settings?.base?.displayName ?: serverDisplayName,
                    initials = initials.compute(serverDisplayName),
                    avatar = avatar,
                    displayColor = settings?.base?.displayColor,
                )
            }
        }
    ) {
        it.toList()
    }
}
