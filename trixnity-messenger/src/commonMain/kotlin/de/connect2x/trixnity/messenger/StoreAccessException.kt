package de.connect2x.trixnity.messenger

class StoreAccessException(cause: Throwable) : RuntimeException(cause)

class StoreLockedException(message: String? = null) : RuntimeException(message)