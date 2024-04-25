package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.RootPath
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteProfileDataModule(): Module = module {
    single<DeleteProfileData> {
        val fileSystem = get<FileSystem>()
        val rootPath = get<RootPath>()
        DeleteProfileData { profile, userId ->
            fileSystem.deleteRecursively(
                rootPath.path.resolve(profile),
                mustExist = false
            )
        }
    }
}
