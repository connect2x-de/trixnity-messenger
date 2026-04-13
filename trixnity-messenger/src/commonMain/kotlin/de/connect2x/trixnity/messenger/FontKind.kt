package de.connect2x.trixnity.messenger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FontKind {

    @SerialName("system")
    SYSTEM,

    @SerialName("bundled")
    BUNDLED,

}
