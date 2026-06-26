package de.connect2x.trixnity.messenger.multi

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.settings.MutableSettings
import de.connect2x.trixnity.messenger.settings.MutableSettingsImpl
import de.connect2x.trixnity.messenger.settings.SettingsJson
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.get
import de.connect2x.trixnity.messenger.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer

interface ProfileManager {
    val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>>
    val activeProfile: StateFlow<String?>
    val activeMatrixMessenger: StateFlow<MatrixMessenger?>

    /**
     * This allows multiple profiles to be used simultaneously. Null means undefined, so the user should be asked if
     * they want to enable this.
     */
    val isMultiProfileEnabled: StateFlow<Boolean?>

    suspend fun closeProfile()

    suspend fun selectProfile(profile: String)

    suspend fun createProfile(
        settings: MatrixMultiMessengerProfileSettingsBase = MatrixMultiMessengerProfileSettingsBase()
    ): String

    suspend fun updateProfile(
        profile: String,
        updater: MutableSettings<MatrixMultiMessengerProfileSettings>.(MatrixMultiMessengerProfileSettings) -> Unit,
    )

    suspend fun setMultiProfileEnabled(enabled: Boolean)

    suspend fun deleteProfile(profile: String)
}

class ProfileManagerImpl(
    private val settingsHolder: MatrixMultiMessengerSettingsHolder,
    private val matrixMessengerFactory: MatrixMessengerFactory,
    private val deleteProfileData: DeleteProfileData,
    private val coroutineScope: CoroutineScope,
) : ProfileManager {
    private companion object {
        val log = Logger("de.connect2x.trixnity.messenger.multi.ProfileManagerImpl")
    }

    override val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>> =
        settingsHolder
            .map { it.base.profiles }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.base.profiles)
    override val activeProfile: StateFlow<String?> =
        settingsHolder
            .map { it.base.activeProfile }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.base.activeProfile)

    override val activeMatrixMessenger: StateFlow<MatrixMessenger?> =
        activeProfile
            .map { profile -> if (profile == null) null else matrixMessengerFactory(profile) }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override val isMultiProfileEnabled: StateFlow<Boolean?> =
        settingsHolder
            .map { it.base.isMultiProfileEnabled }
            .stateIn(coroutineScope, SharingStarted.Eagerly, settingsHolder.value.base.isMultiProfileEnabled)

    override suspend fun closeProfile() {
        coroutineScope
            .launch { // ensure we are NOT running in a CoroutineScope that is any children of the MatrixMessenger
                withContext(NonCancellable) {
                    log.debug { "close current profile ${activeProfile.value}" }
                    activeMatrixMessenger.value?.closeSuspending()
                    settingsHolder.update<MatrixMultiMessengerSettingsBase> { it.copy(activeProfile = null) }
                }
            }
            .join()
    }

    override suspend fun selectProfile(profile: String) {
        coroutineScope
            .launch { // ensure we are NOT running in a CoroutineScope that is any children of the MatrixMessenger
                log.debug { "select profile $profile" }
                closeProfile()
                settingsHolder.update<MatrixMultiMessengerSettingsBase> {
                    if (it.profiles.containsKey(profile)) it.copy(activeProfile = profile) else it
                }
            }
            .join()
    }

    override suspend fun createProfile(settings: MatrixMultiMessengerProfileSettingsBase): String {
        log.debug { "create profile" }
        val profileSettings =
            MatrixMultiMessengerProfileSettings(checkNotNull(SettingsJson.encodeToJsonElement(settings) as? JsonObject))
        var nextId: String? = null
        settingsHolder.update<MatrixMultiMessengerSettingsBase> { oldSettings ->
            val updateNextId =
                generateSequence(0) { it + 1 }
                    .map { it.toString() }
                    .filterNot { oldSettings.profiles.containsKey(it) }
                    .first()
            nextId = updateNextId
            oldSettings.copy(profiles = oldSettings.profiles + (updateNextId to profileSettings))
        }
        return checkNotNull(nextId)
    }

    override suspend fun updateProfile(
        profile: String,
        updater: MutableSettings<MatrixMultiMessengerProfileSettings>.(MatrixMultiMessengerProfileSettings) -> Unit,
    ) =
        settingsHolder.update<MatrixMultiMessengerSettingsBase> {
            log.debug { "update profile settings for $profile" }
            val oldProfiles = it.profiles
            val oldProfileSettings = oldProfiles[profile] ?: return@update it
            val newProfileSettings = MutableSettingsImpl(oldProfileSettings)
            with(newProfileSettings) { updater(oldProfileSettings) }
            it.copy(profiles = oldProfiles + (profile to MatrixMultiMessengerProfileSettings(newProfileSettings)))
        }

    override suspend fun deleteProfile(profile: String) {
        coroutineScope
            .launch { // ensure we are NOT running in a CoroutineScope that is any children of the MatrixMessenger
                log.debug { "delete profile $profile" }
                if (activeProfile.value == profile) closeProfile()
                withContext(NonCancellable) {
                    settingsHolder.update<MatrixMultiMessengerSettingsBase> { oldSettings ->
                        oldSettings.copy(
                            profiles = oldSettings.profiles - profile,
                            activeProfile =
                                if (oldSettings.activeProfile == profile) null else oldSettings.activeProfile,
                        )
                    }
                    deleteProfileData(profile)
                }
            }
            .join()
    }

    override suspend fun setMultiProfileEnabled(enabled: Boolean) {
        if (profiles.value.size > 1) {
            log.warn { "disable multi profiles not possible when more than one profile is created" }
            return
        }
        settingsHolder.update<MatrixMultiMessengerSettingsBase> { it.copy(isMultiProfileEnabled = enabled) }
    }
}

suspend fun <T : SettingsView<MatrixMultiMessengerProfileSettings>> ProfileManager.updateProfile(
    profile: String,
    serializer: KSerializer<T>,
    updater: (T) -> T,
) = updateProfile(profile) { set(updater(it.get(serializer)), serializer) }

suspend inline fun <reified T : SettingsView<MatrixMultiMessengerProfileSettings>> ProfileManager.updateProfile(
    profile: String,
    noinline updater: (T) -> T,
) = updateProfile(profile, serializer(), updater)
