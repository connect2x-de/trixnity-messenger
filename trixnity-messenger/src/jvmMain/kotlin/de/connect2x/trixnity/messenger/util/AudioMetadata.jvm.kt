package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import javax.sound.sampled.AudioSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.metadata.XMPDM
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.xml.sax.helpers.DefaultHandler

actual fun audioMetadataFactoryModule(): Module = module { single<AudioMetadataFactory> { JvmAudioMetadataFactory() } }

private val log = Logger("de.connect2x.trixnity.messenger.util.JvmAudioMetadataFactory")

private class JvmAudioMetadataFactory : AudioMetadataFactory {
    private val parser = AutoDetectParser()

    override suspend fun invoke(file: FileDescriptor): AudioMetadata? {
        if (file !is FileBackedFileDescriptor) throw UnsupportedFileDescriptor()
        return getTikaDuration(file) ?: getJavaSoundDuration(file)
    }

    private fun getTikaDuration(file: FileBackedFileDescriptor): AudioMetadata? =
        runCatching {
                val metadata = Metadata()
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.fileName)
                val stream = TikaInputStream.get(file.filePath.path.toNioPath(), metadata)
                stream.use { input -> parser.parse(input, DefaultHandler(), metadata, ParseContext()) }
                AudioMetadataImpl(duration = metadata.get(XMPDM.DURATION)?.toPositiveSecondsDuration())
            }
            .getOrElse {
                log.warn(it) { "tika could not extract audio duration from ${file.fileName}" }
                null
            }

    private fun getJavaSoundDuration(file: FileBackedFileDescriptor): AudioMetadata? =
        runCatching {
                val audioFileFormat = AudioSystem.getAudioFileFormat(file.filePath.path.toNioPath().toFile())
                val durationMicros = audioFileFormat.properties()["duration"] as? Long
                if (durationMicros != null) {
                    return@runCatching AudioMetadataImpl(
                        duration = (durationMicros / 1_000_000.0).toPositiveSecondsDuration()
                    )
                }
                val frameLength = audioFileFormat.frameLength
                val frameRate = audioFileFormat.format.frameRate
                if (frameLength <= 0 || frameRate <= 0) null
                else {
                    val duration = (frameLength / frameRate.toDouble()).toPositiveSecondsDuration()
                    AudioMetadataImpl(duration = duration)
                }
            }
            .getOrElse {
                log.warn(it) { "javax could not extract audio duration from ${file.fileName}" }
                null
            }
}

private fun String.toPositiveSecondsDuration(): Duration? = toDoubleOrNull()?.toPositiveSecondsDuration()

private fun Double.toPositiveSecondsDuration(): Duration? = takeIf { it.isFinite() && it > 0.0 }?.seconds
