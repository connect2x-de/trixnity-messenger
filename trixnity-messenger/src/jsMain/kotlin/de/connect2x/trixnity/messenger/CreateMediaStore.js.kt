package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.MediaStoreModule
import net.folivo.trixnity.client.media.indexeddb.indexedDB
import net.folivo.trixnity.client.media.opfs.opfs
import org.koin.core.module.Module
import org.koin.dsl.module
import web.fs.FileSystemGetDirectoryOptions
import web.fs.getDirectoryHandle
import web.navigator.navigator
import web.storage.getDirectory

private val log = KotlinLogging.logger {}

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
