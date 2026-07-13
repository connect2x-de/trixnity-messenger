package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import js.promise.Promise
import js.promise.await
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import org.koin.core.module.Module
import org.koin.dsl.module
import web.dom.document
import web.events.EventHandler
import web.events.EventType
import web.events.addEventListener
import web.html.HtmlTagName
import web.url.URL

actual fun audioMetadataFactoryModule(): Module = module { single<AudioMetadataFactory> { WebAudioMetadataFactory() } }

private val log = Logger("de.connect2x.trixnity.messenger.util.WebAudioMetadataFactory")

private class WebAudioMetadataFactory : AudioMetadataFactory {
    override suspend fun invoke(file: FileDescriptor): AudioMetadata? {
        if (file !is FileBackedFileDescriptor) throw UnsupportedFileDescriptor()
        var objectUrl: String? = null
        return try {
            objectUrl = file.filePath.url
            val audio = document.createElement(HtmlTagName.audio)
            val metadataLoaded =
                Promise(
                    executor = { resolve, _ ->
                        audio.addEventListener(
                            type = EventType("loadedmetadata"),
                            handler = EventHandler { resolve(null) },
                        )
                        audio.addEventListener(type = EventType("error"), handler = EventHandler { resolve(null) })
                    }
                )

            audio.src = objectUrl
            audio.load()
            metadataLoaded.await()

            val durationSeconds = audio.duration
            if (durationSeconds.isFinite() && durationSeconds > 0.0) {
                AudioMetadataImpl(duration = (durationSeconds * 1000).roundToLong().milliseconds)
            } else {
                null
            }
        } catch (ex: Exception) {
            log.debug(ex) {}
            null
        } finally {
            objectUrl?.let { URL.revokeObjectURL(it) }
        }
    }
}
