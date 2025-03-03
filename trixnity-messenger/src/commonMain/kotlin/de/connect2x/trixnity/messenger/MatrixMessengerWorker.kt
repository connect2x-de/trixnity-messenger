package de.connect2x.trixnity.messenger

fun interface MatrixMessengerWorker {
    suspend operator fun invoke()
}
