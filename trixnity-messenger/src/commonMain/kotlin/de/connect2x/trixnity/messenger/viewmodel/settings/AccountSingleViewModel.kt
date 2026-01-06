package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
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
        showAccountSetup: () -> Unit,
        removeAccount: () -> Unit,
    ): AccountSingleViewModel {
        return AccountSingleViewModelImpl(viewModelContext, userId, error, showAccountSetup, removeAccount)
    }

    companion object : ProfileSingleViewModelFactory
}

interface AccountSingleViewModel {
    val userId: UserId
    val displayName: StateFlow<String>
    val canChangeDisplayName: StateFlow<Boolean>
    val avatar: StateFlow<ByteArray?>
    val canChangeAvatar: StateFlow<Boolean>
    val initials: StateFlow<String>
    val editDisplayName: TextFieldViewModelImpl
    val openAvatarCutter: MutableStateFlow<Boolean>

    fun logout()
    fun resetSetup()
}

class AccountSingleViewModelImpl(
    viewModelContext: ViewModelContext,
    override val userId: UserId,
    private val error: MutableStateFlow<String?>,
    private val showAccountSetup: () -> Unit,
    private val removeAccount: () -> Unit,
) : AccountSingleViewModel, ViewModelContext by viewModelContext {
    private val matrixClient = getMatrixClient(userId)
    private val initialsComputation = get<Initials>()

    override val displayName = matrixClient.displayName.map { it ?: "" }
        .stateIn(coroutineScope, SharingStarted.Eagerly, userId.localpart)
    override val canChangeDisplayName: StateFlow<Boolean> =
        matrixClient.serverData.map { it?.capabilities?.capabilities?.setDisplayName?.enabled ?: true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    override val avatar = matrixClient.avatarUrl.map { avatarUrl ->
        avatarUrl?.let {
            matrixClient.media.getThumbnail(
                avatarUrl,
                avatarSize().toLong(),
                avatarSize().toLong()
            ).fold(
                onSuccess = {
                    it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
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

    override val editDisplayName = TextFieldViewModelImpl(maxLength = 1_000, matrixClient.displayName.value ?: "")

    override val openAvatarCutter = MutableStateFlow(false)

    override fun logout() = removeAccount()

    override fun resetSetup() = showAccountSetup()
}
