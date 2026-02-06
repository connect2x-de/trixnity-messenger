package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface ProfilesSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseProfilesSettings: () -> Unit,
    ): ProfilesSettingsViewModel {
        return ProfilesSettingsViewModelImpl(
            viewModelContext,
            onCloseProfilesSettings,
        )
    }

    companion object : ProfilesSettingsViewModelFactory
}

interface ProfilesSettingsViewModel {
    val profilesSingleViewModels: StateFlow<Map<String, ProfilesSingleViewModel>>
    val activeProfile: StateFlow<String?>
    val isMultiProfile: StateFlow<Boolean>
    val canChangeMultiProfileMode: StateFlow<Boolean>
    val openedDialogueType: StateFlow<ProfileDialogue?>
    val openedDialogueProfileId: StateFlow<String?>
    fun openRenameDialogue(profileId: String)
    fun openSelectDialogue(profileId: String)
    fun openDeleteDialogue(profileId: String)
    fun openCreateDialogue()
    fun closeOpenedDialogue()
    fun setMultiProfileEnabled(enabled: Boolean)
    fun closeProfile()
    fun close()
}

class ProfilesSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseProfilesSettings: () -> Unit,
) : ProfilesSettingsViewModel, ViewModelContext by viewModelContext {
    private val profileManager = get<ProfileManager>()

    override val activeProfile: StateFlow<String?> = profileManager.activeProfile

    override val profilesSingleViewModels: StateFlow<Map<String, ProfilesSingleViewModel>> = profileManager.profiles.map {
        it.mapValues { (profileId, _) ->
            this@ProfilesSettingsViewModelImpl.get<ProfilesSingleViewModelFactory>()
                .create(
                    viewModelContext.childContext(profileId, this@ProfilesSettingsViewModelImpl),
                    profileId
                )
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    override val isMultiProfile: StateFlow<Boolean> =
        (profileManager.isMultiProfileEnabled.map { it == true })
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canChangeMultiProfileMode: StateFlow<Boolean> =
        combine(isMultiProfile, profileManager.profiles.map { it.size > 1 }) {
            val isMultiProfile = it[0]
            val moreThanOneProfile = it[1]
            // Technically, we could encounter a case where the multi-profile mode is disabled, but there are more than
            // one profiles. In this case, we should still allow the user to enable it.
            !isMultiProfile || (isMultiProfile && !moreThanOneProfile)
        }.stateIn(coroutineScope, WhileSubscribed(), true)

    override val openedDialogueType: MutableStateFlow<ProfileDialogue?> = MutableStateFlow(null)
    override val openedDialogueProfileId: MutableStateFlow<String?> = MutableStateFlow(null)
    override fun openRenameDialogue(profileId: String){
        openedDialogueType.value = ProfileDialogue.RENAME
        openedDialogueProfileId.value = profileId
    }
    override fun openSelectDialogue(profileId: String) {
        openedDialogueType.value = ProfileDialogue.SELECT
        openedDialogueProfileId.value = profileId
    }
    override fun openDeleteDialogue(profileId: String) {
        openedDialogueType.value = ProfileDialogue.DELETE
        openedDialogueProfileId.value = profileId
    }
    override fun openCreateDialogue(){
        openedDialogueType.value = ProfileDialogue.CREATE
        openedDialogueProfileId.value = activeProfile.value
    }
    override fun closeOpenedDialogue() {
        openedDialogueType.value = null
        openedDialogueProfileId.value = null
    }

    override fun closeProfile() {
        log.debug { "close profile" }
        coroutineScope.launch {
            profileManager.closeProfile()
        }
    }

    override fun setMultiProfileEnabled(enabled: Boolean) {
        coroutineScope.launch { profileManager.setMultiProfileEnabled(enabled) }
    }

    override fun close() {
        onCloseProfilesSettings()
    }
}

enum class ProfileDialogue{
    RENAME, DELETE, SELECT, CREATE
}

