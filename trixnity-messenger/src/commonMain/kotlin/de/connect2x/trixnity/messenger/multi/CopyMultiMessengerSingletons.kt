package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.util.SendLogToDevs
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import de.connect2x.trixnity.messenger.util.UriHandler
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
    val uriHandler = from.getOrNull<UriHandler>()
    if (uriHandler != null) to.single<UriHandler> { uriHandler }
    val sharedDataHandler = from.getOrNull<SharedDataHandler>()
    if (sharedDataHandler != null) to.single<SharedDataHandler> { sharedDataHandler }
}

