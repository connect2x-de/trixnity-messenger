package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMediaStoreModule(): Module = module {
    single<CreateMediaStore> {
        val rootPath = get<RootPath>()
        val filesystem = get<FileSystem>()
        CreateMediaStore { userId ->
            withContext(Dispatchers.IO) {
                OkioMediaStore(
                    basePath = rootPath.forAccount(userId).resolve("media"),
                    fileSystem = filesystem
                )
            }
        }
    }
}