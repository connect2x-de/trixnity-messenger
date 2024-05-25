package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.i18n.I18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.Koin

private val log = KotlinLogging.logger { }

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

    val profileName: MutableStateFlow<String>
    fun createProfile()
}

/**
 * @param di e.g., in Compose, "inject" the DI via CompositionLocal
 * @param coroutineScope e.g., in Compose create in the UI component with `rememberCoroutineScope()`
 */
class ProfileCreationViewModelImpl(
    di: Koin,
    private val coroutineScope: CoroutineScope,
) : ProfileCreationViewModel {
    private val profileManager = di.get<ProfileManager>()
    private val i18n = di.get<I18n>()

    override val profileName: MutableStateFlow<String> = MutableStateFlow("")
    override val error: StateFlow<String?> =
        combine(profileManager.profiles, profileName) { existingProfiles, currentProfileName ->
            when {
                existingProfiles.any { (_, settings) -> settings.base.displayName == currentProfileName } -> i18n.profileCreationDuplicate()
                else -> null
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val canCreateProfile: StateFlow<Boolean> = error.map { it == null }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true) // used in createProfile()

    override fun createProfile() {
        if (canCreateProfile.value) {
            coroutineScope.launch {
                val id = profileManager.createProfile(
                    settings = MatrixMultiMessengerProfileSettingsBase(displayName = profileName.value)
                )
                profileManager.selectProfile(id)
            }
        } else {
            log.warn { "cannot create a profile" }
        }
    }

}
