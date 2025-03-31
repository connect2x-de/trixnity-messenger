package de.connect2x.trixnity.messenger.secrets

fun interface GetKey {
    suspend operator fun invoke(size: Int): ByteArray
}
