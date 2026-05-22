package de.connect2x.trixnity.messenger.settings

import web.storage.localStorage

class LocalStorageSettingsStorage(private val name: String) : SettingsStorage {
    override suspend fun write(settings: String) = localStorage.setItem(name, settings)

    override suspend fun read(): String? = localStorage.getItem(name)
}
