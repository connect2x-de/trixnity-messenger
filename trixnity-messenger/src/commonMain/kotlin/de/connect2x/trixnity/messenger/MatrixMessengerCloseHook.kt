package de.connect2x.trixnity.messenger

fun interface MatrixMessengerCloseHook {
    operator fun invoke(messenger: MatrixMessenger)
}
