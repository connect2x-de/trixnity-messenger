package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.Paths
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMessengerSettingsHolderModule(): Module = module {
    single<MatrixMessengerSettingsHolder> {
        val paths = get<Paths>()
        MatrixMessengerSettingsHolderImpl(
            createFilesystemSettingsHolder(
                path = paths.rootPath.resolve("settings.json"),
                fileSystem = paths.fileSystem,
            ) { MatrixMessengerSettings() }
        )
    }.bind<SettingsHolder<*>>()
}