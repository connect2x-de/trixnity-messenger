package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> { IndexedDBCreateRepositoriesModule(rootPath = get<RootPath>()) }
}
