package de.connect2x.messenger.desktop

import de.connect2x.messenger.BuildConfig
import de.connect2x.messenger.messengerConfiguration


fun main(args: Array<String>) = startMessenger(
    appName = BuildConfig.appName,
    version = BuildConfig.version,
    configuration = messengerConfiguration(),
    args = args,
)
