package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.core.model.UserId
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SecretId private constructor(val value: String) {
    companion object {
        val secretIdAlphabet = "[a-z0-9.]+".toRegex()
    }

    constructor(
        id: String,
        userId: UserId?,
    ) : this(
        kotlin.run {
            require(id.matches(secretIdAlphabet))
            if (userId == null) id else "$id-${userId.full}"
        }
    )

    val userId: UserId?
        get() = value.substringAfter('-', "").takeIf { it.isNotEmpty() }?.let(::UserId)

    val id: String
        get() = value.substringBefore('-')
}
