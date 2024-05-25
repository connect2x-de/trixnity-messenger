package de.connect2x.trixnity.messenger.settings

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

interface SettingsHolder<S : Settings<S>> : StateFlow<S> {
    suspend fun init()
    suspend fun update(updater: MutableSettings<S>.(S) -> Unit)
}

suspend fun <S : Settings<S>, T : SettingsView<S>> SettingsHolder<S>.updateView(
    serializer: KSerializer<T>,
    updater: (T) -> T
) = update {
    set(updater(it.get(serializer)), serializer)
}

suspend inline fun <S : Settings<S>, reified T : SettingsView<S>> SettingsHolder<S>.updateView(
    noinline updater: (T) -> T,
) = updateView(serializer(), updater)

abstract class SettingsHolderImpl<S : Settings<S>>(
    private val storage: SettingsStorage,
    private val settingsFactory: (Map<String, JsonElement>) -> S,
    private val settings: MutableStateFlow<S?> = MutableStateFlow(null)
) : SettingsHolder<S>, StateFlow<S> {
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
