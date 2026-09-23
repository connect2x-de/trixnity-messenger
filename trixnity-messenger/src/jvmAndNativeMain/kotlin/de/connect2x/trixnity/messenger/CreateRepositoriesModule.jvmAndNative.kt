package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        RoomCreateRepositoriesModule(
            rootPath = get<RootPath>(),
            fileSystem = getOrNull<FileSystem>(),
            matrixMessengerConfiguration = get<MatrixMessengerConfiguration>(),
            scope = this,
        )
    }
}
