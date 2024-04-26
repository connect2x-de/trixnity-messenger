package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMessengerSettingsHolderModule(): Module = module {
    single<MatrixMessengerSettingsHolder> {
        val rootPath = get<RootPath>().path
        MatrixMessengerSettingsHolderImpl(
            createLocalStorageSettingsHolder(
                name = rootPath.resolve("settings.json").toString()
            ) { MatrixMessengerSettings() })
    }.bind<SettingsHolder<*>>()
}
