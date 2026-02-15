package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettingsBase
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.multi.updateProfile
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface ProfilesSettingsSingleViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        profileId: String
    ): ProfilesSettingsSingleViewModel {
        return ProfilesSettingsSingleViewModelImpl(viewModelContext, profileId)
    }

    companion object : ProfilesSettingsSingleViewModelFactory
}

interface ProfilesSettingsSingleViewModel {
    val profileId: String
    val profileName: StateFlow<String>
    fun renameProfile(newName: String)
    fun selectProfile()
    fun deleteProfile()
}

class ProfilesSettingsSingleViewModelImpl(
    viewModelContext: ViewModelContext,
    override val profileId: String,
) : ProfilesSettingsSingleViewModel, ViewModelContext by viewModelContext {
    private val profileManager = get<ProfileManager>()
    private val settings = profileManager.profiles.map { it[profileId] }.shareIn(coroutineScope, Eagerly, replay = 1)

    override val profileName: StateFlow<String> =
        settings.map { it?.base?.displayName ?: "unknown" }.stateIn(coroutineScope, Eagerly, "unknown")

    override fun renameProfile(newName: String) {
        coroutineScope.launch {
            if(!profileManager.profiles.value.any { (_, settings) ->  settings.base.displayName == newName}){
                profileManager.updateProfile<MatrixMultiMessengerProfileSettingsBase>(profileId) {
                    it.copy(displayName = newName)
                }
            }else{
                log.warn {"Rename failed! A profile with the name $newName already exists" }
            }
        }
    }

    override fun selectProfile() {
        log.debug { "select profile" }
        coroutineScope.launch {
            profileManager.selectProfile(profileId)
        }
    }

    override fun deleteProfile() {
        log.debug { "delete profile" }
        coroutineScope.launch {
            profileManager.deleteProfile(profileId)
        }
    }
}
