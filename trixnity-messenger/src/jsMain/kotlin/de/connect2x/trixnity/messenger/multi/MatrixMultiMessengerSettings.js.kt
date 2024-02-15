package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.SettingsHolder
import de.connect2x.trixnity.messenger.createLocalStorageSettingsHolder
import de.connect2x.trixnity.messenger.util.StoragePrefix
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMultiMessengerSettingsHolderModule(): Module = module {
    single<MatrixMultiMessengerSettingsHolder> {
        val storagePrefix = get<StoragePrefix>().storagePrefix
        MatrixMultiMessengerSettingsHolderImpl(
            createLocalStorageSettingsHolder("${storagePrefix}settings.json") { MatrixMultiMessengerSettings() })
    }.bind<SettingsHolder<*>>()
}