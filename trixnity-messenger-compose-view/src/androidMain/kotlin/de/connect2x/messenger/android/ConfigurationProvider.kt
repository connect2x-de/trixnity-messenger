package de.connect2x.messenger.android

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration

interface ConfigurationProvider {
    fun configuration(): MatrixMultiMessengerConfiguration.() -> Unit
}
