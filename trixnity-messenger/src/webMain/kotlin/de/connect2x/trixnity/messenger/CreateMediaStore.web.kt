package de.connect2x.trixnity.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.client.MediaStoreModule
import de.connect2x.trixnity.client.media.indexeddb.indexedDB
import de.connect2x.trixnity.client.media.opfs.opfs
import org.koin.core.module.Module
import org.koin.dsl.module
import web.fs.FileSystemGetDirectoryOptions
import web.fs.getDirectoryHandle
import web.navigator.navigator
import web.storage.getDirectory

private val log: Logger = Logger("de.connect2x.trixnity.messenger.CreateMediaStoreKt")

actual fun platformCreateMediaStoreModuleModule(): Module = module {
    single<CreateMediaStoreModule> {
        val rootPath = get<RootPath>()
        CreateMediaStoreModule { userId ->
            val basePath = rootPath.forAccountMedia(userId)
            try {
                var opfsDirectory = navigator.storage.getDirectory()
                for (segment in basePath.segments) {
                    opfsDirectory =
                        opfsDirectory.getDirectoryHandle(segment, FileSystemGetDirectoryOptions(create = true))
                }
                MediaStoreModule.opfs(opfsDirectory)
            } catch (error: Throwable) {
                log.warn(error) { "failed to use OPFS as MediaStore. Falling back to Indexeddb" }
                MediaStoreModule.indexedDB(basePath.toString())
            }
        }
    }
}
