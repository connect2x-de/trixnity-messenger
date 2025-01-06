package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.server.setAvatarUrl
import net.folivo.trixnity.clientserverapi.model.server.setDisplayName
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

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
    val profileSingleViewModels: StateFlow<List<ProfileSingleViewModel>>
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
class ProfileViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseProfile: () -> Unit,
    private val onOpenAvatarCutter: (UserId, FileDescriptor) -> Unit,
) : ViewModelContext by viewModelContext, ProfileViewModel {

    override val profileSingleViewModels: StateFlow<List<ProfileSingleViewModel>>
    override val error = MutableStateFlow<String?>(null)
    override val openAvatarCutter: StateFlow<UserId?>

    private val backCallback = BackCallback {
        close()
    }

    init {
        backHandler.register(backCallback)
        profileSingleViewModels = matrixClients.scopedMapLatest { matrixClients ->
            matrixClients.map { (userId, _) ->
                get<ProfileSingleViewModelFactory>().create(
                    viewModelContext.childContext(this@ProfileViewModelImpl),
                    userId,
                    error,
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

        openAvatarCutter = profileSingleViewModels.flatMapLatest { profilesOfAccounts ->
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
                if (matrixClient.serverData.value?.capabilities?.capabilities?.setDisplayName?.enabled == true) {
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
                } else {
                    log.warn { "Missing server capability to set the display name." }
                }
            }
        }
    }

    override fun openAvatarCutter(userId: UserId, file: FileDescriptor) {
        if (getMatrixClient(userId).serverData.value?.capabilities?.capabilities?.setAvatarUrl?.enabled == true) {
            onOpenAvatarCutter(userId, file)
        } else {
            log.warn { "Missing server capability to change the user avatar." }
        }
    }

    override fun closeAvatarCutter() {
        profileSingleViewModels.value.forEach { profileOfAccount ->
            profileOfAccount.openAvatarCutter.value = false
        }
    }

    private fun getDisplayNameFlow(userId: UserId) =
        profileSingleViewModels.value.find { it.userId == userId }?.displayName

    private fun getEditDisplayNameFlow(userId: UserId) =
        profileSingleViewModels.value.find { it.userId == userId }?.editDisplayName
}
