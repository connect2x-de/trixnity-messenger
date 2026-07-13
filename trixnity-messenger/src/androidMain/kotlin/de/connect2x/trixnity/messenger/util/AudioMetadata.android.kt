package de.connect2x.trixnity.messenger.util

import android.media.MediaMetadataRetriever
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import kotlin.time.Duration.Companion.milliseconds
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun audioMetadataFactoryModule(): Module = module {
    single<AudioMetadataFactory> { AndroidAudioMetadataFactory(get<ContextGetter>()) }
}

private val log = Logger("de.connect2x.trixnity.messenger.util.AndroidAudioMetadataFactory")

private class AndroidAudioMetadataFactory(private val getContext: ContextGetter) : AudioMetadataFactory {
    override suspend fun invoke(file: FileDescriptor): AudioMetadata? {
        val retriever = MediaMetadataRetriever()
        try {
            when (file) {
                is FileBackedFileDescriptor -> retriever.setDataSource(getContext(), file.filePath.uri)
                else -> throw UnsupportedFileDescriptor()
            }
            return retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                ?.milliseconds
                .let { AudioMetadataImpl(duration = it) }
        } catch (ex: Exception) {
            log.debug(ex) {}
            return null
        } finally {
            retriever.release()
        }
    }
}
