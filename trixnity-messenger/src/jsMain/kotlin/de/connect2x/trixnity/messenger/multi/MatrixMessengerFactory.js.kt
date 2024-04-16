package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerImpl
import de.connect2x.trixnity.messenger.util.SendLogToDevs
import de.connect2x.trixnity.messenger.util.StoragePrefix
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMatrixMessengerFactory(): Module = module {
    single<MatrixMessengerFactory> {
        val configuration = get<MatrixMultiMessengerConfiguration>().messengerWithBase
        val copyMultiMessengerSingletons = get<CopyMultiMessengerSingletons>()
        val storagePrefix = get<StoragePrefix>().storagePrefix
        MatrixMessengerFactory { profileId ->
            MatrixMessengerImpl {
                configuration()
                modules += module {
                    single<StoragePrefix> {
                        StoragePrefix("$storagePrefix$profileId/")
                    }
                    copyMultiMessengerSingletons.copy(from = this@single, to = this)
                }
            }
        }
    }
}
