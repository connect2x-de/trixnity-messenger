package de.connect2x.trixnity.messenger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SettingsHolder<S : Any> : StateFlow<S> {
    suspend fun update(updater: (S) -> S)
}

interface SettingsStorage<S : Any> {
    suspend fun write(settings: S)
    suspend fun read(): S
}

suspend fun <S : Any> createSettingsHolder(storage: SettingsStorage<S>): SettingsHolder<S> {
    val settings: MutableStateFlow<S> = MutableStateFlow(storage.read())
    return object : SettingsHolder<S>, StateFlow<S> by settings {
        private val updateMutex = Mutex()
        override suspend fun update(updater: (S) -> S) {
            updateMutex.withLock {
                val newSettings = settings.updateAndGet(updater)
                storage.write(newSettings)
            }
        }
    }
}