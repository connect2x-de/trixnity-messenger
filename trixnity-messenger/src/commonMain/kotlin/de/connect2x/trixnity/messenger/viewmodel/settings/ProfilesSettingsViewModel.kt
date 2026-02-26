package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettingsBase
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
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
    val profiles: StateFlow<Map<String, ProfilesSettingsSingleViewModel>>
    val activeProfile: StateFlow<String?>
    val isMultiProfile: StateFlow<Boolean>
    val canChangeMultiProfileMode: StateFlow<Boolean>
    val profileCreationTextField: TextFieldViewModel
    val profileCreationError: StateFlow<String?>
    fun createProfile()
    fun setMultiProfileEnabled(enabled: Boolean)
    fun closeProfile()
    fun close()
}

class ProfilesSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseProfilesSettings: () -> Unit,
) : ProfilesSettingsViewModel, ViewModelContext by viewModelContext {
    private val di = viewModelContext.getKoin()
    private val i18n = di.get<I18n>()
    private val profileManager = get<ProfileManager>()

    override val activeProfile: StateFlow<String?> = profileManager.activeProfile

    override val profiles: StateFlow<Map<String, ProfilesSettingsSingleViewModel>> =
        profileManager.profiles.map {
            it.mapValues { (profileId, _) ->
                this@ProfilesSettingsViewModelImpl.get<ProfilesSettingsSingleViewModelFactory>()
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

    override val profileCreationTextField: TextFieldViewModel = TextFieldViewModelImpl(maxLength = 1_000)
    override val profileCreationError: StateFlow<String?> =
        combine(profileManager.profiles, profileCreationTextField) { existingProfiles, currentNewProfileName ->
            when {
                existingProfiles.any { (_, settings) -> settings.base.displayName == currentNewProfileName.text } -> i18n.profileCreationDuplicate()
                else -> null
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun createProfile() {
        if (profileCreationError.value == null) {
            coroutineScope.launch {
                profileManager.createProfile(
                    settings = MatrixMultiMessengerProfileSettingsBase(displayName = profileCreationTextField.value.text)
                )
            }
        } else {
            log.warn { "cannot create a profile" }
        }
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

