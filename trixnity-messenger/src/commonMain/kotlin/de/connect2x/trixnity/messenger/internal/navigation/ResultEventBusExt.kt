package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@TrixnityMessengerPrivateApi
inline fun <reified T> ResultEventBus.getResult(): Flow<T> {
    return getResult(T::class).map { it as T }
}

@TrixnityMessengerPrivateApi
inline fun <reified T> ResultEventBus.sendResult(result: T) {
    sendResult(T::class, result)
}

@TrixnityMessengerPrivateApi
inline fun <reified T> ResultEventBus.removeResult() {
    removeResult(T::class)
}
