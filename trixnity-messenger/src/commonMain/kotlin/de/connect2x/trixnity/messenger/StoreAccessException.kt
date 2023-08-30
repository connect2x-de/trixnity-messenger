package de.connect2x.trixnity.messenger

sealed interface LoadStoreException {
    class StoreAccessException(cause: Throwable) : LoadStoreException, RuntimeException(cause)
    class StoreLockedException(message: String? = null) : LoadStoreException, RuntimeException(message)
}