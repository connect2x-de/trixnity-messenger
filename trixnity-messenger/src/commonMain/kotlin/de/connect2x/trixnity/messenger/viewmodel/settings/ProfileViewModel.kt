package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.FileDescriptor
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
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

data class ProfileOfAccount(
    val accountName: String,
    val userId: String,
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
        onOpenAvatarCutter: (String, FileDescriptor) -> Unit,
    ): ProfileViewModel {
        return ProfileViewModelImpl(viewModelContext, onCloseProfile, onOpenAvatarCutter)
    }

    companion object : ProfileViewModelFactory
}

interface ProfileViewModel {
    val profilesOfAccounts: StateFlow<List<ProfileOfAccount>>
    val error: MutableStateFlow<String?>
    val openAvatarCutter: StateFlow<String?> // accountName

    fun close()
    fun errorDismiss()
    fun cancelEditDisplayName(accountName: String)
    fun saveDisplayName(accountName: String)
    fun openAvatarCutter(accountName: String, file: FileDescriptor)
    fun closeAvatarCutter()
}

@OptIn(ExperimentalCoroutinesApi::class)
open class ProfileViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseProfile: () -> Unit,
    private val onOpenAvatarCutter: (String, FileDescriptor) -> Unit,
) : ViewModelContext by viewModelContext, ProfileViewModel {

    private val initialsComputation = get<Initials>()

    override val profilesOfAccounts: StateFlow<List<ProfileOfAccount>>
    override val error = MutableStateFlow<String?>(null)
    override val openAvatarCutter: StateFlow<String?>

    private val backCallback = BackCallback {
        close()
    }

    init {
        backHandler.register(backCallback)
        profilesOfAccounts = namedMatrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (accountName, matrixClientFlow) ->
                log.trace { "profiles for account $accountName will be loaded" }
                val matrixClient = matrixClientFlow.value
                    ?: throw IllegalStateException("cannot find MatrixClient for account $accountName")
                val userId = matrixClient.userId
                ProfileOfAccount(
                    accountName = accountName,
                    userId = userId.full,
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
            combine(profilesOfAccounts.map { profileOfAccount -> profileOfAccount.openAvatarCutter.map { profileOfAccount.accountName to it } }) { list ->
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

    override fun cancelEditDisplayName(accountName: String) {
        getEditDisplayNameFlow(accountName)?.value =
            getDisplayNameFlow(accountName)?.value ?: ""
    }


    override fun saveDisplayName(accountName: String) {
        val newDisplayName = getEditDisplayNameFlow(accountName)?.value
        if (newDisplayName != getDisplayNameFlow(accountName)?.value) {
            coroutineScope.launch {
                namedMatrixClients.value.find { it.accountName == accountName }?.let { (_, matrixClientFlow) ->
                    matrixClientFlow.first()?.let { matrixClient ->
                        log.debug { "set new display name in account $accountName: $newDisplayName" }
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
        }
    }

    override fun openAvatarCutter(accountName: String, file: FileDescriptor) {
        onOpenAvatarCutter(accountName, file)
    }

    override fun closeAvatarCutter() {
        profilesOfAccounts.value.forEach { profileOfAccount ->
            profileOfAccount.openAvatarCutter.value = false
        }
    }

    private fun getDisplayNameFlow(accountName: String) =
        profilesOfAccounts.value.find { it.accountName == accountName }?.displayName

    private fun getEditDisplayNameFlow(accountName: String) =
        profilesOfAccounts.value.find { it.accountName == accountName }?.editDisplayName
}
