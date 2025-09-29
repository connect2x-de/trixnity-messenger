package de.connect2x.trixnity.messenger

fun interface Worker {
    suspend fun doWork()
}
