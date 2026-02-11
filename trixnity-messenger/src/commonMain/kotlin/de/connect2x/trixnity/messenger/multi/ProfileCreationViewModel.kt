package de.connect2x.trixnity.messenger.multi

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.Koin

/**
 * In case of multiple profiles, this can create new profiles. Uses [ProfileManager] under the hood.
 */
interface ProfileCreationViewModel {
    /**
     * an error message on why a profile cannot be created
     */
    val error: StateFlow<String?>

    /**
     * is `true` when the profile can be created
     */
    val canCreateProfile: StateFlow<Boolean>

    val profileName: TextFieldViewModel
    fun createProfile()
    fun selectCreatedProfile()
}

/**
 * @param di e.g., in Compose, "inject" the DI via CompositionLocal
 * @param coroutineScope e.g., in Compose create in the UI component with `rememberCoroutineScope()`
 */
class ProfileCreationViewModelImpl(
    di: Koin,
    private val coroutineScope: CoroutineScope,
) : ProfileCreationViewModel {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.multi.ProfileCreationViewModelImpl")
    }

    private val profileManager = di.get<ProfileManager>()
    private val i18n = di.get<I18n>()

    override val profileName = TextFieldViewModelImpl(maxLength = 1_000)
    override val error: StateFlow<String?> =
        combine(profileManager.profiles, profileName) { existingProfiles, currentProfileName ->
            when {
                existingProfiles.any { (_, settings) -> settings.base.displayName == currentProfileName.text } -> i18n.profileCreationDuplicate()
                else -> null
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val canCreateProfile: StateFlow<Boolean> = error.map { it == null }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false) // used in createProfile()

    private var id: String? = null

    override fun createProfile() {
        if (canCreateProfile.value) {
            coroutineScope.launch {
                id = profileManager.createProfile(
                    settings = MatrixMultiMessengerProfileSettingsBase(displayName = profileName.value.text)
                )
            }
        } else {
            log.warn { "cannot create a profile" }
        }
    }

    override fun selectCreatedProfile() {
        val i: String? = id
        if(i != null){
            coroutineScope.launch {
                profileManager.selectProfile(i)
            }
        }
    }

}
