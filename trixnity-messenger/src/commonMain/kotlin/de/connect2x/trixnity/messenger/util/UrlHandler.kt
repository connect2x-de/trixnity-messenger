package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

interface UrlHandler : Flow<Url>

expect class UrlHandlerImpl() : UrlHandler

fun createFilteringUrlHandler(messengerSettings: MessengerSettings): UrlHandler {
    val urlHandler = UrlHandlerImpl().filter { it.protocol == URLProtocol.createOrDefault(messengerSettings.urlScheme) }
    return object : UrlHandler, Flow<Url> by urlHandler {}
}