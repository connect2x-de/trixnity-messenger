package de.connect2x.messenger.android

import de.connect2x.messenger.messengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

class MessengerConfigurationProvider: ConfigurationProvider {
    override fun configuration(): MatrixMultiMessengerConfiguration.() -> Unit {
        log.debug { "loading messenger configuration" }
        return messengerConfiguration()
    }
}
