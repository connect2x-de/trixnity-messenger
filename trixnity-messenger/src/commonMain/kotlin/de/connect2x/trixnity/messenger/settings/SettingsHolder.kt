package de.connect2x.trixnity.messenger.settings

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

private val log = KotlinLogging.logger {}

interface SettingsHolder<S : Settings<S>> : StateFlow<S> {
    suspend fun init()
    suspend fun waitForInit()
    suspend fun update(updater: MutableSettings<S>.(S) -> Unit)
}

suspend fun <S : Settings<S>, T : SettingsView<S>> SettingsHolder<S>.update(
    serializer: KSerializer<T>,
    updater: (T) -> T
) = update {
    set(updater(it.get(serializer)), serializer)
}

suspend inline fun <S : Settings<S>, reified T : SettingsView<S>> SettingsHolder<S>.update(
    noinline updater: (T) -> T,
) = update(serializer(), updater)

abstract class SettingsHolderImpl<S : Settings<S>>(
    private val storage: SettingsStorage,
    private val settingsFactory: (Map<String, JsonElement>) -> S,
    private val settings: MutableStateFlow<S?> = MutableStateFlow(null)
) : SettingsHolder<S>, StateFlow<S> {
    private val updateMutex = Mutex()
    override suspend fun update(updater: MutableSettings<S>.(S) -> Unit) =
        updateMutex.withLock {
            log.debug { "update settings" }
            val currentSettings = value
            val newSettings = MutableSettingsImpl(currentSettings)
            with(newSettings) {
                updater(currentSettings)
            }
            settings.value = settingsFactory(newSettings.toMap())
            storage.write(settingsJson.encodeToString<Map<String, JsonElement>>(newSettings))
        }

    private val initMutex = Mutex()
    override suspend fun init() {
        initMutex.withLock {
            if (settings.value == null) {
                log.debug { "init SettingsHolder" }
                val settingsString = storage.read()
                val settingsContent =
                    if (settingsString == null) emptyMap()
                    else settingsJson.decodeFromString<Map<String, JsonElement>>(settingsString)
                settings.value = settingsFactory(settingsContent)
            } else {
                log.debug { "init SettingsHolder skipped (already initialized)" }
            }
        }
    }

    override val replayCache: List<S>
        get() = settings.replayCache.filterNotNull()
    override val value: S
        get() = checkNotNull(settings.value) { "SettingsHolder has not been initialized" }

    override suspend fun collect(collector: FlowCollector<S>): Nothing =
        settings.collect {
            collector.emit(checkNotNull(it) { "SettingsHolder has not been initialized" })
        }

    override suspend fun waitForInit() {
        settings.first { it != null }
    }
}
