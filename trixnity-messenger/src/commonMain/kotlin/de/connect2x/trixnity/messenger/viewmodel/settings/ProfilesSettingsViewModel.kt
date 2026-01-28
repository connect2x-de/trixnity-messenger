package de.connect2x.trixnity.messenger.viewmodel.settings

import org.koin.core.component.get
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettingsBase
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.multi.updateProfile
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface ProfilesSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseProfilesSettings: () -> Unit,
    ):  ProfilesSettingsViewModel{
        return ProfilesSettingsViewModelImpl(
            viewModelContext,
            onCloseProfilesSettings,
        )
    }

    companion object : ProfilesSettingsViewModelFactory
}

interface ProfilesSettingsViewModel {
    val isMultiProfile: StateFlow<Boolean>
    val canChangeMultiProfileMode: StateFlow<Boolean>
    val profileNameTextFieldViewModel: ApprovableTextFieldViewModel

    fun setMultiProfileEnabled(enabled: Boolean)
    fun close()
}

class ProfilesSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseProfilesSettings: () -> Unit,
) : ProfilesSettingsViewModel, ViewModelContext by viewModelContext {
    private val profileManager = get<ProfileManager>()

    override val isMultiProfile: StateFlow<Boolean> =
        (profileManager.isMultiProfileEnabled.map { it == true })
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canChangeMultiProfileMode: StateFlow<Boolean> =
        combine(isMultiProfile, profileManager.profiles.map { it.size > 1 } ) {
            val isMultiProfile = it[0]
            val moreThanOneProfile = it[1]
            // Technically, we could encounter a case where the multi-profile mode is disabled, but there are more than
            // one profiles. In this case, we should still allow the user to enable it.
            !isMultiProfile || (isMultiProfile && !moreThanOneProfile)
        }.stateIn(coroutineScope, WhileSubscribed(), true)

    private val currentProfileName: StateFlow<String?> = (profileManager.profiles
        .combine(profileManager.activeProfile){ profiles, activeProfile ->
            profiles[activeProfile]?.base?.displayName
        }).stateIn(coroutineScope, WhileSubscribed(), null)

    override val profileNameTextFieldViewModel: ApprovableTextFieldViewModel =
        ApprovableTextFieldViewModelImpl(
            serverValue = currentProfileName.map { it?: "" },
            maxLength = 100,
            coroutineScope = coroutineScope,
            onApplyChange = {
                changeProfileName(it)
                if(currentProfileName.value != it){
                    Result.failure<String>(IllegalStateException("Changing profile name failed"))
                }else{
                    Result.success(it)
                }
            },
        )

    private suspend fun changeProfileName(newName: String){
        val activeProfile = profileManager.activeProfile.value
        if(activeProfile!= null){
            profileManager.updateProfile<MatrixMultiMessengerProfileSettingsBase>(activeProfile){
                it.copy(displayName = newName)
            }
        }
    }

    override fun setMultiProfileEnabled(enabled: Boolean) {
        coroutineScope.launch { profileManager.setMultiProfileEnabled(enabled) }
    }

    override fun close() {
        onCloseProfilesSettings()
    }
}
