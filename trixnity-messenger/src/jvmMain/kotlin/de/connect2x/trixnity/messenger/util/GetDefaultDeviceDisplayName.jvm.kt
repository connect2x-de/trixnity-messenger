package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetDefaultDisplayNameModule(): Module = module {
    single<GetDefaultDeviceDisplayName> {
        val config = get<MatrixMessengerBaseConfiguration>()
        GetDefaultDeviceDisplayName { "${config.appName} (${getOs().value})" }
    }
}
