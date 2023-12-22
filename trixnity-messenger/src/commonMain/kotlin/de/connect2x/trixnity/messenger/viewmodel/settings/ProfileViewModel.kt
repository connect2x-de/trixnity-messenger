package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

data class ProfileOfAccount(
    val userId: UserId,
    val displayName: StateFlow<String>,
    val avatar: StateFlow<ByteArray?>,
    val initials: StateFlow<String>,
    val editDisplayName: MutableStateFlow<String>,
    val openAvatarCutter: MutableStateFlow<Boolean>,
)

interface ProfileViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseProfile: () -> Unit,
        onOpenAvatarCutter: (UserId, FileDescriptor) -> Unit,
    ): ProfileViewModel {
        return ProfileViewModelImpl(viewModelContext, onCloseProfile, onOpenAvatarCutter)
    }

    companion object : ProfileViewModelFactory
}

interface ProfileViewModel {
    val profilesOfAccounts: StateFlow<List<ProfileOfAccount>>
    val error: MutableStateFlow<String?>
    val openAvatarCutter: StateFlow<UserId?>

    fun close()
    fun errorDismiss()
    fun cancelEditDisplayName(userId: UserId)
    fun saveDisplayName(userId: UserId)
    fun openAvatarCutter(userId: UserId, file: FileDescriptor)
    fun closeAvatarCutter()
}

@OptIn(ExperimentalCoroutinesApi::class)
open class ProfileViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseProfile: () -> Unit,
    private val onOpenAvatarCutter: (UserId, FileDescriptor) -> Unit,
) : ViewModelContext by viewModelContext, ProfileViewModel {

    private val initialsComputation = get<Initials>()

    override val profilesOfAccounts: StateFlow<List<ProfileOfAccount>>
    override val error = MutableStateFlow<String?>(null)
    override val openAvatarCutter: StateFlow<UserId?>

    private val backCallback = BackCallback {
        close()
    }

    init {
        backHandler.register(backCallback)
        profilesOfAccounts = matrixClients.scopedMapLatest { matrixClients ->
            matrixClients.map { (userId, matrixClient) ->
                log.trace { "profiles for account $userId will be loaded" }
                ProfileOfAccount(
                    userId = userId,
                    displayName = matrixClient.displayName.map { it ?: userId.localpart }
                        .stateIn(this, SharingStarted.Eagerly, userId.localpart),
                    avatar = matrixClient.avatarUrl.map { avatarUrl ->
                        avatarUrl?.let {
                            matrixClient.media.getThumbnail(
                                avatarUrl.toString(),
                                avatarSize().toLong(),
                                avatarSize().toLong()
                            ).fold(
                                onSuccess = { it.toByteArray() },
                                onFailure = {
                                    log.error(it) { "Cannot load user avatar." }
                                    error.value = i18n.profileLoadError()
                                    null
                                }
                            )
                        }
                    }.stateIn(this, SharingStarted.Eagerly, null),
                    initials = matrixClient.displayName.map { it?.let { initialsComputation.compute(it) } ?: "" }
                        .stateIn(this, SharingStarted.Eagerly, ""),
                    editDisplayName = MutableStateFlow(matrixClient.displayName.value ?: ""),
                    openAvatarCutter = MutableStateFlow(false),
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

        openAvatarCutter = profilesOfAccounts.flatMapLatest { profilesOfAccounts ->
            combine(profilesOfAccounts.map { profileOfAccount -> profileOfAccount.openAvatarCutter.map { profileOfAccount.userId to it } }) { list ->
                list.find { (_, openAvatarChooser) -> openAvatarChooser }?.first
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)
    }

    override fun close() {
        onCloseProfile()
    }

    override fun errorDismiss() {
        error.value = null
    }

    override fun cancelEditDisplayName(userId: UserId) {
        getEditDisplayNameFlow(userId)?.value =
            getDisplayNameFlow(userId)?.value ?: ""
    }


    override fun saveDisplayName(userId: UserId) {
        val newDisplayName = getEditDisplayNameFlow(userId)?.value
        if (newDisplayName != getDisplayNameFlow(userId)?.value) {
            coroutineScope.launch {
                val matrixClient = getMatrixClient(userId)
                log.debug { "set new display name in account $userId: $newDisplayName" }
                matrixClient.setDisplayName(newDisplayName)
                    .onFailure {
                        log.error(it) { "Cannot set display name." }
                        if (it is MatrixServerException && it.errorResponse is ErrorResponse.Forbidden) {
                            error.value = i18n.profileNameForbidden()
                        } else {
                            error.value = i18n.profileNameError()
                        }
                    }
            }
        }
    }

    override fun openAvatarCutter(userId: UserId, file: FileDescriptor) {
        onOpenAvatarCutter(userId, file)
    }

    override fun closeAvatarCutter() {
        profilesOfAccounts.value.forEach { profileOfAccount ->
            profileOfAccount.openAvatarCutter.value = false
        }
    }

    private fun getDisplayNameFlow(userId: UserId) =
        profilesOfAccounts.value.find { it.userId == userId }?.displayName

    private fun getEditDisplayNameFlow(userId: UserId) =
        profilesOfAccounts.value.find { it.userId == userId }?.editDisplayName
}
