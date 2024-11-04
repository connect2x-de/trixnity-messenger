package de.connect2x.messenger.web

import de.connect2x.messenger.compose.view.startMessenger
import de.connect2x.messenger.messengerConfiguration

suspend fun main() = startMessenger(
    configuration = messengerConfiguration(),
)
