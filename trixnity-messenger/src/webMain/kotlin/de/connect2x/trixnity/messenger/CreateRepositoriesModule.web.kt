package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val matrixMessengerConfiguration = get<MatrixMessengerConfiguration>()

        if (matrixMessengerConfiguration.features.enableRoom3ForNewAccountsOnWeb) {
            AdaptiveCreateRepositoriesModule(
                rootPath = get<RootPath>(),
                fileSystem = getOrNull<FileSystem>(),
                matrixMessengerConfiguration = matrixMessengerConfiguration,
                scope = this@single,
            )
        } else {
            IndexedDBCreateRepositoriesModule(rootPath = get<RootPath>())
        }
    }
}
