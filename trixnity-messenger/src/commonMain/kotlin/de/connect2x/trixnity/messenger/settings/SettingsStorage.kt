package de.connect2x.trixnity.messenger.settings

interface SettingsStorage {
    suspend fun write(settings: String)
    suspend fun read(): String?
}
