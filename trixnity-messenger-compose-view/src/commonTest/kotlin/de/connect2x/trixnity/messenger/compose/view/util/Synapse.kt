package de.connect2x.trixnity.messenger.compose.view.util

import de.connect2x.trixnity.core.model.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SynapseLoginWithPassword(
    @SerialName("type") val type: String,
    @SerialName("user") val username: String,
    @SerialName("password") val password: String,
)

@Serializable
data class SynapseLoginResponse(
    @SerialName("user_id") val userId: UserId,
    @SerialName("access_token") val accessToken: String,
)
