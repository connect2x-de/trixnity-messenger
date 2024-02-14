package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteAccountDataModule(): Module = module {
    single<DeleteAccountData> {
        val fileSystem = get<FileSystem>()
        val rootPath = get<RootPath>()
        DeleteAccountData { userId ->
            fileSystem.deleteRecursively(
                rootPath.forAccount(userId),
                mustExist = false
            )
        }
    }
}