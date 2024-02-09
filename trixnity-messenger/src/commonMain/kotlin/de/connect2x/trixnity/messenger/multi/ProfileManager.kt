package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

interface ProfileManager {
    val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>>
    val activeProfile: StateFlow<String?>
    val activeMatrixMessenger: StateFlow<MatrixMessenger?>

    suspend fun closeProfile()
    suspend fun selectProfile(profile: String)
    suspend fun createProfile(settings: MatrixMultiMessengerProfileSettings)
    suspend fun deleteProfile(profile: String)
}

class ProfileManagerImpl(
    private val settingsHolder: MatrixMultiMessengerSettingsHolder,
    private val matrixMessengerFactory: MatrixMessengerFactory,
    private val coroutineScope: CoroutineScope,
) : ProfileManager {
    override val profiles: StateFlow<Map<String, MatrixMultiMessengerProfileSettings>> =
        settingsHolder.map { it.profiles }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settingsHolder.value.profiles)
    override val activeProfile: StateFlow<String?> =
        settingsHolder.map { it.activeProfile }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settingsHolder.value.activeProfile)

    override val activeMatrixMessenger: StateFlow<MatrixMessenger?> =
        activeProfile.map { profile ->
            if (profile == null) null
            else matrixMessengerFactory(profile)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override suspend fun closeProfile() {
        val test = flowOf("")
        test.buffer(Int.MAX_VALUE).shareIn(coroutineScope, SharingStarted.Eagerly, 1)
        activeMatrixMessenger.value?.stop()
        settingsHolder.update { it.copy(activeProfile = null) }
    }

    override suspend fun selectProfile(profile: String) {
        closeProfile()
        settingsHolder.update { it.copy(activeProfile = profile) }
    }

    override suspend fun createProfile(settings: MatrixMultiMessengerProfileSettings) {
        settingsHolder.update { oldSettings ->
            val nextId = generateSequence(0) { it + 1 }.map { it.toString() }
                .filterNot { oldSettings.profiles.containsKey(it) }
                .first()
            oldSettings.copy(profiles = oldSettings.profiles + (nextId to settings))
        }
    }

    override suspend fun deleteProfile(profile: String) {
        if (activeProfile.value == profile) closeProfile()
        settingsHolder.update { oldSettings ->
            oldSettings.copy(profiles = oldSettings.profiles - profile)
        }
    }
}