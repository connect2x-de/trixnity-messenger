package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.UrlHandler
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMatrixMessengerFactory(): Module = module {
    single<MatrixMessengerFactory> {
        val configuration = get<MatrixMultiMessengerConfiguration>().messenger
        val rootPath = get<RootPath>().path
        val urlHandler = getOrNull<UrlHandler>()
        MatrixMessengerFactory { profile ->
            MatrixMessengerImpl {
                configuration()
                modules += module {
                    single<RootPath> {
                        RootPath(rootPath.resolve(profile))
                    }
                    if (urlHandler != null) single<UrlHandler> { urlHandler }
                }
            }
        }
    }
}