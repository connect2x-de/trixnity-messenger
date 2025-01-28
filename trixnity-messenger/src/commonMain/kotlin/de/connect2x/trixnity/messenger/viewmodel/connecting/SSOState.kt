package de.connect2x.trixnity.messenger.viewmodel.connecting

import kotlinx.serialization.Serializable

@Serializable
data class SSOState(
    val state: String,
    val serverUrl: String,
    val providerId: String?,
    val providerName: String?,
)
