package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.SettingsHolder
import de.connect2x.trixnity.messenger.createLocalStorageSettingsHolder
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMultiMessengerSettingsHolderModule(): Module = module {
    single<MatrixMultiMessengerSettingsHolder> {
        val rootPath = get<RootPath>().path
        MatrixMultiMessengerSettingsHolderImpl(
            createLocalStorageSettingsHolder(
                name = rootPath.resolve("settings.json").toString()
            ) { MatrixMultiMessengerSettings() })
    }.bind<SettingsHolder<*>>()
}
