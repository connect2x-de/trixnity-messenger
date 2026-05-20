package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.settings.FileSystemSettingsStorage
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMultiMessengerSettingsHolderModule(): Module = module {
    single<MatrixMultiMessengerSettingsHolder> {
            val rootPath = get<RootPath>().path
            MatrixMultiMessengerSettingsHolderImpl(
                FileSystemSettingsStorage(path = rootPath.resolve("settings.json"), fileSystem = get())
            )
        }
        .bind<SettingsHolder<*>>()
}
