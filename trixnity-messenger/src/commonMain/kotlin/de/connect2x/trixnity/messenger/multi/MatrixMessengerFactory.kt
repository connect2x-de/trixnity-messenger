package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.util.UrlHandler
import org.koin.core.module.Module
import org.koin.core.scope.Scope

fun interface MatrixMessengerFactory {
    suspend operator fun invoke(profile: String): MatrixMessenger
}

internal fun Module.copyMultiMessengerSingletons(from: Scope) {
    single<MatrixMultiMessengerConfiguration> { from.get() }
    single<MatrixMultiMessengerSettingsHolder> { from.get() }
    val urlHandler = from.getOrNull<UrlHandler>()
    if (urlHandler != null) single<UrlHandler> { urlHandler }
}

expect fun platformMatrixMessengerFactory(): Module