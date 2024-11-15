package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.SendLogToDevs
import de.connect2x.trixnity.messenger.util.SharedFileHandler
import de.connect2x.trixnity.messenger.util.UrlHandler
import org.koin.core.module.Module
import org.koin.core.scope.Scope

/**
 * In case you introduce new settings in the [de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger], you need
 * to make sure that these settings are copied to each of your [de.connect2x.trixnity.messenger.MatrixMessenger]s
 * for the SDK to work properly.
 */
fun interface CopyMultiMessengerSingletons {
    fun copy(from: Scope, to: Module)
}

val DefaultCopyMultiMessengerSingletons = CopyMultiMessengerSingletons { from: Scope, to: Module ->
    to.single<MatrixMultiMessengerConfiguration> { from.get() }
    to.single<MatrixMultiMessengerSettingsHolder> { from.get() }
    to.single<ProfileManager> { from.get() }
    to.single<SendLogToDevs> { from.get() }
    val urlHandler = from.getOrNull<UrlHandler>()
    if (urlHandler != null) to.single<UrlHandler> { urlHandler }
    val sharedFileHandler = from.getOrNull<SharedFileHandler>()
    if (sharedFileHandler != null) to.single<SharedFileHandler> { sharedFileHandler }
}

