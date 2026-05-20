package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.clientserverapi.model.server.profileFields
import de.connect2x.trixnity.clientserverapi.model.user.ProfileField
import de.connect2x.trixnity.clientserverapi.model.user.displayName
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

@Deprecated("Use AccountSingleViewModelFactory")
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

    @Suppress("DEPRECATION") companion object : ProfileSingleViewModelFactory
}

interface AccountSingleViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        userId: UserId,
        error: MutableStateFlow<String?>,
        showAccountSetup: () -> Unit,
        removeAccount: () -> Unit,
    ): AccountSingleViewModel {
        return AccountSingleViewModelImpl(viewModelContext, userId, error, showAccountSetup, removeAccount)
    }

    companion object : AccountSingleViewModelFactory
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

    fun cancelEditDisplayName()

    fun saveDisplayName()

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

    override val displayName =
        matrixClient.profile
            .map { it?.get(ProfileField.DisplayName)?.value ?: "" }
            .stateIn(coroutineScope, SharingStarted.Eagerly, userId.localpart)
    override val canChangeDisplayName: StateFlow<Boolean> =
        matrixClient.serverData
            .map { it?.capabilities?.capabilities?.profileFields?.enabled ?: true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    override val avatar =
        matrixClient.profile
            .map { profile ->
                profile?.get(ProfileField.AvatarUrl)?.let { avatarUrl ->
                    avatarUrl.value?.let { avatarUrl ->
                        matrixClient.media
                            .getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                            .fold(
                                onSuccess = { it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory) },
                                onFailure = {
                                    log.error(it) { "Cannot load user avatar." }
                                    error.value = i18n.profileLoadError()
                                    null
                                },
                            )
                    }
                }
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override val canChangeAvatar =
        matrixClient.serverData
            .map { it?.capabilities?.capabilities?.profileFields?.enabled ?: true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val initials =
        matrixClient.profile
            .map { it?.get(ProfileField.DisplayName)?.value?.let { initialsComputation.compute(it) } ?: "" }
            .stateIn(coroutineScope, SharingStarted.Eagerly, "")

    override val editDisplayName =
        TextFieldViewModelImpl(maxLength = 1_000, matrixClient.profile.value?.displayName ?: "")

    override val openAvatarCutter = MutableStateFlow(false)

    override fun cancelEditDisplayName() {
        editDisplayName.also { it.update(displayName.value) }
    }

    override fun saveDisplayName() {
        val newDisplayName = editDisplayName.value.text
        if (newDisplayName != displayName.value) {
            coroutineScope.launch {
                val matrixClient = getMatrixClient(userId)
                if (matrixClient.serverData.value?.capabilities?.capabilities?.profileFields?.enabled ?: true) {
                    log.debug { "set new display name in account $userId: $newDisplayName" }
                    matrixClient.setProfileField(ProfileField.DisplayName(newDisplayName)).onFailure {
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

    override fun logout() = removeAccount()

    override fun resetSetup() = showAccountSetup()
}
