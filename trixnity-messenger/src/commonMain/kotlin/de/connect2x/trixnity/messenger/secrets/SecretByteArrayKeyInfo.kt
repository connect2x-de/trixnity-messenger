package de.connect2x.trixnity.messenger.secrets

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class SecretByteArrayKeyInfo(val dependsOn: String? = null, val extra: JsonObject? = null)
