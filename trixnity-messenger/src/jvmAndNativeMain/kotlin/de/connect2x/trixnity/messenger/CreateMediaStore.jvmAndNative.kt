package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.Paths
import de.connect2x.trixnity.messenger.util.asFilesystemSafeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMediaStoreModule(): Module = module {
    single<CreateMediaStore> {
        val paths = get<Paths>()
        CreateMediaStore { userId ->
            withContext(Dispatchers.IO) {
                OkioMediaStore(
                    paths.rootPath.resolve(userId.asFilesystemSafeString()).resolve("media")
                )
            }
        }
    }
}