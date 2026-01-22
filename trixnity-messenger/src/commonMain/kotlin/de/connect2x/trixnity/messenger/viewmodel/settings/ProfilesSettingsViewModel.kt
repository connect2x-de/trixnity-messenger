package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull

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
    val profileNameTextField: ApprovableTextFieldViewModel
    val isMultiProfile: StateFlow<Boolean>
    val canChangeMultiProfileMode: StateFlow<Boolean>
    val shouldShowProfileRename: StateFlow<Boolean>

    fun setMultiProfileEnabled(enabled: Boolean)
    fun close()
}

class ProfilesSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseProfilesSettings: () -> Unit,
) : ProfilesSettingsViewModel, ViewModelContext by viewModelContext {
    private val profileManager = getOrNull<ProfileManager>() //change

    override val profileNameTextField: ApprovableTextFieldViewModel =
        ApprovableTextFieldViewModelImpl(
            serverValue = flowOf(""), //change
            maxLength = 1000,
            coroutineScope = coroutineScope,
            onApplyChange = { changeProfileNameAndUseMultipleProfile(it) },
        )

    override val isMultiProfile: StateFlow<Boolean> =
        (profileManager?.isMultiProfileEnabled?.map { it != null && it } ?: flowOf(false))
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canChangeMultiProfileMode: StateFlow<Boolean> =
        combine(isMultiProfile, profileManager?.profiles?.map { it.size > 1 } ?: flowOf(false)) {
            val isMultiProfile = it[0]
            val moreThanOneProfile = it[1]
            // Technically, we could encounter a case where the multi-profile mode is disabled, but there are more than
            // one profiles. In this case, we should still allow the user to enable it.
            !isMultiProfile || (isMultiProfile && !moreThanOneProfile)
        }.stateIn(coroutineScope, WhileSubscribed(), true)

    override val shouldShowProfileRename: StateFlow<Boolean> = (profileManager?.profiles
        ?.combine(profileManager.activeProfile){ profiles, activeProfile ->
            profiles[activeProfile]?.get("displayName") == JsonNull
        }?: flowOf(false))
        .stateIn(coroutineScope, WhileSubscribed(), false)

    private fun changeProfileNameAndUseMultipleProfile(newName: String) : Result<String>{
        val activeProfile = profileManager?.activeProfile?.value
        if(activeProfile!= null){
            coroutineScope.launch {
                profileManager.updateProfile(activeProfile) {
                    set("displayName", Json.parseToJsonElement(newName))
                }
            }
            setMultiProfileEnabled(true)
            if(profileManager.profiles.value[activeProfile]?.get("displayName") == null){
                return Result.failure(IllegalStateException("There was no current profile selected, while trying to change it's name!"))
            }
        }
        return Result.success(newName)
    }

    override fun setMultiProfileEnabled(enabled: Boolean) {
        coroutineScope.launch { profileManager?.setMultiProfileEnabled(enabled) }
    }

    override fun close() {
        onCloseProfilesSettings()
    }
}
