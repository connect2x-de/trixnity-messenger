package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.StoragePrefix
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMessengerSettingsHolderModule(): Module = module {
    single<MatrixMessengerSettingsHolder> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        MatrixMessengerSettingsHolderImpl(
            createLocalStorageSettingsHolder("${storagePrefix}settings.json") { MatrixMessengerSettings() })
    }.bind<SettingsHolder<*>>()
}