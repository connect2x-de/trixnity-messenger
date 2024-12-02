package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import io.github.oshai.kotlinlogging.KotlinLogging
import js.objects.jso
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore
import net.folivo.trixnity.client.media.opfs.OpfsMediaStore
import org.koin.core.module.Module
import org.koin.dsl.module
import web.navigator.navigator

private val log = KotlinLogging.logger {}

actual fun platformCreateMediaStoreModule(): Module = module {
    single<CreateMediaStore> {
        val rootPath = get<RootPath>()
        CreateMediaStore { userId ->
            val basePath = rootPath.forAccountMedia(userId)
            try {
                var opfsDirectory = navigator.storage.getDirectory()
                for (segment in basePath.segments) {
                    opfsDirectory = opfsDirectory.getDirectoryHandle(segment, jso { create = true })
                }
                OpfsMediaStore(opfsDirectory)
            } catch (exception: Throwable) {
                log.error(exception) { "failed to use OPFS as MediaStore. Falling back to Indexeddb" }
                IndexedDBMediaStore(basePath.toString())
            }
        }
    }
}
