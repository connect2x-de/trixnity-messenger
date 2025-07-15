package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import io.github.oshai.kotlinlogging.KotlinLogging
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

private val log = KotlinLogging.logger { }

class NetworkMonitor : IsNetworkAvailable {
    private val monitor: nw_path_monitor_t = nw_path_monitor_create()
    private val queue = dispatch_get_main_queue()
    private var connected: Boolean = false

    init {
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
        log.info { "Starting network monitor" }
        nw_path_monitor_set_update_handler(monitor) { path ->
            connected = nw_path_get_status(path) == nw_path_status_satisfied
            log.info { "Network is now ${if (connected) "connected" else "disconnected"}" }
        }
    }

    override fun invoke(): Boolean = connected
}
