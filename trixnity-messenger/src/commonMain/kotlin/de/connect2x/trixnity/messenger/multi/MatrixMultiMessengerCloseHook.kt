package de.connect2x.trixnity.messenger.multi

fun interface MatrixMultiMessengerCloseHook {
    operator fun invoke(multiMessenger: MatrixMultiMessenger)
}
