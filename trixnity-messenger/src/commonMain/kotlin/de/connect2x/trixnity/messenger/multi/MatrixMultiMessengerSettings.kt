package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.settings.JsonDelegateSerializer
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.settings.SettingsHolderImpl
import de.connect2x.trixnity.messenger.settings.SettingsImpl
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.koin.core.module.Module

@Serializable
data class MatrixMultiMessengerSettingsBase(
    val profiles: Map<String, MatrixMultiMessengerProfileSettings> = mapOf(),
    val activeProfile: String? = null,
    val forgetActiveProfileOnStart: Boolean = false,
) : SettingsView<MatrixMultiMessengerSettings>

@Serializable
data class MatrixMultiMessengerProfileSettingsBase(
    val displayName: String? = null,
) : SettingsView<MatrixMultiMessengerProfileSettings>

class MatrixMultiMessengerSettings(
    delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMultiMessengerSettings>(delegate) {
    val base by lazy { get<MatrixMultiMessengerSettings, MatrixMultiMessengerSettingsBase>() }
}

@Serializable(MatrixMultiMessengerProfileSettingsSerializer::class)
class MatrixMultiMessengerProfileSettings(
    delegate: Map<String, JsonElement>
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
) : SettingsHolderImpl<MatrixMultiMessengerSettings>(storage, ::MatrixMultiMessengerSettings),
    MatrixMultiMessengerSettingsHolder

expect fun platformMatrixMultiMessengerSettingsHolderModule(): Module
