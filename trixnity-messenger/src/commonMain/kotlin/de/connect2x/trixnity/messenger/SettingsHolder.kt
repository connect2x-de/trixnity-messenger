package de.connect2x.trixnity.messenger

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

interface SettingsHolder<S : Any> : StateFlow<S> {
    suspend fun init()
    suspend fun update(updater: (S) -> S)
}

interface SettingsStorage<S : Any> {
    suspend fun write(settings: S)
    suspend fun read(): S
}

fun <S : Any> createSettingsHolder(storage: SettingsStorage<S>): SettingsHolder<S> {
    return object : SettingsHolder<S> {
        val settings: MutableStateFlow<S?> = MutableStateFlow(null)
        private val updateMutex = Mutex()
        override suspend fun update(updater: (S) -> S) {
            updateMutex.withLock {
                val currentSettings = value
                val newSettings = updater(currentSettings)
                settings.value = newSettings
                storage.write(newSettings)
            }
        }

        override suspend fun init() {
            settings.value = storage.read()
        }

        override val replayCache: List<S>
            get() = settings.replayCache.filterNotNull()
        override val value: S
            get() = checkNotNull(settings.value) { "SettingsHolder has not been initialized" }

        override suspend fun collect(collector: FlowCollector<S>): Nothing =
            settings.collect {
                collector.emit(checkNotNull(it) { "SettingsHolder has not been initialized" })
            }
    }
}

@PublishedApi
internal val settingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
