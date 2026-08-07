package de.connect2x.trixnity.messenger.internal.utils

import de.connect2x.trixnity.messenger.settings.SettingsStorage
import kotlin.concurrent.atomics.AtomicReference

internal class InMemorySettingsStorage : SettingsStorage {
    private val settings = AtomicReference<String?>(null)

    override suspend fun read(): String? {
        return this.settings.load()
    }

    override suspend fun write(settings: String) {
        this.settings.store(settings)
    }
}
