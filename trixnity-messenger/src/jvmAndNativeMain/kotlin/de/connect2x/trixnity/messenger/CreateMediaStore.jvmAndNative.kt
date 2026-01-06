package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MediaStoreModule
import net.folivo.trixnity.client.media.okio.okio
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMediaStoreModuleModule(): Module = module {
    single<CreateMediaStoreModule> {
        val rootPath = get<RootPath>()
        val filesystem = get<FileSystem>()
        CreateMediaStoreModule { userId ->
            withContext(Dispatchers.IO) {
                MediaStoreModule.okio(
                    basePath = rootPath.forAccountMedia(userId),
                    fileSystem = filesystem
                )
            }
        }
    }
}
