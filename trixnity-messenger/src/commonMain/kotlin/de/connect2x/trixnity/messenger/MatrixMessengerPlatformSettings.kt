package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.JsonDelegateSerializer
import de.connect2x.trixnity.messenger.settings.SettingsImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable(with = MatrixMessengerPlatformSettingsSerializer::class)
class MatrixMessengerPlatformSettings(
    delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMessengerPlatformSettings>(delegate)

internal object MatrixMessengerPlatformSettingsSerializer :
    JsonDelegateSerializer<MatrixMessengerPlatformSettings>(
        "MatrixMessengerPlatformSettingsSerializer", ::MatrixMessengerPlatformSettings
    )

@Serializable(with = MatrixMessengerAccountPlatformSettingsSerializer::class)
class MatrixMessengerAccountPlatformSettings(
    delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMessengerAccountPlatformSettings>(delegate)

internal object MatrixMessengerAccountPlatformSettingsSerializer :
    JsonDelegateSerializer<MatrixMessengerAccountPlatformSettings>(
        "MatrixMessengerAccountPlatformSettingsSerializer", ::MatrixMessengerAccountPlatformSettings
    )
