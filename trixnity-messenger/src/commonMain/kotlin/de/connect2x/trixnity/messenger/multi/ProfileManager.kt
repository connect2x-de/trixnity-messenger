package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.settings.settingsJson
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

private val log = KotlinLogging.logger {}

interface ProfileManager {
    val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>>
    val activeProfile: StateFlow<String?>
    val activeMatrixMessenger: StateFlow<MatrixMessenger?>

    suspend fun closeProfile()
    suspend fun selectProfile(profile: String)
    suspend fun createProfile(settings: MatrixMultiMessengerProfileSettingsBase = MatrixMultiMessengerProfileSettingsBase()): String
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
        settingsHolder.map { it.base.profiles }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.base.profiles)
    override val activeProfile: StateFlow<String?> =
        settingsHolder.map { it.base.activeProfile }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.base.activeProfile)

    override val activeMatrixMessenger: StateFlow<MatrixMessenger?> =
        activeProfile.map { profile ->
            if (profile == null) null
            else matrixMessengerFactory(profile)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override suspend fun closeProfile() {
        log.debug { "close current profile ${activeProfile.value}" }
        activeMatrixMessenger.value?.stop()
        settingsHolder.update<MatrixMultiMessengerSettingsBase> { it.copy(activeProfile = null) }
    }

    override suspend fun selectProfile(profile: String) {
        log.debug { "select profile $profile" }
        closeProfile()
        settingsHolder.update<MatrixMultiMessengerSettingsBase> {
            if (it.profiles.containsKey(profile)) it.copy(activeProfile = profile)
            else it
        }
    }

    override suspend fun createProfile(settings: MatrixMultiMessengerProfileSettingsBase): String {
        log.debug { "create profile" }
        val profileSettings = MatrixMultiMessengerProfileSettings(
            checkNotNull(settingsJson.encodeToJsonElement(settings) as? JsonObject)
        )
        var nextId: String? = null
        settingsHolder.update<MatrixMultiMessengerSettingsBase> { oldSettings ->
            val updateNextId = generateSequence(0) { it + 1 }.map { it.toString() }
                .filterNot { oldSettings.profiles.containsKey(it) }
                .first()
            nextId = updateNextId
            oldSettings.copy(profiles = oldSettings.profiles + (updateNextId to profileSettings))
        }
        return checkNotNull(nextId)
    }

    override suspend fun updateProfile(
        profile: String,
        updateSettings: (MatrixMultiMessengerProfileSettings) -> MatrixMultiMessengerProfileSettings
    ) {
        settingsHolder.update<MatrixMultiMessengerSettingsBase> { oldSettings ->
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
            settingsHolder.update<MatrixMultiMessengerSettingsBase> { oldSettings ->
                oldSettings.copy(
                    profiles = oldSettings.profiles - profile,
                    activeProfile = if (oldSettings.activeProfile == profile) null else oldSettings.activeProfile
                )
            }
            deleteProfileData(profile)
        }
    }
}
