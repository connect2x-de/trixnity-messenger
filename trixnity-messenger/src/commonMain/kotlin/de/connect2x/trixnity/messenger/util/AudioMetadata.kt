package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlin.time.Duration
import org.koin.core.module.Module

fun interface AudioMetadataFactory {
    suspend operator fun invoke(file: FileDescriptor): AudioMetadata?
}

interface AudioMetadata {
    suspend fun duration(): Duration?
}

@TrixnityMessengerPrivateApi
data class AudioMetadataImpl(val duration: Duration? = null) : AudioMetadata {
    override suspend fun duration(): Duration? = duration
}

expect fun audioMetadataFactoryModule(): Module

class UnsupportedFileDescriptor(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
