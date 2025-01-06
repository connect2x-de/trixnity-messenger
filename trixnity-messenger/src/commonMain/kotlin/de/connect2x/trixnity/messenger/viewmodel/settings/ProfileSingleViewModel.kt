package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.server.setAvatarUrl
import net.folivo.trixnity.clientserverapi.model.server.setDisplayName
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface ProfileSingleViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId,
        error: MutableStateFlow<String?>,
    ): ProfileSingleViewModel {
        return ProfileSingleViewModelImpl(viewModelContext, userId, error)
    }

    companion object : ProfileSingleViewModelFactory
}

interface ProfileSingleViewModel {
    val userId: UserId
    val displayName: StateFlow<String>
    val canChangeDisplayName: StateFlow<Boolean>
    val avatar: StateFlow<ByteArray?>
    val canChangeAvatar: StateFlow<Boolean>
    val initials: StateFlow<String>
    val editDisplayName: MutableStateFlow<String>
    val openAvatarCutter: MutableStateFlow<Boolean>
}

class ProfileSingleViewModelImpl(
    viewModelContext: ViewModelContext,
    override val userId: UserId,
    private val error: MutableStateFlow<String?>,
) : ProfileSingleViewModel, ViewModelContext by viewModelContext {
    private val matrixClient = getMatrixClient(userId)
    private val initialsComputation = get<Initials>()

    override val displayName = matrixClient.displayName.map { it ?: userId.localpart }
        .stateIn(coroutineScope, SharingStarted.Eagerly, userId.localpart)
    override val canChangeDisplayName: StateFlow<Boolean> =
        matrixClient.serverData.map { it?.capabilities?.capabilities?.setDisplayName?.enabled ?: true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    private val maxAvatarSize = get<MatrixMessengerConfiguration>().avatarMaxSize
    override val avatar = matrixClient.avatarUrl.map { avatarUrl ->
        avatarUrl?.let {
            matrixClient.media.getThumbnail(
                avatarUrl.toString(),
                avatarSize().toLong(),
                avatarSize().toLong()
            ).fold(
                onSuccess = {
                    it.limitedByteArrayOrNull(maxAvatarSize) {
                        log.error { "User avatar for $userId exceeds max preview size, so it's not displayed" }
                    }
                },
                onFailure = {
                    log.error(it) { "Cannot load user avatar." }
                    error.value = i18n.profileLoadError()
                    null
                }
            )
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val canChangeAvatar =
        matrixClient.serverData.map { it?.capabilities?.capabilities?.setAvatarUrl?.enabled ?: true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val initials = matrixClient.displayName.map { it?.let { initialsComputation.compute(it) } ?: "" }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "")

    override val editDisplayName = MutableStateFlow(matrixClient.displayName.value ?: "")

    override val openAvatarCutter = MutableStateFlow(false)
}
