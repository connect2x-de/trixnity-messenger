package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.IOOrDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

suspend inline fun <reified S : Any> createFilesystemSettingsHolder(
    path: Path,
    fileSystem: FileSystem,
    crossinline initialSettings: () -> S,
): SettingsHolder<S> {
    path.parent?.let {
        if (!fileSystem.exists(it)) fileSystem.createDirectories(it)
    }
    return createSettingsHolder(object : SettingsStorage<S> {
        override suspend fun write(settings: S) {
            val json = Json.encodeToString(settings)
            withContext(Dispatchers.IOOrDefault) {
                fileSystem.write(path) {
                    writeUtf8(json)
                }
            }
        }

        override suspend fun read(): S {
            return if (fileSystem.exists(path)) {
                val json = withContext(Dispatchers.IOOrDefault) {
                    fileSystem.read(path) {
                        readUtf8()
                    }
                }
                Json.decodeFromString<S>(json)
            } else initialSettings()
        }
    })
}