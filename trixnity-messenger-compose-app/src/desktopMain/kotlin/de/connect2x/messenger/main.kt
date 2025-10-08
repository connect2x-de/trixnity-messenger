package de.connect2x.messenger

import de.connect2x.messenger.compose.view.startMultiMessenger

fun main(args: Array<String>) = startMultiMessenger(
    configuration = {
        configure()
    },
    args = args,
)
