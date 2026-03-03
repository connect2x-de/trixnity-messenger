package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDeleteAccountDataModule(): Module = module {
    single<DeleteAccountData> {
        val rootPath = get<RootPath>()
        DeleteAccountData { userId ->
            val accountPath = rootPath.forAccount(userId)
            accountPath.deleteVirtualFileSystemData()
        }
    }
}
