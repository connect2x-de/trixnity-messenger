package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger {}

interface ProfileManager {
    val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>>
    val activeProfile: StateFlow<String?>
    val activeMatrixMessenger: StateFlow<MatrixMessenger?>

    suspend fun closeProfile()
    suspend fun selectProfile(profile: String)
    suspend fun createProfile(settings: MatrixMultiMessengerProfileSettings = MatrixMultiMessengerProfileSettings()): String
    suspend fun updateProfile(
        profile: String,
        updateSettings: (MatrixMultiMessengerProfileSettings) -> MatrixMultiMessengerProfileSettings
    )

    suspend fun deleteProfile(profile: String)
}

class ProfileManagerImpl(
    private val settingsHolder: MatrixMultiMessengerSettingsHolder,
    private val matrixMessengerFactory: MatrixMessengerFactory,
    private val deleteProfileData: DeleteProfileData,
    coroutineScope: CoroutineScope,
) : ProfileManager {
    override val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>> =
        settingsHolder.map { it.profiles }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.profiles)
    override val activeProfile: StateFlow<String?> =
        settingsHolder.map { it.activeProfile }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.activeProfile)

    override val activeMatrixMessenger: StateFlow<MatrixMessenger?> =
        activeProfile.map { profile ->
            if (profile == null) null
            else matrixMessengerFactory(profile)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override suspend fun closeProfile() {
        log.debug { "close current profile ${activeProfile.value}" }
        activeMatrixMessenger.value?.stop()
        settingsHolder.update { it.copy(activeProfile = null) }
    }

    override suspend fun selectProfile(profile: String) {
        log.debug { "select profile $profile" }
        closeProfile()
        settingsHolder.update {
            if (it.profiles.containsKey(profile)) it.copy(activeProfile = profile)
            else it
        }
    }

    override suspend fun createProfile(settings: MatrixMultiMessengerProfileSettings): String {
        log.debug { "create profile" }
        var nextId: String? = null
        settingsHolder.update { oldSettings ->
            val updateNextId = generateSequence(0) { it + 1 }.map { it.toString() }
                .filterNot { oldSettings.profiles.containsKey(it) }
                .first()
            nextId = updateNextId
            oldSettings.copy(profiles = oldSettings.profiles + (updateNextId to settings))
        }
        return checkNotNull(nextId)
    }

    override suspend fun updateProfile(
        profile: String,
        updateSettings: (MatrixMultiMessengerProfileSettings) -> MatrixMultiMessengerProfileSettings
    ) {
        settingsHolder.update { oldSettings ->
            val newProfileSettings = oldSettings.profiles[profile]?.also { updateSettings(it) }
            if (newProfileSettings != null)
                oldSettings.copy(profiles = oldSettings.profiles + (profile to newProfileSettings))
            else oldSettings
        }
    }

    override suspend fun deleteProfile(profile: String) {
        log.debug { "delete profile $profile" }
        if (activeProfile.value == profile) closeProfile()
        withContext(NonCancellable) {
            settingsHolder.update { oldSettings ->
                oldSettings.copy(
                    profiles = oldSettings.profiles - profile,
                    activeProfile = if (oldSettings.activeProfile == profile) null else oldSettings.activeProfile
                )
            }
            deleteProfileData(profile)
        }
    }
}
