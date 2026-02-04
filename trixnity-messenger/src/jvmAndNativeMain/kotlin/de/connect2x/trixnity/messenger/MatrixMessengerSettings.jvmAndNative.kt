package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.FileSystemSettingsStorage
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMessengerSettingsHolderModule(): Module = module {
    single<MatrixMessengerSettingsHolder> {
        val rootPath = get<RootPath>().path
        val configuration =
            getOrNull<MatrixMessengerConfiguration>()?.let { MatrixMessengerSettingsBase.withConfigDefaults(it) }
        MatrixMessengerSettingsHolderImpl(
            FileSystemSettingsStorage(
                path = rootPath.resolve("settings.json"),
                fileSystem = get(),
            ),
            defaultSettings = configuration
        )
    }.bind<SettingsHolder<*>>()
}
