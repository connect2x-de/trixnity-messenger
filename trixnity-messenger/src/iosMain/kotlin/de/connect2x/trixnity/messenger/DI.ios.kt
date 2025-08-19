package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<IsNetworkAvailable> { NetworkMonitor() }
}
