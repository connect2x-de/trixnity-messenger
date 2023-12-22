package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.Paths
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMatrixMessengerSettingsHolderModule(): Module = module {
    single<CreateMatrixMessengerSettingsHolder> {
        val paths = get<Paths>()
        CreateMatrixMessengerSettingsHolder {
            MatrixMessengerSettingsHolderImpl(
                createFilesystemSettingsHolder(
                    path = paths.rootPath.resolve("settings.json"),
                    fileSystem = paths.fileSystem,
                ) { MatrixMessengerSettings() }
            )
        }
    }
}