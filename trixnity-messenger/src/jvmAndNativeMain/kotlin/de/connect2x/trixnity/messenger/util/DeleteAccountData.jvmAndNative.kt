package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteAccountDataModule(): Module = module {
    single<DeleteAccountData> {
        val paths = get<Paths>()
        DeleteAccountData { userId ->
            paths.fileSystem.deleteRecursively(paths.rootPath.resolve(userId.full), mustExist = false)
        }
    }
}