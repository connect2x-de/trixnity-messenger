package de.connect2x.trixnity.messenger

import kotlinx.serialization.Serializable

@Serializable
sealed interface LoadStoreException {
    @Serializable
    data class StoreAccessException(override val message: String? = null) : LoadStoreException, RuntimeException(message)

    @Serializable
    data class StoreLockedException(override val message: String? = null) : LoadStoreException, RuntimeException(message)
}