package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.deleteVirtualFileSystemData
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteProfileDataModule(): Module = module {
    single<DeleteProfileData> {
        val rootPath = get<RootPath>().path
        DeleteProfileData { profile ->
            val profilePath = rootPath.resolve(profile)
            profilePath.deleteVirtualFileSystemData()
        }
    }
}
