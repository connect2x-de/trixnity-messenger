package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.settings.JsonDelegateSerializer
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.settings.SettingsHolderImpl
import de.connect2x.trixnity.messenger.settings.SettingsImpl
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.get
import de.connect2x.trixnity.messenger.settings.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.koin.core.module.Module

@Serializable
data class MatrixMultiMessengerSettingsBase(
    val profiles: Map<String, MatrixMultiMessengerProfileSettings> = mapOf(),
    val activeProfile: String? = null,
    val forgetActiveProfileOnStart: Boolean = false,
    val isMultiProfileEnabled: Boolean? = null,
) : SettingsView<MatrixMultiMessengerSettings>

@Serializable
data class MatrixMultiMessengerProfileSettingsBase(
    val displayName: String? = null,
) : SettingsView<MatrixMultiMessengerProfileSettings>

data class MatrixMultiMessengerSettings(
    private val delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMultiMessengerSettings>(delegate) {
    val base by lazy {
        get<MatrixMultiMessengerSettings, MatrixMultiMessengerSettingsBase>()
    }
}

@Serializable(MatrixMultiMessengerProfileSettingsSerializer::class)
data class MatrixMultiMessengerProfileSettings(
    private val delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMultiMessengerProfileSettings>(delegate) {
    val base by lazy { get<MatrixMultiMessengerProfileSettings, MatrixMultiMessengerProfileSettingsBase>() }
}

internal object MatrixMultiMessengerProfileSettingsSerializer :
    JsonDelegateSerializer<MatrixMultiMessengerProfileSettings>(
        "MatrixMultiMessengerProfileSettingsSerializer", ::MatrixMultiMessengerProfileSettings
    )

interface MatrixMultiMessengerSettingsHolder : SettingsHolder<MatrixMultiMessengerSettings>

class MatrixMultiMessengerSettingsHolderImpl(
    storage: SettingsStorage,
    settings: MutableStateFlow<MatrixMultiMessengerSettings?> = MutableStateFlow(null)
) : SettingsHolderImpl<MatrixMultiMessengerSettings>(storage, ::MatrixMultiMessengerSettings, settings),
    MatrixMultiMessengerSettingsHolder

suspend inline fun <reified T : SettingsView<MatrixMultiMessengerSettings>> MatrixMultiMessengerSettingsHolder.update(
    noinline updater: (T) -> T,
) = update(serializer(), updater)

expect fun platformMatrixMultiMessengerSettingsHolderModule(): Module
