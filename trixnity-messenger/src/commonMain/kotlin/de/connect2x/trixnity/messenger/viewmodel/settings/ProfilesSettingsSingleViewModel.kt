package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettingsBase
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.multi.updateProfile
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface ProfilesSettingsSingleViewModelFactory {
    fun create(viewModelContext: ViewModelContext, profileId: String): ProfilesSettingsSingleViewModel {
        return ProfilesSettingsSingleViewModelImpl(viewModelContext, profileId)
    }

    companion object : ProfilesSettingsSingleViewModelFactory
}

interface ProfilesSettingsSingleViewModel {
    val profileId: String
    val profileName: StateFlow<String>
    val profileNameTextField: TextFieldViewModel
    val profileNameError: StateFlow<String?>

    /** Change profile according to the value in [profileName] */
    fun changeProfileName()

    fun selectProfile()

    fun deleteProfile()
}

class ProfilesSettingsSingleViewModelImpl(viewModelContext: ViewModelContext, override val profileId: String) :
    ProfilesSettingsSingleViewModel, ViewModelContext by viewModelContext {
    private val profileManager = get<ProfileManager>()
    private val i18n = get<I18n>()

    override val profileName: StateFlow<String> =
        profileManager.profiles
            .map { it[profileId]?.base?.displayName ?: "" }
            .stateIn(coroutineScope, WhileSubscribed(), "")

    override val profileNameTextField: TextFieldViewModel =
        TextFieldViewModelImpl(
            initialText = profileManager.profiles.value[profileId]?.base?.displayName ?: "",
            maxLength = 1_000,
        )

    override val profileNameError: StateFlow<String?> =
        combine(profileManager.profiles, profileNameTextField.text) { profiles, newName ->
                if (
                    profiles.any { settings ->
                        (settings.value.base.displayName == newName) && (settings.key != profileId)
                    }
                ) {
                    i18n.profileRenameDialogueError()
                } else {
                    null
                }
            }
            .stateIn(coroutineScope, Eagerly, null)

    override fun changeProfileName() {
        coroutineScope.launch {
            if (profileNameError.value == null) {
                profileManager.updateProfile<MatrixMultiMessengerProfileSettingsBase>(profileId) {
                    it.copy(displayName = profileNameTextField.textValue)
                }
            } else {
                log.warn { "Rename failed! A profile with the name ${profileNameTextField.textValue} already exists" }
            }
        }
    }

    override fun selectProfile() {
        log.debug { "select profile" }
        coroutineScope.launch { profileManager.selectProfile(profileId) }
    }

    override fun deleteProfile() {
        log.debug { "delete profile" }
        coroutineScope.launch { profileManager.deleteProfile(profileId) }
    }
}
