package de.connect2x.trixnity.messenger

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformCreateMatrixMessengerSettingsHolderModule(): Module = module {
    single<CreateMatrixMessengerSettingsHolder> {
        CreateMatrixMessengerSettingsHolder {
            MatrixMessengerSettingsHolderImpl(
                createLocalStorageSettingsHolder("settings.json") { MatrixMessengerSettings() }
            )
        }
    }
}