package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.CoreMedia.CMTimeGetSeconds

actual fun audioMetadataFactoryModule(): Module = module { single<AudioMetadataFactory> { IosAudioMetadataFactory() } }

private val log = Logger("de.connect2x.trixnity.messenger.util.IosAudioMetadataFactory")

private class IosAudioMetadataFactory : AudioMetadataFactory {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun invoke(file: FileDescriptor): AudioMetadata? {
        return try {
            if (file !is FileKitFileDescriptor) throw UnsupportedFileDescriptor()
            val options =
                file.mimeType?.toString()?.let { mimeType ->
                    mapOf<Any?, Any>(AVURLAssetOverrideMIMETypeKey to mimeType)
                } ?: emptyMap()
            val durationSeconds =
                AVURLAsset.URLAssetWithURL(URL = file.file.nsUrl, options = options).duration.let(::CMTimeGetSeconds)
            return if (durationSeconds.isFinite() && durationSeconds > 0.0) {
                AudioMetadataImpl(duration = (durationSeconds * 1000).roundToLong().milliseconds)
            } else {
                null
            }
        } catch (ex: Exception) {
            log.debug(ex) {}
            null
        }
    }
}
