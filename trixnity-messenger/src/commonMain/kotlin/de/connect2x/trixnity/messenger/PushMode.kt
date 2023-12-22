package de.connect2x.trixnity.messenger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PushMode {
    @SerialName("none")
    NONE,

    @SerialName("polling")
    POLLING,

    @SerialName("push")
    PUSH,
}