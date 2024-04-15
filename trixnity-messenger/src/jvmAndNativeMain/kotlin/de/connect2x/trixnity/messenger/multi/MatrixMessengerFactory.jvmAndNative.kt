package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMatrixMessengerFactory(): Module = module {
    single<MatrixMessengerFactory> {
        val configuration = get<MatrixMultiMessengerConfiguration>().messengerWithBase
        val copyMultiMessengerSingletons = get<CopyMultiMessengerSingletons>()
        val rootPath = get<RootPath>().path
        MatrixMessengerFactory { profileId ->
            MatrixMessengerImpl {
                configuration()
                modules += module {
                    single<RootPath> {
                        RootPath(rootPath.resolve(profileId))
                    }
                    copyMultiMessengerSingletons.copy(from = this@single, to = this)
                } + module {
                    platformSendLogToDevsModule()
                    copyMultiMessengerSingletons.copy(from = this@single, to = this)
                }
            }
        }
    }
}
