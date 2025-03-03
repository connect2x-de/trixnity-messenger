package de.connect2x.trixnity.messenger.multi

fun interface MatrixMultiMessengerWorker {
    suspend operator fun invoke()
}
