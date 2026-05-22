package de.connect2x.trixnity.messenger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    @SerialName("default") DEFAULT,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}
