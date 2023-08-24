package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.awt.Desktop

private val log = KotlinLogging.logger { }

actual class UrlHandlerImpl(
    private val urlHandlerFlow: MutableSharedFlow<Url>
) : UrlHandler, Flow<Url> by urlHandlerFlow {

    actual constructor() : this(
        MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    )

    init {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                val url = Url(event.uri)
                urlHandlerFlow.tryEmit(url)
            }
        } else {
            log.warn("this platform is not supported to set URI handlers")
        }
    }
}