package de.connect2x.trixnity.messenger

import java.net.NetworkInterface


actual fun isNetworkAvailable(): Boolean {
    val interfaces = NetworkInterface.getNetworkInterfaces().toList()
    return interfaces.any { it.isUp && !it.isLoopback }
}