package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.media
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteArray

private val log = KotlinLogging.logger { }

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
fun MatrixClients.toAccountInfo(settings: MatrixMessengerSettingsHolder, initials: Initials) =
    flatMapLatest { matrixClients ->
        combine(
            matrixClients.map { (userId, matrixClient) ->
                val serverDisplayNameFlow = matrixClient.displayName.map { it ?: userId.localpart }
                val avatarFlow = matrixClient.avatarUrl.map { avatarUrlOrNull ->
                    avatarUrlOrNull?.let { avatarUrl ->
                        matrixClient.media.getThumbnail(
                            avatarUrl,
                            avatarSize().toLong(),
                            avatarSize().toLong(),
                        ).fold(
                            onSuccess = {
                                val maxPreviewSize = MatrixMessengerConfiguration().filePreviewMaxSize
                                try {
                                    it.limitSize(maxPreviewSize).toByteArray()
                                } catch (_: Exception) {
                                    log.error { "Avatar for $userId exceeds preview size limits, so it's not displayed" }
                                    null
                                }
                            },
                            onFailure = {
                                log.error(it) { "Cannot load user avatar" }
                                null
                            }
                        )
                    }
                }
                combine(
                    serverDisplayNameFlow,
                    settings[userId],
                    avatarFlow
                ) { serverDisplayName, settings, avatar ->
                    AccountInfo(
                        userId = userId,
                        displayName = settings?.base?.displayName ?: serverDisplayName,
                        initials = initials.compute(serverDisplayName),
                        avatar = avatar,
                        displayColor = settings?.base?.displayColor,
                    )
                }
            }
        ) { it.toList() }
    }
