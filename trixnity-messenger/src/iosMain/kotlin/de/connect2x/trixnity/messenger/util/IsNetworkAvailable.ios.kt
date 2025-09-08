package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformIsNetworkAvailableModule(): Module = module {
    single<IsNetworkAvailable> { NetworkMonitor() }
}
