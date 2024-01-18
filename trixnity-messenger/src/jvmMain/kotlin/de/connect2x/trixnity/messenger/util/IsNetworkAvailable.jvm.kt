package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module
import java.net.NetworkInterface

actual fun platformIsNetworkAvailableModule(): Module = module {
    single<IsNetworkAvailable> {
        IsNetworkAvailable {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            interfaces.any { it.isUp && !it.isLoopback }
        }
    }
}