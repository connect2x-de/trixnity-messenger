package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.StoragePrefix
import de.connect2x.trixnity.messenger.util.UrlHandler
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMatrixMessengerFactory(): Module = module {
    single<MatrixMessengerFactory> {
        val configuration = get<MatrixMultiMessengerConfiguration>().messenger
        val storagePrefix = get<StoragePrefix>().storagePrefix
        val urlHandler = getOrNull<UrlHandler>()
        MatrixMessengerFactory { profile ->
            MatrixMessengerImpl {
                configuration()
                modules += module {
                    single<StoragePrefix> {
                        StoragePrefix("$storagePrefix$profile/")
                    }
                    if (urlHandler != null) single<UrlHandler> { urlHandler }
                }
            }
        }
    }
}