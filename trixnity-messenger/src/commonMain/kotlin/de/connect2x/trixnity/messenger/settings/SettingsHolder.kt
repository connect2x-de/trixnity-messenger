package de.connect2x.trixnity.messenger.settings

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

interface SettingsHolder<S : Settings<S>> : StateFlow<S> {
    suspend fun init()
    suspend fun update(updater: MutableSettings<S>.(S) -> Unit)
}

abstract class SettingsHolderImpl<S : Settings<S>>(
    private val storage: SettingsStorage,
    private val settingsFactory: (Map<String, JsonElement>) -> S,
) : SettingsHolder<S>, StateFlow<S> {
    val settings: MutableStateFlow<S?> = MutableStateFlow(null)
    private val updateMutex = Mutex()
    override suspend fun update(updater: MutableSettings<S>.(S) -> Unit) =
        updateMutex.withLock {
            val currentSettings = value
            val newSettings = MutableSettingsImpl(currentSettings)
            with(newSettings) {
                updater(currentSettings)
            }
            settings.value = settingsFactory(newSettings.toMap())
            storage.write(settingsJson.encodeToString<Map<String, JsonElement>>(newSettings))
        }

    override suspend fun init() {
        val settingsString = storage.read()
        val settingsContent =
            if (settingsString == null) emptyMap()
            else settingsJson.decodeFromString<Map<String, JsonElement>>(settingsString)
        settings.value = settingsFactory(settingsContent)
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
