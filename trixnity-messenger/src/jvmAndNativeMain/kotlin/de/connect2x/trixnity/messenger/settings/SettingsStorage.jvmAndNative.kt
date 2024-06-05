package de.connect2x.trixnity.messenger.settings

import de.connect2x.trixnity.messenger.util.IOOrDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

class FileSystemSettingsStorage(
    private val path: Path,
    private val fileSystem: FileSystem,
) : SettingsStorage {
    init {
        path.parent?.let {
            if (!fileSystem.exists(it)) fileSystem.createDirectories(it)
        }
    }

    override suspend fun write(settings: String) {
        withContext(Dispatchers.IOOrDefault) {
            fileSystem.write(path) {
                writeUtf8(settings)
            }
        }
    }

    override suspend fun read(): String? {
        return if (fileSystem.exists(path)) {
            withContext(Dispatchers.IOOrDefault) {
                fileSystem.read(path) {
                    readUtf8()
                }
            }
        } else null
    }
}
