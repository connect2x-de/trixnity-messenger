package de.connect2x.trixnity.messenger

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMatrixMessengerSettingsHolderModule(): Module = module {
    single<MatrixMessengerSettingsHolder> {
        MatrixMessengerSettingsHolderImpl(
            createLocalStorageSettingsHolder("settings.json") { MatrixMessengerSettings() })
    }.bind<SettingsHolder<*>>()
}